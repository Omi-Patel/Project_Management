package com.pms.pms.service



import com.pms.pms.config.JwtUtil
import com.pms.pms.model.*
import com.pms.pms.repository.AuthRepository
import com.pms.pms.repository.RefreshTokenRepository
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
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun register(request: UserInput): UserResponse {

        val userId = UUID.randomUUID().toString()

        val createdUser = authRepository.saveUserWithAuth(
            userId = userId,
            name = request.name,
            phoneNumber = request.phoneNumber,
            email = request.email,
            role = "EMPLOYEE",
            hashPassword = passwordEncoder.encode(request.password)
        )

        return createdUser
    }

    fun login(request: LoginInput): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        // Fetch user details from DB
        val authUserDetails = authRepository.findUserAuthByUsername(request.email)
        val user = authRepository.findUserById(authUserDetails?.userId ?: throw RuntimeException("User not found!"))

        if (user?.status != "INACTIVE") {
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

}
