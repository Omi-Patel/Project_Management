package com.pms.pms.service


import com.pms.pms.config.JwtUtil
import com.pms.pms.model.*
import com.pms.pms.repository.AuthRepository
import com.pms.pms.repository.OtpRepository
import com.pms.pms.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Service
class AuthService(
    private val authRepository: AuthRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpRepository: OtpRepository,
    private val emailService: EmailService,
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Value("\${otp.expiration.minutes:10}")
    private var otpExpirationMinutes: Int = 10

    fun register(request: UserInput): UserResponse {

        val existingUser = authRepository.findUserAuthByUsername(request.email)

        if (existingUser != null) {
            throw RuntimeException("User with this email already exists")
        }

        val userId = UUID.randomUUID().toString()

        val createdUser = authRepository.saveUserWithAuth(
            userId = userId,
            name = request.name,
            phoneNumber = request.phoneNumber,
            email = request.email,
            role = "EMPLOYEE",
            hashPassword = passwordEncoder.encode(request.password)
        )

        // Generate and send OTP
        generateAndSendOtp(userId, request.email, request.name)

        return createdUser
    }

    fun login(request: LoginInput): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        // Fetch user details from DB
        val authUserDetails = authRepository.findUserAuthByUsername(request.email)
        val user = authRepository.findUserById(authUserDetails?.userId ?: throw RuntimeException("User not found!"))

        // Check if user is verified
        if (user?.emailVerified != true) {
            throw RuntimeException("Please verify your email before logging in. Check your email for verification code.")
        }

        if (user.status == "ACTIVE") {
            val accessToken = jwtUtil.generateAccessToken(user!!, authRepository.loadUserDetails(request.email))
            val refreshToken = jwtUtil.generateRefreshToken(request.email)

            val refreshTokenId = UUID.randomUUID().toString()
            refreshTokenRepository.saveRefreshToken(
                refreshTokenId,
                user.id,
                refreshToken,
                LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                System.currentTimeMillis()
            )

            return AuthResponse(accessToken, refreshToken)
        }

        throw IllegalAccessException("You are not allowed to enter in this system!!")
    }

    fun refreshAccessToken(request: RefreshTokenRequest): AuthResponse {
        val storedToken = refreshTokenRepository.findRefreshToken(request.refreshToken)
            ?: throw RuntimeException("Invalid refresh token")

        val userId = storedToken.userId
        val username = authRepository.findUserNameByUserId(userId) ?: throw RuntimeException("User Not Found")

        // Fetch user details from DB
        val user = authRepository.findUserById(userId)

        val newAccessToken = jwtUtil.generateAccessToken(user!!, authRepository.loadUserDetailsByUserId(userId))

        val refreshToken = jwtUtil.generateRefreshToken(username)

        refreshTokenRepository.updateRefreshToken(storedToken.id, refreshToken)

        return AuthResponse(newAccessToken, refreshToken)
    }

    fun getUserById(id: String): UserResponse {
        return authRepository.findUserById(id)!!
    }

    fun verifyOtp(request: OtpVerificationRequest): OtpVerificationResponse {
        val otp = otpRepository.findValidOtpByEmail(request.email)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (otp.otpCode != request.otpCode) {
            throw RuntimeException("Invalid OTP code")
        }

        // Mark OTP as used
        otpRepository.markOtpAsUsed(otp.id)

        // Get user details for welcome email
        val user = authRepository.findUserById(otp.userId)
            ?: throw RuntimeException("User not found")

        // Update user verification status
        authRepository.updateUserVerificationStatus(otp.userId, "ACTIVE", true)

        // Send welcome email
        emailService.sendWelcomeEmail(request.email, user.name)

        return OtpVerificationResponse(
            message = "Email verified successfully! Welcome email has been sent. You can now login.",
            email = request.email,
            verified = true
        )
    }

    fun resendOtp(email: String): OtpResponse {
        val user = authRepository.findUserById(
            authRepository.findUserAuthByUsername(email)?.userId
                ?: throw RuntimeException("User not found")
        ) ?: throw RuntimeException("User not found")

        if (user.emailVerified) {
            throw RuntimeException("Email is already verified")
        }

        // Generate and send new OTP
        generateAndSendOtp(user.id, email, user.name)

        return OtpResponse(
            message = "New verification code has been sent to your email",
            email = email
        )
    }

    private fun generateAndSendOtp(userId: String, email: String, userName: String) {
        val otpCode = generateOtpCode()
        val expiresAt = System.currentTimeMillis() + (otpExpirationMinutes * 60 * 1000L)

        val otp = Otp(
            id = UUID.randomUUID().toString(),
            userId = userId,
            email = email,
            otpCode = otpCode,
            expiresAt = expiresAt
        )

        otpRepository.saveOtp(otp)
        emailService.sendOtpEmail(email, otpCode, userName)
    }

    private fun generateOtpCode(): String {
        val random = SecureRandom()
        return String.format("%06d", random.nextInt(1000000))
    }
}
