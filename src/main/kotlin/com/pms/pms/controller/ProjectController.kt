package com.pms.pms.controller


import com.pms.pms.model.Project
import com.pms.pms.model.ProjectRequest
import com.pms.pms.model.ProjectResponse
import com.pms.pms.service.ProjectService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(private val projectService: ProjectService) {

    // Create a new project
    @PostMapping("/create")
    fun createProject(@RequestBody projectRequest: ProjectRequest): ResponseEntity<ProjectResponse> =
        ResponseEntity.ok(projectService.createProject(projectRequest))

    // Get all projects
    @GetMapping("/list")
    fun getAllProjects(): ResponseEntity<List<ProjectResponse>> =
        ResponseEntity.ok(projectService.getAllProjects())

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
