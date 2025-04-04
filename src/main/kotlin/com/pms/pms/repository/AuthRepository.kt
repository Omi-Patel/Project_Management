package com.pms.pms.repository


import com.pms.pms.model.User
import com.pms.pms.model.UserAuth
import com.pms.pms.model.UserResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class AuthRepository(private val jdbcTemplate: JdbcTemplate) {

    @Transactional
    fun saveUserWithAuth(
        userId: String, name: String, phoneNumber: String?, email: String?,
        role: String, hashPassword: String
    ): UserResponse {

        val currentTime = System.currentTimeMillis()

        // Save user data
        val userSql = """
        INSERT INTO users (id, name, phone_number, email, role, status, created_at, updated_at) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent()
        jdbcTemplate.update(
            userSql,
            userId,
            name,
            phoneNumber,
            email,
            role,
            "ACTIVE",
            currentTime,
            currentTime
        )

        // Save authentication data
        val authId = userId
        val authSql = """
        INSERT INTO user_auth (id, user_id, email, hash_password, created_at) 
        VALUES (?, ?, ?, ?, ?)
    """.trimIndent()
        jdbcTemplate.update(authSql, authId, userId, email, hashPassword, currentTime)


        // Return the newly created user
        return findUserById(userId) ?: throw RuntimeException("Failed to retrieve saved user")
    }


    fun findUserAuthByUsername(email: String): UserAuth? {
        val sql = "SELECT * FROM user_auth WHERE email = ?"
        return jdbcTemplate.query(sql, arrayOf(email)) { rs, _ ->
            UserAuth(
                id = rs.getString("id"),
                userId = rs.getString("user_id"),
                email = rs.getString("email"),
                hashPassword = rs.getString("hash_password")
            )
        }.firstOrNull()
    }

    fun findUserAuthByUserId(username: String): UserAuth? {
        val sql = "SELECT * FROM user_auth WHERE user_id = ?"
        return jdbcTemplate.query(sql, arrayOf(username)) { rs, _ ->
            UserAuth(
                id = rs.getString("id"),
                userId = rs.getString("user_id"),
                email = rs.getString("email"),
                hashPassword = rs.getString("hash_password")
            )
        }.firstOrNull()
    }

    fun findUserNameByUserId(userId: String): String? {
        val sql = "SELECT email FROM user_auth WHERE user_id = ?"
        return jdbcTemplate.queryForObject(sql, arrayOf(userId), String::class.java)
    }

    fun loadUserDetails(username: String): org.springframework.security.core.userdetails.User {
        val userAuth = findUserAuthByUsername(username) ?: throw RuntimeException("User not found")
        return org.springframework.security.core.userdetails.User(
            userAuth.email, userAuth.hashPassword, listOf()
        )
    }

    fun loadUserDetailsByUserId(userId: String): org.springframework.security.core.userdetails.User {
        val userAuth = findUserAuthByUserId(userId) ?: throw RuntimeException("User not found")
        return org.springframework.security.core.userdetails.User(
            userAuth.email, userAuth.hashPassword, listOf()
        )
    }

    fun findUserById(userId: String): UserResponse? {
        val sql = "SELECT * FROM users WHERE id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, arrayOf(userId)) { rs, _ ->
                val user = UserResponse(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    phoneNumber = rs.getString("phone_number"),
                    email = rs.getString("email"),
                    role = rs.getString("role"), // Fetch roles separately
                    status = rs.getString("status"),
                    createdAt = rs.getLong("created_at"),
                    updatedAt = rs.getLong("updated_at")
                )
                user
            }
        } catch (e: Exception) {
            null // Return null if user is not found
        }
    }


    // Update user status to 'Authorized' from 'Pending' once user logs in for the first time
    fun updateUserStatus(id: String, status: String): Int {
        val sql = "UPDATE users SET status=? WHERE id=?"
        return jdbcTemplate.update(sql, status, id)
    }

}