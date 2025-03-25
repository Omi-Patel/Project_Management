package com.pms.pms.service


import com.pms.pms.model.Project
import com.pms.pms.model.ProjectRequest
import com.pms.pms.model.ProjectResponse
import com.pms.pms.repository.ProjectRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectService(private val projectRepository: ProjectRepository) {



    fun createProject(project: ProjectRequest): ProjectResponse {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        return projectRepository.save(project, id, createdAt, updatedAt).toResponse()
    }

    // Get all projects
    fun getAllProjects(): List<ProjectResponse> {
        return projectRepository.findAll().map { it.toResponse() }
    }

    // Get project by ID
    fun getProjectById(id: String): ProjectResponse? {
        val project = projectRepository.findById(id) ?: return null
        return project.toResponse()
    }

    // Update existing project
    fun updateProject(projectRequest: Project): ProjectResponse {
        val existingProject = projectRepository.findById(projectRequest.id) ?: throw RuntimeException("Project not found")
        val updatedAt = System.currentTimeMillis()

        val updatedProject = existingProject.copy(
            name = projectRequest.name,
            description = projectRequest.description,
            startDate = projectRequest.startDate,
            endDate = projectRequest.endDate,
            updatedAt = updatedAt
        )
        projectRepository.update(projectRequest.id, updatedProject)
        return updatedProject.toResponse()
    }

    // Delete a project by ID
    fun deleteProject(id: String) {
        val project = projectRepository.findById(id) ?: throw RuntimeException("Project not found")
        projectRepository.delete(id)
    }

    // Convert Project to ProjectResponse
    private fun Project.toResponse() = ProjectResponse(
        id = id,
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
