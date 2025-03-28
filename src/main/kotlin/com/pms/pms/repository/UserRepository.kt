package com.pms.pms.repository


import com.pms.pms.model.User
import com.pms.pms.model.UserInput
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val jdbcTemplate: JdbcTemplate) {

    fun save(user: User): User {
        val sql = """
            INSERT INTO users (id, name, email, password, phone_number, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql, user.id, user.name, user.email, user.password,
            user.phoneNumber, user.status, user.createdAt, user.updatedAt
        )

        return findById(user.id) ?: throw RuntimeException("Failed to retrieve saved user.")
    }

    fun findAll(): List<User> {
        val sql = "SELECT * FROM users"
        return jdbcTemplate.query(sql) { rs, _ ->
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
        }
    }

    fun findById(id: String): User? {
        val sql = "SELECT * FROM users WHERE id = ?"
        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
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


    fun update(user: User): User {
        val sql = """
            UPDATE users
            SET name = ?, email = ?, password = ?, phone_number = ?, status = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, user.name, user.email, user.password, user.phoneNumber,
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
