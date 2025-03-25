package com.pms.pms.repository


import com.pms.pms.model.Project
import com.pms.pms.model.ProjectRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProjectRepository(private val jdbcTemplate: JdbcTemplate) {

    // Save a new project and return the saved project using findById
    fun save(projectRequest: ProjectRequest, id: String, createdAt: Long, updatedAt: Long): Project {
        val sql = """
            INSERT INTO projects (id, name, description, start_date, end_date, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql, id, projectRequest.name, projectRequest.description, projectRequest.startDate,
            projectRequest.endDate, createdAt, updatedAt
        )

        return findById(id) ?: throw RuntimeException("Failed to retrieve the saved project.")
    }

    // Find a project by its ID
    fun findById(id: String): Project? {
        val sql = "SELECT * FROM projects WHERE id = ?"
        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            Project(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                startDate = rs.getLong("start_date"),
                endDate = rs.getLong("end_date"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }

    // Find all projects
    fun findAll(): List<Project> {
        val sql = "SELECT * FROM projects"
        return jdbcTemplate.query(sql) { rs, _ ->
            Project(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                startDate = rs.getLong("start_date"),
                endDate = rs.getLong("end_date"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Update project by ID
    fun update(id: String, project: Project) {
        val sql = """
            UPDATE projects 
            SET name = ?, description = ?, start_date = ?, end_date = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(
            sql, project.name, project.description, project.startDate,
            project.endDate, project.updatedAt, id
        )
    }

    // Delete a project by ID
    fun delete(id: String) {
        val sql = "DELETE FROM projects WHERE id = ?"
        val rowsAffected = jdbcTemplate.update(sql, id)

        if (rowsAffected == 0) {
            throw RuntimeException("No project found with ID: $id")
        }
    }
}

