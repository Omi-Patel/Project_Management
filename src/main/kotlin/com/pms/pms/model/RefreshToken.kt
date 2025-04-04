package com.pms.pms.model



import org.springframework.data.annotation.Id
import java.time.Instant

data class RefreshToken(
    @Id
    val id: String,

    val userId: String,
    val token: String,
    val expiresAt: Long = Instant.now().toEpochMilli(),
    val createdAt: Long = Instant.now().toEpochMilli()
)