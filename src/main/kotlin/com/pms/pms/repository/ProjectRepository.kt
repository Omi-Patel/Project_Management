package com.pms.pms.repository


import com.pms.pms.model.Project
import com.pms.pms.model.ProjectRequest
import com.pms.pms.model.ProjectResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProjectRepository(private val jdbcTemplate: JdbcTemplate) {

    // Save a new project and return the saved project using findById
    fun save(projectRequest: ProjectRequest, id: String, createdAt: Long, updatedAt: Long): Project {
        val sql = """
            INSERT INTO projects (id, name, description, user_id, workspace_id, color, start_date, end_date, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql,
            id,
            projectRequest.name,
            projectRequest.description,
            projectRequest.userId,
            projectRequest.workspaceId,
            projectRequest.color,
            projectRequest.startDate,
            projectRequest.endDate,
            createdAt,
            updatedAt
        )

        return Project(
            id = id,
            name = projectRequest.name,
            description = projectRequest.description,
            userId = projectRequest.userId,
            workspaceId = projectRequest.workspaceId,
            color = projectRequest.color,
            startDate = projectRequest.startDate,
            endDate = projectRequest.endDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // Find a project by its ID
    fun findById(id: String): ProjectResponse? {
        // First query to fetch project details
        val projectSql = "SELECT * FROM projects WHERE id = ?"
        val project = jdbcTemplate.query(projectSql, arrayOf(id)) { rs, _ ->
            ProjectResponse(
                id = rs.getString("id"),
                name = rs.getString("name"),
                taskIds = emptyList(), // Placeholder for now
                description = rs.getString("description"),
                userId = rs.getString("user_id"),
                workspaceId = rs.getString("workspace_id"),
                color = rs.getString("color"),
                startDate = rs.getLong("start_date").takeIf { !rs.wasNull() },
                endDate = rs.getLong("end_date").takeIf { !rs.wasNull() },
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull() ?: return null

        // Second query to fetch task IDs
        val taskSql = "SELECT id FROM tasks WHERE project_id = ?"
        val taskIds = jdbcTemplate.query(taskSql, arrayOf(id)) { rs, _ ->
            rs.getString("id")
        }

        // Return the project response with task IDs
        return project.copy(taskIds = taskIds)
    }


    // Find all projects
    fun findAll(userId: String?): List<Project> {
        val sql: String
        val params: Array<Any?>

        if (userId != null) {
            sql = """
          SELECT DISTINCT p.*
        FROM projects p
        JOIN tasks t ON t.project_id = p.id
        JOIN user_tasks ut ON ut.task_id = t.id
        WHERE ut.user_id = ?
        
        UNION
        
        SELECT *
        FROM projects p
        WHERE p.user_id = ?
    """.trimIndent()
            params = arrayOf(userId, userId)
        } else {
            sql = "SELECT * FROM projects"
            params = emptyArray()
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            Project(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                userId = rs.getString("user_id"),
                workspaceId = rs.getString("workspace_id"),
                color = rs.getString("color"),
                startDate = rs.getLong("start_date"),
                endDate = rs.getLong("end_date"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }


    // Update project by ID
    fun update(id: String, project: ProjectResponse) {
        val sql = """
            UPDATE projects 
            SET name = ?, description = ?, color = ?, start_date = ?, end_date = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, project.name, project.description, project.color, project.startDate,
            project.endDate, project.updatedAt, id
        )
    }

    // Delete a project by ID
    fun delete(id: String) {
        try {
            // First delete all related records in the correct order to maintain referential integrity
            
            // 1. Delete all user-task assignments for tasks in this project
            val deleteUserTasksSql = """
                DELETE FROM user_tasks 
                WHERE task_id IN (
                    SELECT id FROM tasks WHERE project_id = ?
                )
            """.trimIndent()
            jdbcTemplate.update(deleteUserTasksSql, id)
            
            // 2. Delete all tasks in this project
            val deleteTasksSql = "DELETE FROM tasks WHERE project_id = ?"
            jdbcTemplate.update(deleteTasksSql, id)
            
            // 3. Finally delete the project
            val sql = "DELETE FROM projects WHERE id = ?"
            val rowsAffected = jdbcTemplate.update(sql, id)

            if (rowsAffected == 0) {
                throw RuntimeException("No project found with ID: $id")
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            println("Database constraint error during project deletion: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to delete project due to database constraints: ${e.message}")
        }
    }
}

