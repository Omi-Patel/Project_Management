package com.pms.pms.service


import com.pms.pms.model.*
import com.pms.pms.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    val id = UUID.randomUUID().toString()
    val createdAt = System.currentTimeMillis()
    val updatedAt = System.currentTimeMillis()


    fun getAllUsers(request: UserListInput): List<UserResponse> {
        return userRepository.findAll(request)
    }

    fun getUserById(id: String): UserResponse? =
        userRepository.findById(id)?.toResponse()


    fun updateUser(user: User): UserResponse? {
        val existingUser = userRepository.findById(user.id) ?: return null
        val updatedUser = existingUser.copy(
            name = user.name,
            email = user.email,
            password = user.password,
            phoneNumber = user.phoneNumber,
            role = user.role,
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
        role = role,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
