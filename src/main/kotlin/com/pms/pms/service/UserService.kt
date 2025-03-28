package com.pms.pms.service


import com.pms.pms.model.LoginInput
import com.pms.pms.model.User
import com.pms.pms.model.UserInput
import com.pms.pms.model.UserResponse
import com.pms.pms.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    val id = UUID.randomUUID().toString()
    val createdAt = System.currentTimeMillis()
    val updatedAt = System.currentTimeMillis()


    fun createUser(request: UserInput): UserResponse {
        val user = User(
            id = UUID.randomUUID().toString(),
            name = request.name,
            email = request.email,
            password = request.password,
            phoneNumber = request.phoneNumber,
            status = request.status ?: "ACTIVE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return userRepository.save(user).toResponse()
    }

    fun login(request: LoginInput): String {
        // Find user by email
        val user = userRepository.findByEmail(request.email) ?: throw RuntimeException("User not found")

        // Validate password using BCrypt
        if (user.password != request.password) {
            throw RuntimeException("Invalid credentials")
        }

        // Generate a simple token (In real case, use JWT or other token mechanism)
        return "Login successful for user: ${user.id}"
    }

    fun getAllUsers(): List<UserResponse> =
        userRepository.findAll().map { it.toResponse() }

    fun getUserById(id: String): UserResponse? =
        userRepository.findById(id)?.toResponse()


    fun updateUser(user: User): UserResponse? {
        val existingUser = userRepository.findById(user.id) ?: return null
        val updatedUser = existingUser.copy(
            name = user.name,
            email = user.email,
            password = user.password,
            phoneNumber = user.phoneNumber,
            status = user.status ?: existingUser.status,
            createdAt = existingUser.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return userRepository.update(updatedUser).toResponse()
    }

    fun deleteUser(id: String) = userRepository.deleteById(id)

    private fun User.toResponse() = UserResponse(
        id = id,
        name = name,
        email = email,
        phoneNumber = phoneNumber,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
