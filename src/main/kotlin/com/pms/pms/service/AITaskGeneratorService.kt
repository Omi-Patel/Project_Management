package com.pms.pms.service

import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import com.pms.pms.model.*
import com.pms.pms.repository.ProjectRepository
import com.pms.pms.repository.TaskRepository

@Service
class AITaskGeneratorService(
    @Value("\${gemini.api.key}")
    private val apiKey: String,

    @Value("\${gemini.model.name}")
    private val modelName: String,

    private val objectMapper: ObjectMapper,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val taskService: TaskService
) {

    private val logger = LoggerFactory.getLogger(AITaskGeneratorService::class.java)

    data class GeneratedTask(
        val title: String,
        val description: String,
        val priority: String = "MEDIUM",
        val estimatedDays: Int? = null
    )

    data class AITaskResponse(
        val tasks: List<GeneratedTask>
    )

    data class TaskTimeline(
        val createdAt: Long,
        val dueDate: Long?
    )

    /**
     * Enhanced AI task generation with preferences
     */
    fun generateIntelligentTasks(request: AITaskGenerationRequest): AITaskGenerationResponse {
        val startTime = System.currentTimeMillis()

        try {
            // Use project context from request or fetch from DB
            val project = request.projectContext?.let {
                createProjectFromContext(it, request.projectId)
            } ?: (projectRepository.findById(request.projectId)
                ?: throw IllegalArgumentException("Project not found"))


            // Generate project analysis
            val analysis = analyzeProject(project as Project, request)

            // Generate tasks based on preferences
            val generatedTasks = generateTasksWithPreferences(project, request)

            // Create actual tasks in the database with proper timeline distribution
            val currentTimestamp = System.currentTimeMillis()
            val taskTimelines = calculateTaskTimelines(project, generatedTasks, request)

            val createdTasks: List<TaskResponse> = generatedTasks.mapIndexed { index, aiTask ->
                val timeline = taskTimelines[index]
                
                // STRICT validation: Due date MUST be within project bounds
                val validDueDate = if (request.includeTimelines && timeline.dueDate != null) {
                    val projectStart = project.startDate ?: currentTimestamp
                    val projectEnd = project.endDate ?: (projectStart + (90 * 24 * 60 * 60 * 1000L))
                    
                    // GUARANTEE: Due date is ALWAYS within project timeline
                    val clampedDueDate = when {
                        timeline.dueDate < projectStart -> {
                            logger.warn("Task due date ${java.time.Instant.ofEpochMilli(timeline.dueDate)} is before project start, clamping to start")
                            projectStart + (24 * 60 * 60 * 1000L) // Start + 1 day
                        }
                        timeline.dueDate > projectEnd -> {
                            logger.warn("Task due date ${java.time.Instant.ofEpochMilli(timeline.dueDate)} is after project end, clamping to end")
                            projectEnd
                        }
                        else -> timeline.dueDate
                    }
                    
                    logger.info("Task due date set to: ${java.time.Instant.ofEpochMilli(clampedDueDate)}")
                    clampedDueDate
                } else null
                
                val taskRequest = TaskRequest(
                    projectId = request.projectId,
                    title = aiTask.title,
                    description = aiTask.description,
                    assigneeIds = if (request.autoAssign) listOf() else listOf(), // TODO: Implement smart assignment
                    status = "TO_DO",
                    priority = aiTask.priority,
                    dueDate = validDueDate
                )
                
                // Task is created NOW (current timestamp), not at timeline.createdAt
                logger.info("Creating task '${aiTask.title}' with creation time: ${java.time.Instant.ofEpochMilli(currentTimestamp + index)} and due date: ${validDueDate?.let { java.time.Instant.ofEpochMilli(it) }}")
                taskService.createTaskWithTimestamp(taskRequest, currentTimestamp + index)
            }

            val generationTime = System.currentTimeMillis() - startTime

            return AITaskGenerationResponse(
                success = true,
                message = "Successfully generated ${createdTasks.size} intelligent tasks based on your preferences",
                generatedTasks = createdTasks,
                analysis = analysis,
                metadata = GenerationMetadata(
                    generationTime = generationTime,
                    aiModel = "Gemini-$modelName",
                    confidenceScore = calculateConfidenceScore(request, analysis),
                    preferencesUsed = PreferencesSnapshot(
                        complexity = request.complexity,
                        focusArea = request.focusArea,
                        taskCount = request.taskCount,
                        creativityLevel = request.creativityLevel,
                        detailLevel = request.detailLevel
                    )
                )
            )

        } catch (e: Exception) {
            logger.error("Error generating intelligent tasks: ${e.message}", e)
            return AITaskGenerationResponse(
                success = false,
                message = "Failed to generate AI tasks: ${e.message}",
                generatedTasks = emptyList()
            )
        }
    }

    fun generateTasksForProject(projectName: String, projectDescription: String?): List<GeneratedTask> {
        return try {
            val client = Client.builder().apiKey(apiKey).build()

            val prompt = buildPrompt(projectName, projectDescription)

            val response = client.models.generateContent(
                modelName,
                prompt,
                null
            )

            val responseText = response.text()
            logger.info("AI Response: $responseText")

            parseTasksFromResponse(responseText)

        } catch (exception: Exception) {
            logger.error("Error generating tasks with AI: ${exception.message}", exception)
            // Return fallback tasks if AI fails
            generateFallbackTasks(projectName)
        }
    }

    private fun buildPrompt(projectName: String, projectDescription: String?): String {
        val description = projectDescription ?: "No description provided"

        return """
        You are a project management assistant. Based on the following project information, generate 5-8 realistic and actionable tasks that would be needed to complete this project.

        Project Name: $projectName
        Project Description: $description

        Please respond with a JSON object in this exact format:
        {
          "tasks": [
            {
              "title": "Task title (keep it concise, max 50 characters)",
              "description": "Detailed description of what needs to be done",
              "priority": "HIGH" | "MEDIUM" | "LOW",
              "estimatedDays": number (1-30 days realistic estimate)
            }
          ]
        }

        Guidelines:
        - Create practical, actionable tasks
        - Include planning, development, testing, and deployment phases
        - Vary priorities based on task importance
        - Keep task titles clear and specific
        - Provide realistic time estimates
        - Ensure tasks are broken down into manageable chunks
        - Focus on tasks that are typical for this type of project

        Return only the JSON object, no additional text or formatting.
        """.trimIndent()
    }

    private fun parseTasksFromResponse(responseText: String): List<GeneratedTask> {
        return try {
            // Clean the response text to extract JSON
            val jsonText = extractJsonFromResponse(responseText)
            val response = objectMapper.readValue<AITaskResponse>(jsonText)
            response.tasks
        } catch (exception: Exception) {
            logger.error("Error parsing AI response: ${exception.message}", exception)
            logger.info("Raw response was: $responseText")

            // Try to parse a simpler format or return fallback
            parseTasksFromPlainText(responseText)
        }
    }

    private fun extractJsonFromResponse(text: String): String {
        // Find JSON object in the response
        val startIndex = text.indexOf("{")
        val endIndex = text.lastIndexOf("}") + 1

        return if (startIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex)
        } else {
            throw IllegalArgumentException("No JSON object found in response")
        }
    }

    private fun parseTasksFromPlainText(responseText: String): List<GeneratedTask> {
        // Fallback: try to extract tasks from plain text if JSON parsing fails
        val tasks = mutableListOf<GeneratedTask>()
        val lines = responseText.lines()

        var currentTask: String? = null
        var currentDescription = ""

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") ||
                trimmedLine.matches(Regex("\\d+\\."))
            ) {

                // Save previous task if exists
                currentTask?.let { title ->
                    tasks.add(
                        GeneratedTask(
                            title = title.take(50),
                            description = currentDescription.ifEmpty { "AI generated task" },
                            priority = "MEDIUM",
                            estimatedDays = 3
                        )
                    )
                }

                // Start new task
                currentTask = trimmedLine.removePrefix("- ").removePrefix("* ")
                    .replaceFirst(Regex("^\\d+\\.\\s*"), "")
                currentDescription = ""
            } else if (trimmedLine.isNotEmpty() && currentTask != null) {
                currentDescription += if (currentDescription.isEmpty()) trimmedLine else " $trimmedLine"
            }
        }

        // Add last task
        currentTask?.let { title ->
            tasks.add(
                GeneratedTask(
                    title = title.take(50),
                    description = currentDescription.ifEmpty { "AI generated task" },
                    priority = "MEDIUM",
                    estimatedDays = 3
                )
            )
        }

        return tasks.ifEmpty { generateFallbackTasks("Unknown Project") }
    }

    /**
     * Create project object from context to avoid database fetch
     */
    private fun createProjectFromContext(context: ProjectContext, projectId: String): Project {
        return Project(
            id = projectId,
            name = context.name,
            description = context.description,
            startDate = context.startDate?.let { parseDate(it) },
            endDate = context.endDate?.let { parseDate(it) },
            // Set other required fields with defaults
            userId = "",
            color = "#3B82F6",
            workspaceId = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Parse date string to timestamp - handles multiple formats
     */
    private fun parseDate(dateString: String): Long {
        return try {
            // Try to parse as timestamp first (if it's already a number)
            dateString.toLongOrNull()?.let { return it }
            
            // Try ISO date format (YYYY-MM-DD)
            if (dateString.contains("-")) {
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    
                    return java.time.LocalDate.of(year, month, day)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }
            }
            
            // Try other common formats
            val instant = java.time.Instant.parse(dateString)
            instant.toEpochMilli()
        } catch (e: Exception) {
            logger.warn("Failed to parse date: $dateString, using current time", e)
            System.currentTimeMillis()
        }
    }

    /**
     * Analyze project and generate insights
     */
    private fun analyzeProject(project: Project, request: AITaskGenerationRequest): ProjectAnalysis {
        val complexity = when (request.complexity) {
            "simple" -> "Low"
            "balanced" -> "Medium"
            "detailed" -> "High"
            else -> "Medium"
        }

        // Calculate duration based on project timeline and task complexity
        val projectDurationMs = if (project.startDate != null && project.endDate != null) {
            project.endDate!! - project.startDate!!
        } else {
            // Estimate based on task count and complexity
            val baseDays = when (request.complexity) {
                "simple" -> request.taskCount * 2
                "balanced" -> request.taskCount * 3
                "detailed" -> request.taskCount * 5
                else -> request.taskCount * 3
            }
            baseDays * 24 * 60 * 60 * 1000L
        }

        val durationDays = projectDurationMs / (24 * 60 * 60 * 1000L)
        val duration = when (durationDays) {
            in 1..14 -> "${durationDays} days"
            in 15..60 -> "${(durationDays / 7)} weeks"
            in 61..365 -> "${(durationDays / 30)} months"
            else -> "${(durationDays / 365)} years"
        }

        val teamSize = request.projectContext?.teamSize ?: when (request.complexity) {
            "simple" -> 2
            "balanced" -> 4
            "detailed" -> 6
            else -> 4
        }

        val riskFactors = mutableListOf<String>()
        if (request.taskCount > 15) riskFactors.add("Large scope may require careful planning")
        if (request.complexity == "detailed") riskFactors.add("High complexity requires experienced team")
        if (request.creativityLevel > 70) riskFactors.add("High creativity may increase uncertainty")

        // Timeline-related risk assessment
        if (project.startDate != null && project.endDate != null) {
            val totalProjectDays = (project.endDate!! - project.startDate!!) / (24 * 60 * 60 * 1000L)
            val estimatedWorkDays = request.taskCount * when (request.complexity) {
                "simple" -> 2
                "balanced" -> 3
                "detailed" -> 5
                else -> 3
            }

            if (estimatedWorkDays > totalProjectDays) {
                riskFactors.add("Timeline may be too tight for the planned scope")
            }

            if (totalProjectDays < 14) {
                riskFactors.add("Short timeline requires intensive coordination")
            }
        }

        if (request.templateStyle == "waterfall" && request.taskCount > 10) {
            riskFactors.add("Waterfall approach with many tasks increases delivery risk")
        }

        val milestones = when (request.templateStyle) {
            "agile" -> listOf("Sprint Planning", "MVP Development", "Sprint Review", "Release")
            "waterfall" -> listOf("Requirements", "Design", "Development", "Testing", "Deployment")
            "kanban" -> listOf("Backlog Ready", "Development", "Testing", "Done")
            else -> listOf("Planning", "Development", "Testing", "Release")
        }

        val suggestions = mutableListOf<String>()
        if (request.focusArea == "all") suggestions.add("Consider focusing on specific areas for better results")
        if (!request.includeTimelines) suggestions.add("Adding timelines would improve project planning")
        if (request.detailLevel < 50) suggestions.add("Higher detail level recommended for complex projects")

        // Timeline-based optimization suggestions
        if (project.startDate != null && project.endDate != null) {
            val totalProjectDays = (project.endDate!! - project.startDate!!) / (24 * 60 * 60 * 1000L)

            if (totalProjectDays > 90 && request.templateStyle != "agile") {
                suggestions.add("Consider Agile methodology for long-term projects")
            }

            if (totalProjectDays < 30 && request.taskCount > 8) {
                suggestions.add("Reduce task count or extend timeline for better execution")
            }
        }

        if (request.templateStyle == "kanban" && !request.includeDependencies) {
            suggestions.add("Enable dependencies for better Kanban flow management")
        }

        if (request.autoAssign && teamSize > 1) {
            suggestions.add("Auto-assignment enabled - ensure team skills are properly defined")
        }

        return ProjectAnalysis(
            projectComplexity = complexity,
            estimatedDuration = duration,
            recommendedTeamSize = teamSize,
            riskFactors = riskFactors,
            keyMilestones = milestones,
            optimizationSuggestions = suggestions
        )
    }

    /**
     * Generate tasks based on user preferences
     */
    private fun generateTasksWithPreferences(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        return when (request.templateStyle) {
            "agile" -> generateAgileBasedTasks(project, request)
            "waterfall" -> generateWaterfallTasks(project, request)
            "kanban" -> generateKanbanTasks(project, request)
            "custom" -> generateCustomTasks(project, request)
            else -> generateBalancedTasks(project, request)
        }
    }

    /**
     * Generate Agile-based tasks
     */
    private fun generateAgileBasedTasks(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        val prompt = buildEnhancedPrompt(project, request, "agile")
        val aiTasks = getTasksFromAI(prompt)
        return enhanceTasksWithPreferences(aiTasks, request).take(request.taskCount)
    }

    /**
     * Generate Waterfall-based tasks
     */
    private fun generateWaterfallTasks(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        val prompt = buildEnhancedPrompt(project, request, "waterfall")
        val aiTasks = getTasksFromAI(prompt)
        return enhanceTasksWithPreferences(aiTasks, request).take(request.taskCount)
    }

    /**
     * Generate Kanban-based tasks
     */
    private fun generateKanbanTasks(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        val prompt = buildEnhancedPrompt(project, request, "kanban")
        val aiTasks = getTasksFromAI(prompt)
        return enhanceTasksWithPreferences(aiTasks, request).take(request.taskCount)
    }

    /**
     * Generate Custom-based tasks
     */
    private fun generateCustomTasks(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        val customInstructions = request.customInstructions ?: "Generate tasks based on project requirements"
        val prompt = buildEnhancedPrompt(project, request, "custom", customInstructions)
        val aiTasks = getTasksFromAI(prompt)
        return enhanceTasksWithPreferences(aiTasks, request).take(request.taskCount)
    }

    /**
     * Generate balanced tasks (fallback)
     */
    private fun generateBalancedTasks(project: Project, request: AITaskGenerationRequest): List<GeneratedTask> {
        return generateTasksForProject(project.name, project.description).take(request.taskCount)
    }

    /**
     * Build enhanced prompt based on preferences
     */
    private fun buildEnhancedPrompt(
        project: Project,
        request: AITaskGenerationRequest,
        style: String,
        customInstructions: String? = null
    ): String {
        val complexityInstruction = when (request.complexity) {
            "simple" -> "Keep tasks simple and straightforward with clear objectives"
            "balanced" -> "Create a mix of simple and complex tasks with moderate detail"
            "detailed" -> "Generate comprehensive tasks with thorough descriptions and acceptance criteria"
            else -> "Create well-structured tasks"
        }

        val focusInstruction = when (request.focusArea) {
            "development" -> "Focus primarily on coding, implementation, and technical tasks"
            "design" -> "Emphasize UI/UX, visual design, and user experience tasks"
            "testing" -> "Concentrate on quality assurance, testing strategies, and validation"
            "planning" -> "Focus on project planning, documentation, and strategy tasks"
            else -> "Include a balanced mix of all project areas"
        }

        val styleInstruction = when (style) {
            "agile" -> "Follow Agile methodology with user stories, sprints, and iterative development"
            "waterfall" -> "Use Waterfall approach with sequential phases and detailed documentation"
            "kanban" -> "Apply Kanban principles with continuous flow and flexible prioritization"
            "custom" -> customInstructions ?: "Use custom approach based on project requirements"
            else -> "Use standard project management practices"
        }

        val creativityInstruction = when {
            request.creativityLevel >= 70 -> "Be highly creative and innovative with task suggestions"
            request.creativityLevel >= 40 -> "Balance creativity with practical considerations"
            else -> "Focus on proven, conservative approaches"
        }

        val detailInstruction = when {
            request.detailLevel >= 80 -> "Provide very detailed descriptions with specific acceptance criteria"
            request.detailLevel >= 60 -> "Include good detail with clear requirements"
            else -> "Keep descriptions concise but informative"
        }

        val projectTimelineInfo = if (project.startDate != null && project.endDate != null) {
            val totalDays = (project.endDate!! - project.startDate!!) / (24 * 60 * 60 * 1000L)
            "- Project Timeline: ${totalDays} days total duration"
        } else ""

        return """
        You are an expert project management assistant. Generate exactly ${request.taskCount} tasks for this project.

        Project Information:
        - Name: ${project.name}
        - Description: ${project.description ?: "No description provided"}
        ${request.projectContext?.let { "- Team Size: ${it.teamSize}" } ?: ""}
        ${request.projectContext?.technologies?.let { "- Technologies: ${it.joinToString(", ")}" } ?: ""}
        $projectTimelineInfo

        Requirements:
        - $complexityInstruction
        - $focusInstruction
        - $styleInstruction
        - $creativityInstruction
        - $detailInstruction
        ${if (request.includeTimelines) "- Include realistic time estimates that fit within the project timeline" else ""}
        ${if (request.riskAssessment) "- Consider potential risks and challenges" else ""}
        ${if (request.includeDependencies) "- Consider logical task dependencies and sequencing" else ""}

        Response Format (JSON only):
        {
          "tasks": [
            {
              "title": "Task title (max 60 characters)",
              "description": "Detailed description based on requirements",
              "priority": "HIGH" | "MEDIUM" | "LOW",
              "estimatedDays": number (1-21 days, consider total project timeline)
            }
          ]
        }

        Generate exactly ${request.taskCount} tasks that work well with the ${request.templateStyle} methodology. Return only the JSON object.
        """.trimIndent()
    }

    /**
     * Get tasks from AI with error handling
     */
    private fun getTasksFromAI(prompt: String): List<GeneratedTask> {
        return try {
            val client = Client.builder().apiKey(apiKey).build()
            val response = client.models.generateContent(modelName, prompt, null)
            val responseText = response.text()
            parseTasksFromResponse(responseText)
        } catch (e: Exception) {
            logger.error("AI generation failed, using fallback: ${e.message}")
            generateFallbackTasks("Project")
        }
    }

    /**
     * Enhance tasks based on preferences
     */
    private fun enhanceTasksWithPreferences(
        tasks: List<GeneratedTask>,
        request: AITaskGenerationRequest
    ): List<GeneratedTask> {
        return tasks.map { task ->
            task.copy(
                description = if (request.detailLevel >= 70) {
                    "${task.description}\n\nAcceptance Criteria:\n- Task completion verified\n- Quality standards met"
                } else task.description,
                priority = adjustPriorityWithCreativity(task.priority, request.creativityLevel)
            )
        }
    }

    /**
     * Adjust priority based on creativity level
     */
    private fun adjustPriorityWithCreativity(priority: String, creativityLevel: Int): String {
        return if (creativityLevel > 70) {
            // Higher creativity might suggest different priorities
            when (priority) {
                "LOW" -> if (creativityLevel > 80) "MEDIUM" else "LOW"
                "MEDIUM" -> priority
                "HIGH" -> priority
                else -> priority
            }
        } else priority
    }

    /**
     * Calculate confidence score based on request and analysis
     */
    private fun calculateConfidenceScore(request: AITaskGenerationRequest, analysis: ProjectAnalysis): Double {
        var confidence = 0.8 // Base confidence

        // Adjust based on complexity
        when (request.complexity) {
            "simple" -> confidence += 0.1
            "detailed" -> confidence -= 0.1
        }

        // Adjust based on focus area
        if (request.focusArea != "all") confidence += 0.05

        // Adjust based on task count
        if (request.taskCount in 5..12) confidence += 0.05

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate task timelines within STRICT project bounds
     */
    private fun calculateTaskTimelines(
        project: Project,
        tasks: List<GeneratedTask>,
        request: AITaskGenerationRequest
    ): List<TaskTimeline> {
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Get EXACT project timeline bounds - NO MODIFICATIONS
        val projectStartTime = project.startDate ?: currentTime
        val projectEndTime = project.endDate ?: (projectStartTime + (90 * oneDayMs))
        
        logger.info("Project timeline: Start=${java.time.Instant.ofEpochMilli(projectStartTime)}, End=${java.time.Instant.ofEpochMilli(projectEndTime)}")
        
        // ALWAYS use strict bounds - never extend or modify project timeline
        return distributeTasksWithinStrictBounds(projectStartTime, projectEndTime, tasks)
    }
    
    /**
     * Distribute tasks within STRICT project bounds - GUARANTEED to stay within timeline
     */
    private fun distributeTasksWithinStrictBounds(
        startTime: Long,
        endTime: Long,
        tasks: List<GeneratedTask>
    ): List<TaskTimeline> {
        val oneDayMs = 24 * 60 * 60 * 1000L
        val totalDuration = endTime - startTime
        
        // Ensure minimum project duration of 1 day
        val safeDuration = maxOf(totalDuration, oneDayMs)
        val safeEndTime = if (totalDuration < oneDayMs) startTime + oneDayMs else endTime
        
        val timelines = mutableListOf<TaskTimeline>()
        
        for (index in tasks.indices) {
            // Distribute tasks evenly across the EXACT project timeline
            val progressRatio = if (tasks.size == 1) {
                0.5 // Single task goes in middle
            } else {
                index.toDouble() / (tasks.size - 1).toDouble()
            }
            
            // Calculate due date within project bounds
            val taskDueDate = startTime + (progressRatio * safeDuration).toLong()
            
            // GUARANTEE: Due date is NEVER outside project timeline
            val guaranteedDueDate = when {
                taskDueDate < startTime -> startTime + (oneDayMs / 2) // Start + 12 hours
                taskDueDate > safeEndTime -> safeEndTime
                else -> taskDueDate
            }
            
            timelines.add(
                TaskTimeline(
                    createdAt = startTime, // Not used for actual creation timestamp
                    dueDate = guaranteedDueDate
                )
            )
            
            logger.info("Task ${index + 1}: Due=${java.time.Instant.ofEpochMilli(guaranteedDueDate)}")
        }
        
        return timelines
    }
    
    private fun generateFallbackTasks(projectName: String): List<GeneratedTask> {
        return listOf(
            GeneratedTask(
                title = "Project Planning & Requirements",
                description = "Define project scope, requirements, and create initial project plan for $projectName",
                priority = "HIGH",
                estimatedDays = 2
            ),
            GeneratedTask(
                title = "System Design & Architecture",
                description = "Design system architecture and technical specifications",
                priority = "HIGH",
                estimatedDays = 3
            ),
            GeneratedTask(
                title = "Core Development Setup",
                description = "Set up development environment and implement core functionality",
                priority = "MEDIUM",
                estimatedDays = 5
            ),
            GeneratedTask(
                title = "Feature Implementation",
                description = "Implement main features and functionality",
                priority = "MEDIUM",
                estimatedDays = 7
            ),
            GeneratedTask(
                title = "Testing & Quality Assurance",
                description = "Perform comprehensive testing and quality checks",
                priority = "MEDIUM",
                estimatedDays = 3
            ),
            GeneratedTask(
                title = "Documentation & Deployment",
                description = "Create documentation and deploy the project",
                priority = "LOW",
                estimatedDays = 2
            )
        )
    }
} 