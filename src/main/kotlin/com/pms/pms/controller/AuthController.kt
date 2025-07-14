package com.pms.pms.controller

import com.pms.pms.model.*
import com.pms.pms.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: UserInput): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(authService.register(request))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginInput): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(@RequestBody request: OtpVerificationRequest): ResponseEntity<OtpVerificationResponse> {
        return ResponseEntity.ok(authService.verifyOtp(request))
    }

    @PostMapping("/resend-otp")
    fun resendOtp(@RequestBody request: Map<String, String>): ResponseEntity<OtpResponse> {
        val email = request["email"] ?: throw IllegalArgumentException("Email is required")
        return ResponseEntity.ok(authService.resendOtp(email))
    }

    @PostMapping("/refresh-token")
    fun refreshAccessToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.refreshAccessToken(request))
    }
}
