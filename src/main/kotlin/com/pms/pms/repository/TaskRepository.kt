package com.pms.pms.repository

import com.pms.pms.model.Task
import com.pms.pms.model.TaskRequest
import com.pms.pms.model.TaskResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
class TaskRepository(private val jdbcTemplate: JdbcTemplate) {

    // Save task and assign users to the task
    fun save(taskRequest: TaskRequest, id: String, createdAt: Long, updatedAt: Long): TaskResponse {

        // Insert task into the tasks table
        val taskSql = """
            INSERT INTO tasks (id, project_id, title, description, status, priority, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            taskSql, id, taskRequest.projectId, taskRequest.title, taskRequest.description,
            taskRequest.status, taskRequest.priority, createdAt, updatedAt
        )

        // Assign the task to multiple users
        assignUsersToTask(id, taskRequest.assigneeIds, createdAt, updatedAt)

        return findById(id) ?: throw RuntimeException("Failed to retrieve the saved task.")
    }

    // Assign users to a task
    fun assignUsersToTask(taskId: String, userIds: List<String>, createdAt: Long, updatedAt: Long) {
        if (userIds.isNotEmpty()) {
            val assignSql = """
                INSERT INTO user_tasks (user_id, task_id, created_at, updated_at) 
                VALUES (?, ?, ?, ?)
            """.trimIndent()

            userIds.forEach { userId ->
                jdbcTemplate.update(assignSql, userId, taskId, createdAt, updatedAt)
            }
        }
    }

    // Find task by ID
    fun findById(id: String): TaskResponse? {
        val sql = "SELECT * FROM tasks WHERE id = ?"
        val task = jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            Task(
                id = rs.getString("id"),
                projectId = rs.getString("project_id"),
                title = rs.getString("title"),
                description = rs.getString("description"),
                status = rs.getString("status"),
                priority = rs.getString("priority"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull() ?: return null

        // Fetch assignees from user_tasks
        val assigneeSql = "SELECT user_id FROM user_tasks WHERE task_id = ?"
        val assigneeIds = jdbcTemplate.queryForList(assigneeSql, arrayOf(id), String::class.java)

        // Convert to TaskResponse
        return TaskResponse(
            id = task.id,
            projectId = task.projectId,
            title = task.title,
            description = task.description,
            assigneeIds = assigneeIds,
            status = task.status,
            priority = task.priority,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }


    // Find all tasks
    fun findAll(): List<TaskResponse> {
        val sql = "SELECT id FROM tasks"
        val taskIds = jdbcTemplate.queryForList(sql, String::class.java)

        // Use findById to fetch detailed information and convert to TaskResponse
        return taskIds.mapNotNull { taskId ->
            findById(taskId)
        }
    }

    // Find all assignees by Task ID
    fun findAssigneesByTaskId(taskId: String): List<String> {
        val sql = "SELECT user_id FROM user_tasks WHERE task_id = ?"
        return jdbcTemplate.queryForList(sql, arrayOf(taskId), String::class.java)
    }

    // Update Task
    fun update(id: String, task: TaskResponse): TaskResponse {
        val sql = """
            UPDATE tasks SET title = ?, description = ?, status = ?, priority = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, task.title, task.description, task.status, task.priority, System.currentTimeMillis(), id
        )

        // Remove all previous user assignments
        deleteUserAssignments(id)

        // Re-assign users to the task
        if (task.assigneeIds.isNotEmpty()) {
            assignUsersToTask(id, task.assigneeIds, task.createdAt, task.updatedAt)
        }

        return task
    }

    // Delete Task by ID
    fun delete(id: String) {
        // Delete from user_tasks first (foreign key dependency)
        deleteUserAssignments(id)
        val sql = "DELETE FROM tasks WHERE id = ?"
        jdbcTemplate.update(sql, id)
    }

    // Delete user assignments for a task
    fun deleteUserAssignments(taskId: String) {
        val sql = "DELETE FROM user_tasks WHERE task_id = ?"
        jdbcTemplate.update(sql, taskId)
    }

    // Find tasks by userId
    fun findTaskIdsByUserId(userId: String): List<String> {
        val sql = "SELECT task_id FROM user_tasks WHERE user_id = ?"
        return jdbcTemplate.queryForList(sql, arrayOf(userId), String::class.java)
    }
}
