package com.pms.pms.service


import com.pms.pms.model.Project
import com.pms.pms.model.ProjectRequest
import com.pms.pms.model.ProjectResponse
import com.pms.pms.repository.ProjectRepository
import com.pms.pms.repository.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectService(private val projectRepository: ProjectRepository, private val taskRepository: TaskRepository) {



    fun createProject(project: ProjectRequest): ProjectResponse {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        return projectRepository.save(project, id, createdAt, updatedAt).toResponse()
    }

    // Get all projects
    fun getAllProjects(userId: String): List<ProjectResponse> {
        return projectRepository.findAll(userId).map { it.toResponse() }
    }

    // Get project by ID
    fun getProjectById(id: String): ProjectResponse? {
        val project = projectRepository.findById(id) ?: return null
        return project
    }

    // Update existing project
    fun updateProject(projectRequest: Project): ProjectResponse {
        val existingProject = projectRepository.findById(projectRequest.id) ?: throw RuntimeException("Project not found")
        val updatedAt = System.currentTimeMillis()

        val updatedProject = existingProject.copy(
            name = projectRequest.name,
            description = projectRequest.description,
            color = projectRequest.color,
            startDate = projectRequest.startDate,
            endDate = projectRequest.endDate,
            updatedAt = updatedAt
        )
        projectRepository.update(projectRequest.id, updatedProject)
        return updatedProject
    }

    // Delete a project by ID
    fun deleteProject(id: String) {
        // Check if the project exists
        val project = projectRepository.findById(id) ?: throw RuntimeException("Project not found")

        // Get task IDs associated with the project
        val taskIds = taskRepository.findTaskIdsByProjectId(id)

        if (taskIds.isNotEmpty()) {
            // Delete from user_tasks using task IDs
            taskIds.forEach { taskId ->
                // Delete from user_tasks using task ID
                taskRepository.delete(taskId)
            }
        }

        // Finally, delete the project
        projectRepository.delete(id)
    }

    // Convert Project to ProjectResponse
    private fun Project.toResponse() = ProjectResponse(
        id = id,
        name = name,
        taskIds = listOf(),
        description = description,
        userId = userId,
        color = color,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
