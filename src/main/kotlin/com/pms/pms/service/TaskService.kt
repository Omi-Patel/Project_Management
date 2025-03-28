package com.pms.pms.service


import com.pms.pms.model.Task
import com.pms.pms.model.TaskRequest
import com.pms.pms.model.TaskResponse
import com.pms.pms.repository.TaskRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class TaskService(private val taskRepository: TaskRepository) {



    fun createTask(task: TaskRequest): TaskResponse {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()
        // Save task and assign users
        val savedTask = taskRepository.save(task, id, createdAt, updatedAt)
        return savedTask
    }


    // Get all tasks
    fun getAllTasks(): List<TaskResponse> {
        val tasks = taskRepository.findAll()
        return tasks
    }

    // Get task by ID
    fun getTaskById(id: String): TaskResponse? {
        val task = taskRepository.findById(id) ?: return null
        return task
    }

    // Update task
    fun updateTask(taskRequest: TaskResponse): TaskResponse {
        val existingTask = taskRepository.findById(taskRequest.id) ?: throw RuntimeException("Task not found")

        val updatedAt = System.currentTimeMillis()

        // Update task details
        return taskRepository.update(taskRequest.id, taskRequest)
    }

    // Delete task
    fun deleteTask(id: String) {
        taskRepository.delete(id)
    }

    fun getTasksByUserId(userId: String): List<String>? {
        return taskRepository.findTaskIdsByUserId(userId)
    }

//    // Assign users to a task
//    fun assignUsersToTask(taskId: String, userIds: List<String>) {
//        val updatedAt = System.currentTimeMillis()
//        userIds.forEach { userId ->
//            taskRepository.assignUsersToTask(taskId, userId, System.currentTimeMillis(), updatedAt)
//        }
//    }

    // Convert Task to TaskResponse
    private fun Task.toResponse(assigneeIds: List<String>) = TaskResponse(
        id = id,
        projectId = projectId,
        title = title,
        description = description,
        assigneeIds = assigneeIds,
        status = status,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
