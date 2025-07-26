package com.pms.pms.service

import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory

@Service
class AITaskGeneratorService(
    @Value("\${gemini.api.key}")
    private val apiKey: String,
    
    @Value("\${gemini.model.name}")
    private val modelName: String,
    
    private val objectMapper: ObjectMapper
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
                trimmedLine.matches(Regex("\\d+\\."))) {
                
                // Save previous task if exists
                currentTask?.let { title ->
                    tasks.add(GeneratedTask(
                        title = title.take(50),
                        description = currentDescription.ifEmpty { "AI generated task" },
                        priority = "MEDIUM",
                        estimatedDays = 3
                    ))
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
            tasks.add(GeneratedTask(
                title = title.take(50),
                description = currentDescription.ifEmpty { "AI generated task" },
                priority = "MEDIUM",
                estimatedDays = 3
            ))
        }
        
        return tasks.ifEmpty { generateFallbackTasks("Unknown Project") }
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