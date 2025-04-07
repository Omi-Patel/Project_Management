package com.pms.pms.controller


import com.pms.pms.model.ListProjectTask
import com.pms.pms.model.ListTask
import com.pms.pms.model.TaskRequest
import com.pms.pms.model.TaskResponse
import com.pms.pms.service.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(private val taskService: TaskService) {

    // Create a new task
    @PostMapping("/create")
    fun createTask(@RequestBody taskRequest: TaskRequest): ResponseEntity<TaskResponse> =
        ResponseEntity.ok(taskService.createTask(taskRequest))

    // Get all tasks
    @PostMapping("/list")
    fun getAllTasks(@RequestBody request: ListTask): ResponseEntity<List<TaskResponse>> =
        ResponseEntity.ok(taskService.getAllTasks(request))

    @PostMapping("/project/list")
    fun getAllTasksByProjectId(@RequestBody request: ListProjectTask): ResponseEntity<List<TaskResponse>> =
        ResponseEntity.ok(taskService.getAllTasksByProjectId(request))

    // Get task by ID
    @GetMapping("/get/{id}")
    fun getTaskById(@PathVariable id: String): ResponseEntity<TaskResponse> {
        val task = taskService.getTaskById(id)
        return task?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    // Update an existing task
    @PostMapping("/update")
    fun updateTask(@RequestBody taskRequest: TaskResponse): ResponseEntity<TaskResponse?> =
        ResponseEntity.ok(taskService.updateTask(taskRequest))

    // Update an existing task
    @PostMapping("/update-status/{taskId}/{newStatus}")
    fun updateTaskStatus(@PathVariable taskId: String, @PathVariable newStatus: String): ResponseEntity<String> =
        ResponseEntity.ok(taskService.updateTaskStatus(taskId, newStatus))

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
    @GetMapping("/{userId}")
    fun getTasksByUserId(@PathVariable userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok(taskService.getTasksByUserId(userId))
}
