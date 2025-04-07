package com.pms.pms.repository

import com.pms.pms.model.*
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
    fun findAll(input: ListTask): List<TaskResponse> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        // Base SQL Query
        val baseSql = """
        SELECT t.id 
        FROM tasks t
        LEFT JOIN user_tasks ut ON t.id = ut.task_id
    """.trimIndent()

        // Filter by userId (if provided)
        if (input.userId!!.isNotBlank()) {
            conditions.add("ut.user_id = ?")
            params.add(input.userId)
        }

        // Search filter
        if (!input.search.isNullOrBlank()) {
            conditions.add("(t.title ILIKE ? OR t.description ILIKE ?)")
            params.add("%${input.search}%")
            params.add("%${input.search}%")
        }

        // Pagination
        val page = input.page ?: 1
        val size = input.size ?: 10
        val offset = (page - 1) * size

        // Build final SQL query
        val sql = buildString {
            append(baseSql)
            if (conditions.isNotEmpty()) {
                append(" WHERE ").append(conditions.joinToString(" AND "))
            }
            append(" ORDER BY t.created_at DESC")
            append(" LIMIT ? OFFSET ?")
        }

        // Add pagination params
        params.add(size)
        params.add(offset)

        // Execute query and get task IDs
        val taskIds = jdbcTemplate.queryForList(sql, params.toTypedArray(), String::class.java)

        // Fetch full task details
        return taskIds.mapNotNull { taskId ->
            findById(taskId)
        }
    }

    fun findAllByProjectId(input: ListProjectTask): List<TaskResponse> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        // Base SQL Query for filtering tasks by projectId
        val baseSql = """
    SELECT t.id 
    FROM tasks t
    """.trimIndent()

        // Filter by projectId (if provided)
        if (!input.projectId.isNullOrBlank()) {
            conditions.add("t.project_id = ?")
            params.add(input.projectId)
        }

        // Search filter (task title or description)
        if (!input.search.isNullOrBlank()) {
            conditions.add("(t.title ILIKE ? OR t.description ILIKE ?)")
            params.add("%${input.search}%")
            params.add("%${input.search}%")
        }

        // Pagination
        val page = input.page ?: 1
        val size = input.size ?: 10
        val offset = (page - 1) * size

        // Build final SQL query
        val sql = buildString {
            append(baseSql)
            if (conditions.isNotEmpty()) {
                append(" WHERE ").append(conditions.joinToString(" AND "))
            }
            append(" ORDER BY t.created_at DESC")
            append(" LIMIT ? OFFSET ?")
        }

        // Add pagination params
        params.add(size)
        params.add(offset)

        // Execute query to fetch task IDs
        val taskIds = jdbcTemplate.queryForList(sql, params.toTypedArray(), String::class.java)

        // Fetch the tasks for each taskId
        val tasks = taskIds.mapNotNull { taskId ->
            findById(taskId)  // Fetch each task details by its ID
        }

        return tasks
    }


    // Find all assignees by Task ID
    fun findAssigneesByTaskId(taskId: String): List<String> {
        val sql = "SELECT user_id FROM user_tasks WHERE task_id = ?"
        return jdbcTemplate.queryForList(sql, arrayOf(taskId), String::class.java)
    }

    // Update Task
    fun update(id: String, task: TaskResponse, updatedAt: Long): TaskResponse {
        val sql = """
            UPDATE tasks SET title = ?, description = ?, status = ?, priority = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, task.title, task.description, task.status, task.priority, updatedAt, id
        )

        // Remove all previous user assignments
        deleteUserAssignments(id)

        // Re-assign users to the task
        if (task.assigneeIds.isNotEmpty()) {
            assignUsersToTask(id, task.assigneeIds, task.createdAt, task.updatedAt)
        }

        return task
    }

    fun updateTaskStatus(taskId: String, newStatus: String, updatedAt: Long): String {
        val sql = "UPDATE tasks SET status = ?, updated_at = ? WHERE id = ?"
        jdbcTemplate.update(sql, newStatus, updatedAt, taskId)
        return "Task Updated Successfully"
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

    fun findTaskIdsByProjectId(id: String): List<String> {
        val sql = "SELECT id FROM tasks WHERE project_id = ?"
        return jdbcTemplate.queryForList(sql, arrayOf(id), String::class.java)
    }
}
