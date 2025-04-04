package com.pms.pms.model


import org.springframework.data.annotation.Id
import java.time.Instant


data class UserAuth(
    @Id
    val id: String,

    val userId: String,
    val email: String,
    val hashPassword: String,
    val createdAt: Long = Instant.now().toEpochMilli()
)

data class RefreshTokenRequest(
    val refreshToken: String
)