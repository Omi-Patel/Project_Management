package com.pms.pms.controller


import com.pms.pms.model.*
import com.pms.pms.service.ProjectService
import com.pms.pms.service.TaskService
import com.pms.pms.service.AITaskGeneratorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val taskService: TaskService,
    private val aiTaskGeneratorService: AITaskGeneratorService
) {

    // Create a new project
    @PostMapping("/create")
    fun createProject(@RequestBody projectRequest: ProjectRequest): ResponseEntity<ProjectResponse> =
        ResponseEntity.ok(projectService.createProject(projectRequest))

    // Enhanced AI task generation with preferences
    @PostMapping("/generate-ai-tasks")
    fun generateAITasks(@RequestBody request: AITaskGenerationRequest): ResponseEntity<AITaskGenerationResponse> {
        return try {
            val response = aiTaskGeneratorService.generateIntelligentTasks(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                AITaskGenerationResponse(
                    success = false,
                    message = "Failed to generate AI tasks: ${e.message}",
                    generatedTasks = emptyList()
                )
            )
        }
    }

    // Legacy endpoint for backward compatibility
    @PostMapping("/generate-ai-tasks/{projectId}")
    fun generateAITasksForExistingProject(@PathVariable projectId: String): ResponseEntity<Map<String, Any>> {
        val project = projectService.getProjectById(projectId)
            ?: return ResponseEntity.notFound().build()
        
        // Generate AI tasks for the project
        val generatedTasks = aiTaskGeneratorService.generateTasksForProject(
            project.name,
            project.description
        )
        
        // Create actual tasks in the database with proper ordering
        val baseTimestamp = System.currentTimeMillis()
        val createdTasks = generatedTasks.mapIndexed { index, aiTask ->
            val taskRequest = TaskRequest(
                projectId = projectId,
                title = aiTask.title,
                description = aiTask.description,
                assigneeIds = listOf(), // No assignees initially
                status = "TO_DO",
                priority = aiTask.priority,
                dueDate = aiTask.estimatedDays?.let { days ->
                    baseTimestamp + (days * 24 * 60 * 60 * 1000L)
                }
            )
            // Create task with incremented timestamp to ensure proper ordering
            taskService.createTaskWithTimestamp(taskRequest, baseTimestamp + index)
        }
        
        return ResponseEntity.ok(mapOf(
            "project" to project,
            "generatedTasks" to createdTasks,
            "message" to "Generated ${createdTasks.size} AI tasks for project: ${project.name}"
        ))
    }

    // Get all projects of user
    @GetMapping("/list/{userId}")
    fun getAllProjectsByUserId(@PathVariable userId: String): ResponseEntity<List<ProjectResponse>> =
        ResponseEntity.ok(projectService.getAllProjects(userId))

    // Get all projects
    @GetMapping("/list")
    fun getAllProjects(): ResponseEntity<List<ProjectResponse>> =
        ResponseEntity.ok(projectService.getAllProjects(null))

    // Get project by ID
    @GetMapping("/get/{id}")
    fun getProjectById(@PathVariable id: String): ResponseEntity<ProjectResponse> {
        val project = projectService.getProjectById(id)
        return project?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    // Update project
    @PostMapping("/update")
    fun updateProject(@RequestBody projectRequest: Project): ResponseEntity<ProjectResponse> =
        ResponseEntity.ok(projectService.updateProject(projectRequest))

    // Delete project
    @DeleteMapping("/delete/{id}")
    fun deleteProject(@PathVariable id: String): ResponseEntity<Void> {
        projectService.deleteProject(id)
        return ResponseEntity.noContent().build()
    }
}
