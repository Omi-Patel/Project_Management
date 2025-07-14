package com.pms.pms.repository

import com.pms.pms.model.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class WorkspaceRepository(private val jdbcTemplate: JdbcTemplate) {

    // Save a new workspace
    fun save(workspaceRequest: WorkspaceRequest, id: String, createdAt: Long, updatedAt: Long): Workspace {
        val sql = """
            INSERT INTO workspaces (id, name, description, owner_id, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        jdbcTemplate.update(
            sql,
            id,
            workspaceRequest.name,
            workspaceRequest.description,
            workspaceRequest.ownerId,
            createdAt,
            updatedAt
        )

        return Workspace(
            id = id,
            name = workspaceRequest.name,
            description = workspaceRequest.description,
            ownerId = workspaceRequest.ownerId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // Find workspace by ID
    fun findById(id: String): WorkspaceResponse? {
        val sql = """
            SELECT w.*, 
                   COUNT(DISTINCT wm.user_id) as member_count,
                   COUNT(DISTINCT p.id) as project_count
            FROM workspaces w
            LEFT JOIN workspace_members wm ON w.id = wm.workspace_id
            LEFT JOIN projects p ON w.id = p.workspace_id
            WHERE w.id = ?
            GROUP BY w.id
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            WorkspaceResponse(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                ownerId = rs.getString("owner_id"),
                memberCount = rs.getInt("member_count"),
                projectCount = rs.getInt("project_count"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }

    // Find all workspaces for a user (owned or member of)
    fun findAllForUser(userId: String, search: String? = null): List<WorkspaceResponse> {
        val baseSql = """
            SELECT w.*, 
                   COUNT(DISTINCT wm2.user_id) as member_count,
                   COUNT(DISTINCT p.id) as project_count
            FROM workspaces w
            LEFT JOIN workspace_members wm ON w.id = wm.workspace_id
            LEFT JOIN workspace_members wm2 ON w.id = wm2.workspace_id
            LEFT JOIN projects p ON w.id = p.workspace_id
            WHERE w.owner_id = ? OR wm.user_id = ?
        """.trimIndent()

        val searchCondition = if (search != null && search.isNotBlank()) {
            " AND (w.name ILIKE ? OR w.description ILIKE ?)"
        } else ""

        val sql = baseSql + searchCondition + " GROUP BY w.id ORDER BY w.created_at DESC"

        val params = if (search != null && search.isNotBlank()) {
            arrayOf(userId, userId, "%$search%", "%$search%")
        } else {
            arrayOf(userId, userId)
        }

        return jdbcTemplate.query(sql, params) { rs, _ ->
            WorkspaceResponse(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                ownerId = rs.getString("owner_id"),
                memberCount = rs.getInt("member_count"),
                projectCount = rs.getInt("project_count"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Find all workspaces (admin function)
    fun findAll(): List<WorkspaceResponse> {
        val sql = """
            SELECT w.*, 
                   COUNT(DISTINCT wm.user_id) as member_count,
                   COUNT(DISTINCT p.id) as project_count
            FROM workspaces w
            LEFT JOIN workspace_members wm ON w.id = wm.workspace_id
            LEFT JOIN projects p ON w.id = p.workspace_id
            GROUP BY w.id
            ORDER BY w.created_at DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            WorkspaceResponse(
                id = rs.getString("id"),
                name = rs.getString("name"),
                description = rs.getString("description"),
                ownerId = rs.getString("owner_id"),
                memberCount = rs.getInt("member_count"),
                projectCount = rs.getInt("project_count"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Update workspace
    fun update(id: String, workspace: WorkspaceResponse) {
        val sql = """
            UPDATE workspaces 
            SET name = ?, description = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(sql, workspace.name, workspace.description, workspace.updatedAt, id)
    }

    // Delete workspace
    fun delete(id: String) {
        try {
            // First delete all related records in the correct order to maintain referential integrity
            
            // 1. Delete all user-task assignments for tasks in projects of this workspace
            val deleteUserTasksSql = """
                DELETE FROM user_tasks 
                WHERE task_id IN (
                    SELECT t.id 
                    FROM tasks t 
                    JOIN projects p ON t.project_id = p.id 
                    WHERE p.workspace_id = ?
                )
            """.trimIndent()
            jdbcTemplate.update(deleteUserTasksSql, id)
            
            // 2. Delete all tasks associated with projects in this workspace
            val deleteTasksSql = """
                DELETE FROM tasks 
                WHERE project_id IN (
                    SELECT id FROM projects WHERE workspace_id = ?
                )
            """.trimIndent()
            jdbcTemplate.update(deleteTasksSql, id)
            
            // 3. Delete all projects in this workspace
            val deleteProjectsSql = "DELETE FROM projects WHERE workspace_id = ?"
            jdbcTemplate.update(deleteProjectsSql, id)
            
            // 4. Delete workspace members
            val deleteMembersSql = "DELETE FROM workspace_members WHERE workspace_id = ?"
            jdbcTemplate.update(deleteMembersSql, id)
            
            // 5. Delete workspace invitations
            val deleteInvitationsSql = "DELETE FROM workspace_invitations WHERE workspace_id = ?"
            jdbcTemplate.update(deleteInvitationsSql, id)
            
            // 6. Finally delete the workspace
            val sql = "DELETE FROM workspaces WHERE id = ?"
            val rowsAffected = jdbcTemplate.update(sql, id)

            if (rowsAffected == 0) {
                throw RuntimeException("No workspace found with ID: $id")
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            println("Database constraint error during workspace deletion: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to delete workspace due to database constraints: ${e.message}")
        }
    }

    // Add member to workspace
    fun addMember(workspaceId: String, userId: String, role: String = "MEMBER") {
        val sql = """
            INSERT INTO workspace_members (id, workspace_id, user_id, role, joined_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val currentTime = System.currentTimeMillis()
        jdbcTemplate.update(sql, UUID.randomUUID().toString(), workspaceId, userId, role, currentTime, currentTime)
    }

    // Remove member from workspace
    fun removeMember(workspaceId: String, userId: String) {
        val sql = "DELETE FROM workspace_members WHERE workspace_id = ? AND user_id = ?"
        jdbcTemplate.update(sql, workspaceId, userId)
    }

    // Get workspace members
    fun getMembers(workspaceId: String): List<WorkspaceMemberResponse> {
        val sql = """
            SELECT wm.*, u.name as user_name, u.email as user_email
            FROM workspace_members wm
            JOIN users u ON wm.user_id = u.id
            WHERE wm.workspace_id = ?
            ORDER BY wm.joined_at ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(workspaceId)) { rs, _ ->
            WorkspaceMemberResponse(
                id = rs.getString("id"),
                workspaceId = rs.getString("workspace_id"),
                userId = rs.getString("user_id"),
                userName = rs.getString("user_name"),
                userEmail = rs.getString("user_email"),
                role = rs.getString("role"),
                joinedAt = rs.getLong("joined_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Check if user is member of workspace
    fun isMember(workspaceId: String, userId: String): Boolean {
        val sql = """
            SELECT COUNT(*) FROM workspace_members 
            WHERE workspace_id = ? AND user_id = ?
        """.trimIndent()

        val count = jdbcTemplate.queryForObject(sql, arrayOf(workspaceId, userId), Int::class.java)
        return count != null && count > 0
    }

    // Check if user is owner of workspace
    fun isOwner(workspaceId: String, userId: String): Boolean {
        val sql = "SELECT COUNT(*) FROM workspaces WHERE id = ? AND owner_id = ?"
        val count = jdbcTemplate.queryForObject(sql, arrayOf(workspaceId, userId), Int::class.java)
        return count != null && count > 0
    }

    // Create invitation
    fun createInvitation(invitation: WorkspaceInvitationRequest, id: String, expiresAt: Long): WorkspaceInvitation {
        val sql = """
            INSERT INTO workspace_invitations (id, workspace_id, invited_email, invited_by, expires_at, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val currentTime = System.currentTimeMillis()
        jdbcTemplate.update(
            sql,
            id,
            invitation.workspaceId,
            invitation.invitedEmail,
            invitation.invitedBy,
            expiresAt,
            currentTime,
            currentTime
        )

        return WorkspaceInvitation(
            id = id,
            workspaceId = invitation.workspaceId,
            invitedEmail = invitation.invitedEmail,
            invitedBy = invitation.invitedBy,
            expiresAt = expiresAt,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    // Get invitation by ID
    fun getInvitation(id: String): WorkspaceInvitationResponse? {
        val sql = """
            SELECT wi.*, w.name as workspace_name, u.name as inviter_name
            FROM workspace_invitations wi
            JOIN workspaces w ON wi.workspace_id = w.id
            JOIN users u ON wi.invited_by = u.id
            WHERE wi.id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, arrayOf(id)) { rs, _ ->
            WorkspaceInvitationResponse(
                id = rs.getString("id"),
                workspaceId = rs.getString("workspace_id"),
                workspaceName = rs.getString("workspace_name"),
                invitedEmail = rs.getString("invited_email"),
                invitedBy = rs.getString("invited_by"),
                inviterName = rs.getString("inviter_name"),
                status = rs.getString("status"),
                expiresAt = rs.getLong("expires_at"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }.firstOrNull()
    }

    // Update invitation status
    fun updateInvitationStatus(id: String, status: String) {
        val sql = """
            UPDATE workspace_invitations 
            SET status = ?, updated_at = ?
            WHERE id = ?
        """.trimIndent()

        jdbcTemplate.update(sql, status, System.currentTimeMillis(), id)
    }

    // Get pending invitations for email
    fun getPendingInvitationsByEmail(email: String): List<WorkspaceInvitationResponse> {
        val sql = """
            SELECT wi.*, w.name as workspace_name, u.name as inviter_name
            FROM workspace_invitations wi
            JOIN workspaces w ON wi.workspace_id = w.id
            JOIN users u ON wi.invited_by = u.id
            WHERE wi.invited_email = ? AND wi.status = 'PENDING' AND wi.expires_at > ?
        """.trimIndent()

        val currentTime = System.currentTimeMillis()
        return jdbcTemplate.query(sql, arrayOf(email, currentTime)) { rs, _ ->
            WorkspaceInvitationResponse(
                id = rs.getString("id"),
                workspaceId = rs.getString("workspace_id"),
                workspaceName = rs.getString("workspace_name"),
                invitedEmail = rs.getString("invited_email"),
                invitedBy = rs.getString("invited_by"),
                inviterName = rs.getString("inviter_name"),
                status = rs.getString("status"),
                expiresAt = rs.getLong("expires_at"),
                createdAt = rs.getLong("created_at"),
                updatedAt = rs.getLong("updated_at")
            )
        }
    }

    // Delete expired invitations
    fun deleteExpiredInvitations() {
        val sql = "DELETE FROM workspace_invitations WHERE expires_at < ?"
        jdbcTemplate.update(sql, System.currentTimeMillis())
    }

    // Update member role
    fun updateMemberRole(workspaceId: String, userId: String, newRole: String) {
        val sql = """
            UPDATE workspace_members 
            SET role = ?, updated_at = ?
            WHERE workspace_id = ? AND user_id = ?
        """.trimIndent()

        val currentTime = System.currentTimeMillis()
        jdbcTemplate.update(sql, newRole, currentTime, workspaceId, userId)
    }
} 