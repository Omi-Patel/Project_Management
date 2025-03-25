package com.pms.pms.controller


import com.pms.pms.model.Task
import com.pms.pms.model.TaskRequest
import com.pms.pms.model.TaskResponse
import com.pms.pms.service.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(private val taskService: TaskService) {

    // Create a new task
    @PostMapping("/create")
    fun createTask(@RequestBody taskRequest: TaskRequest): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.createTask(taskRequest))

    // Get all tasks
    @GetMapping("/list")
    fun getAllTasks(): ResponseEntity<List<TaskResponse>> =
        ResponseEntity.ok(taskService.getAllTasks())

    // Get task by ID
    @GetMapping("/get/{id}")
    fun getTaskById(@PathVariable id: String): ResponseEntity<TaskResponse> {
        val task = taskService.getTaskById(id)
        return task?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    // Update an existing task
    @PostMapping("/update")
    fun updateTask(@RequestBody taskRequest: TaskResponse): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.updateTask(taskRequest))

    // Delete task by ID
    @DeleteMapping("/delete/{id}")
    fun deleteTask(@PathVariable id: String): ResponseEntity<Void> {
        taskService.deleteTask(id)
        return ResponseEntity.noContent().build()
    }

//    // Assign users to task
//    @PostMapping("/assign")
//    fun assignUsersToTask(@RequestParam taskId: String, @RequestParam userIds: List<String>): ResponseEntity<Void> {
//        taskService.assignUsersToTask(taskId, userIds)
//        return ResponseEntity.ok().build()
//    }

    // Get tasks assigned to a specific user
//    @GetMapping("/user/{userId}")
//    fun getTasksByUserId(@PathVariable userId: String): ResponseEntity<List<TaskResponse>> =
//        ResponseEntity.ok(taskService.getTasksByUserId(userId))
}
