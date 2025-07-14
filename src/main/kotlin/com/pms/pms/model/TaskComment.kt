package com.pms.pms.model

import java.time.Instant
import java.util.UUID

data class TaskComment(
    val id: String,
    val taskId: String,
    val userId: String,
    val content: String,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

data class TaskCommentRequest(
    val taskId: String,
    val content: String
)

data class TaskCommentResponse(
    val id: String,
    val taskId: String,
    val userId: String,
    val userName: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ListTaskComments(
    val taskId: String,
    val page: Int? = 1,
    val size: Int? = 20
) 