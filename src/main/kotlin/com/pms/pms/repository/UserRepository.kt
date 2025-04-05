package com.pms.pms.repository


import com.pms.pms.model.User
import com.pms.pms.model.UserListInput
import com.pms.pms.model.UserResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    fun findAll(request: UserListInput): List<UserResponse> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        val baseSql = """
        SELECT id, name, email, phone_number, role, status, created_at, updated_at
        FROM users
    """.trimIndent()

        // Search filter
        if (!request.search.isNullOrBlank()) {
            conditions.add("(LOWER(name) LIKE ? OR LOWER(email) LIKE ?)")
            val searchParam = "%${request.search.trim().lowercase()}%"
            params.add(searchParam)
            params.add(searchParam)
        }

        // Pagination defaults
        val page = (request.page ?: 1).coerceAtLeast(1)
        val size = (request.size ?: 10).coerceAtLeast(1)
        val offset = (page - 1) * size

        // Final SQL build
        val sql = buildString {
            append(baseSql)
            if (conditions.isNotEmpty()) {
                append(" WHERE ").append(conditions.joinToString(" AND "))
            }
            append(" ORDER BY created_at DESC")
            append(" LIMIT ? OFFSET ?")
        }

        // Add pagination
        params.add(size)
        params.add(offset)

        // Execute and map result
        return jdbcTemplate.query(sql, params.toTypedArray()) { rs, _ ->
            UserResponse(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                phoneNumber = rs.getString("phone_number"),
                role = rs.getString("role"),
                status = rs.getString("status"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }


    fun findById(id: String): User? {
        val sql = "SELECT * FROM users WHERE id = ?"
        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            User(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                phoneNumber = rs.getString("phone_number"),
                role = rs.getString("role"),
                status = rs.getString("status"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }


    fun update(user: User): User {
        val sql = """
            UPDATE users
            SET name = ?, email = ?, phone_number = ?, role = ?, status = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, user.name, user.email, user.phoneNumber, user.role,
            user.status, user.updatedAt, user.id
        )

        return findById(user.id) ?: throw RuntimeException("Failed to retrieve updated user.")
    }

    fun deleteById(id: String): Int {
        val sql = "DELETE FROM users WHERE id = ?"
        return jdbcTemplate.update(sql, id)
    }

    fun findByEmail(email: String): User? {
        val sql = "SELECT * FROM users WHERE email = ?"
        return jdbcTemplate.query(sql, arrayOf(email)) { rs, _ ->
            User(
                id = rs.getString("id"),
                name = rs.getString("name"),
                email = rs.getString("email"),
                password = rs.getString("password"),
                phoneNumber = rs.getString("phone_number"),
                status = rs.getString("status"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }
}
