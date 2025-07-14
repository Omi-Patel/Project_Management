package com.pms.pms.model

import org.springframework.data.annotation.Id
import java.time.Instant

data class Otp(
    @Id
    val id: String,
    val userId: String,
    val email: String,
    val otpCode: String,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val createdAt: Long = Instant.now().toEpochMilli()
)

data class OtpVerificationRequest(
    val email: String,
    val otpCode: String
)

data class OtpResponse(
    val message: String,
    val email: String
)

data class OtpVerificationResponse(
    val message: String,
    val email: String,
    val verified: Boolean
) 