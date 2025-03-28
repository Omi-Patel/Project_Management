package com.pms.pms.model


import org.springframework.data.annotation.Id
import java.time.Instant
import java.util.UUID

data class Project(
    @Id
    val id: String,
    val name: String,
    val description: String?,
    val startDate: Long?,
    val endDate: Long?,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)


data class ProjectRequest(
    val name: String,
    val description: String?,
    val startDate: Long?,
    val endDate: Long?
)

data class ProjectResponse(
    val id: String,
    val name: String,
    val taskIds: List<String>?,
    val description: String?,
    val startDate: Long?,
    val endDate: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
