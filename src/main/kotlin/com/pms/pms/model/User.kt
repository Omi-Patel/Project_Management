package com.pms.pms.model


import org.springframework.data.annotation.Id
import java.time.Instant
import java.time.LocalDateTime

data class User(
    @Id
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null,
    val status: String? = null, // Example: ACTIVE, INACTIVE, PENDING
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

data class UserInput(
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null,
    val status: String? = "ACTIVE" // Default status set to ACTIVE
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val status: String? = null, // Example: ACTIVE, INACTIVE, PENDING
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)