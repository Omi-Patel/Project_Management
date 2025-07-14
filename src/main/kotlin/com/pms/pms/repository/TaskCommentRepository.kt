package com.pms.pms.repository

import com.pms.pms.model.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TaskCommentRepository(private val jdbcTemplate: JdbcTemplate) {

    // Create a new comment
    fun createComment(comment: TaskCommentRequest, userId: String): TaskCommentResponse {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val updatedAt = System.currentTimeMillis()

        val sql = """
            INSERT INTO task_comments (id, task_id, user_id, content, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql, id, comment.taskId, userId, comment.content, createdAt, updatedAt
        )

        return findById(id) ?: throw RuntimeException("Failed to retrieve the saved comment.")
    }

    // Find comment by ID
    fun findById(id: String): TaskCommentResponse? {
        val sql = """
            SELECT tc.*, u.name as user_name
            FROM task_comments tc
            JOIN users u ON tc.user_id = u.id
            WHERE tc.id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            TaskCommentResponse(
                id = rs.getString("id"),
                taskId = rs.getString("task_id"),
                userId = rs.getString("user_id"),
                userName = rs.getString("user_name"),
                content = rs.getString("content"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }

    // Find all comments for a task
    fun findByTaskId(request: ListTaskComments): List<TaskCommentResponse> {
        val page = request.page ?: 1
        val size = request.size ?: 20
        val offset = (page - 1) * size

        val sql = """
            SELECT tc.*, u.name as user_name
            FROM task_comments tc
            JOIN users u ON tc.user_id = u.id
            WHERE tc.task_id = ?
            ORDER BY tc.created_at DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(request.taskId, size, offset)) { rs, _ ->
            TaskCommentResponse(
                id = rs.getString("id"),
                taskId = rs.getString("task_id"),
                userId = rs.getString("user_id"),
                userName = rs.getString("user_name"),
                content = rs.getString("content"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Update comment
    fun updateComment(id: String, content: String): TaskCommentResponse? {
        val updatedAt = System.currentTimeMillis()
        
        val sql = """
            UPDATE task_comments 
            SET content = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(sql, content, updatedAt, id)
        
        return findById(id)
    }

    // Delete comment
    fun deleteComment(id: String): Boolean {
        val sql = "DELETE FROM task_comments WHERE id = ?"
        val rowsAffected = jdbcTemplate.update(sql, id)
        return rowsAffected > 0
    }

    // Get comment count for a task
    fun getCommentCount(taskId: String): Int {
        val sql = "SELECT COUNT(*) FROM task_comments WHERE task_id = ?"
        return jdbcTemplate.queryForObject(sql, arrayOf(taskId), Int::class.java) ?: 0
    }

    // Check if user can edit/delete comment
    fun canUserModifyComment(commentId: String, userId: String): Boolean {
        val sql = "SELECT COUNT(*) FROM task_comments WHERE id = ? AND user_id = ?"
        val count = jdbcTemplate.queryForObject(sql, arrayOf(commentId, userId), Int::class.java) ?: 0
        return count > 0
    }
} 