package com.pms.pms.repository

import com.pms.pms.model.RefreshToken

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepository(private val jdbcTemplate: JdbcTemplate) {

    fun saveRefreshToken(id: String, userId: String, token: String, expiresAt: Long, updatedAt: Long) {
        val sql = "INSERT INTO refresh_tokens (id, user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?, ?)"
        jdbcTemplate.update(sql, id, userId, token, expiresAt, updatedAt)
    }

    fun findRefreshToken(token: String): RefreshToken? {
        val sql = "SELECT * FROM refresh_tokens WHERE token = ?"
        return jdbcTemplate.query(sql, arrayOf(token)) { rs, _ ->
            RefreshToken(
                id = rs.getString("id"),
                userId = rs.getString("user_id"),
                token = rs.getString("token"),
                expiresAt = rs.getLong("expires_at"),
                createdAt = rs.getLong("created_at")
            )
        }.firstOrNull()
    }

    fun deleteByUserAuth(userId: String) {
        val sql = "DELETE FROM refresh_tokens WHERE user_id = ?"
        jdbcTemplate.update(sql, userId)
    }

    fun updateRefreshToken(id: String, newToken: String) {
        val sql = """
        UPDATE refresh_tokens 
        SET token = ?, expires_at = ? 
        WHERE id = ?
    """

        val expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days in milliseconds

        jdbcTemplate.update(sql, newToken, expiresAt, id)
    }

}