package com.pms.pms.repository

import com.pms.pms.model.Otp
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class OtpRepository(private val jdbcTemplate: JdbcTemplate) {

    fun saveOtp(otp: Otp) {
        val sql = """
            INSERT INTO otps (id, user_id, email, otp_code, expires_at, is_used, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        
        jdbcTemplate.update(sql, otp.id, otp.userId, otp.email, otp.otpCode, otp.expiresAt, otp.isUsed, otp.createdAt)
    }

    fun findValidOtpByEmail(email: String): Otp? {
        val sql = """
            SELECT id, user_id, email, otp_code, expires_at, is_used, created_at
            FROM otps 
            WHERE email = ? AND is_used = false AND expires_at > ?
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()
        
        return try {
            jdbcTemplate.queryForObject(sql, { rs: ResultSet, _: Int ->
                Otp(
                    id = rs.getString("id"),
                    userId = rs.getString("user_id"),
                    email = rs.getString("email"),
                    otpCode = rs.getString("otp_code"),
                    expiresAt = rs.getLong("expires_at"),
                    isUsed = rs.getBoolean("is_used"),
                    createdAt = rs.getLong("created_at")
                )
            }, email, System.currentTimeMillis())
        } catch (e: Exception) {
            null
        }
    }

    fun markOtpAsUsed(otpId: String) {
        val sql = "UPDATE otps SET is_used = true WHERE id = ?"
        jdbcTemplate.update(sql, otpId)
    }

    fun deleteExpiredOtps() {
        val sql = "DELETE FROM otps WHERE expires_at < ?"
        jdbcTemplate.update(sql, System.currentTimeMillis())
    }

    fun findOtpByUserId(userId: String): Otp? {
        val sql = """
            SELECT id, user_id, email, otp_code, expires_at, is_used, created_at
            FROM otps 
            WHERE user_id = ? AND is_used = false AND expires_at > ?
            ORDER BY created_at DESC 
            LIMIT 1
        """.trimIndent()
        
        return try {
            jdbcTemplate.queryForObject(sql, { rs: ResultSet, _: Int ->
                Otp(
                    id = rs.getString("id"),
                    userId = rs.getString("user_id"),
                    email = rs.getString("email"),
                    otpCode = rs.getString("otp_code"),
                    expiresAt = rs.getLong("expires_at"),
                    isUsed = rs.getBoolean("is_used"),
                    createdAt = rs.getLong("created_at")
                )
            }, userId, System.currentTimeMillis())
        } catch (e: Exception) {
            null
        }
    }
} 