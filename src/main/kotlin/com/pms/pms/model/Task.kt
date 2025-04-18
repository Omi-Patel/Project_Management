package com.pms.pms.model


import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.UUID

data class Task(
    @Id
    val id: String,
    val projectId: String,
    val title: String,
    val description: String?,
    val status: String = "TO_DO",
    val priority: String = "MEDIUM",
    val dueDate: Long?,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)


data class TaskRequest(
    val projectId: String,
    val title: String,
    val description: String?,
    val assigneeIds: List<String>,
    val status: String = "TO_DO",
    val priority: String = "MEDIUM",
    val dueDate: Long?
)

data class TaskResponse(
    val id: String,
    val project: Project?,
    val title: String,
    val description: String?,
    val assigneeIds: List<String>,
    val status: String,
    val priority: String,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class UserTaskRequest(
    val userId: String,
    val taskId: String
)

data class ListTask(
    val userId: String?,
    val search: String?,
    val page: Int? = 1,
    val size: Int? = 10,
    val statuses: List<String>?,
    val priorities: List<String>?,
    val assigneeIds: List<String>?
)

data class ListProjectTask(
    val projectId: String,
    val search: String?,
    val page: Int? = 1,
    val size: Int? = 10
)
