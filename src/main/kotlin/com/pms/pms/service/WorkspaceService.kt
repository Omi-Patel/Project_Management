package com.pms.pms.service

import com.pms.pms.model.*
import com.pms.pms.repository.ProjectRepository
import com.pms.pms.repository.WorkspaceRepository
import com.pms.pms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val emailService: EmailService
) {

    @Transactional
    fun createWorkspace(workspaceRequest: WorkspaceRequest): WorkspaceResponse {
        val workspaceId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()

        // Create the workspace
        val workspace = workspaceRepository.save(workspaceRequest, workspaceId, currentTime, currentTime)

        // Add the owner as the first member
        workspaceRepository.addMember(workspaceId, workspaceRequest.ownerId, "OWNER")

        return workspaceRepository.findById(workspaceId) 
            ?: throw RuntimeException("Failed to retrieve created workspace")
    }

    fun getWorkspace(workspaceId: String, userId: String): WorkspaceResponse {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Check if user has access to this workspace
        if (!workspaceRepository.isMember(workspaceId, userId) && !workspaceRepository.isOwner(workspaceId, userId)) {
            throw RuntimeException("Access denied to workspace")
        }

        return workspace
    }

    fun getWorkspaceProjects(workspaceId: String, userId: String): List<Project> {
        // Check if user has access to this workspace
        if (!workspaceRepository.isMember(workspaceId, userId) && !workspaceRepository.isOwner(workspaceId, userId)) {
            throw RuntimeException("Access denied to workspace")
        }

        // Get all projects for the workspace
        return projectRepository.findAll(null).filter { it.workspaceId == workspaceId }
    }

    fun getWorkspaceProjectsForAdmin(workspaceId: String): List<Project> {
        // Admin can access all workspace projects without user authentication
        return projectRepository.findAll(null).filter { it.workspaceId == workspaceId }
    }

    fun getWorkspaceMembersForAdmin(workspaceId: String): List<WorkspaceMemberResponse> {
        // Admin can access all workspace members without user authentication
        return workspaceRepository.getMembers(workspaceId)
    }

    fun getWorkspacesForUser(userId: String, search: String? = null): List<WorkspaceResponse> {
        return workspaceRepository.findAllForUser(userId, search)
    }

    fun getAllWorkspaces(): List<WorkspaceResponse> {
        return workspaceRepository.findAll()
    }

    @Transactional
    fun updateWorkspace(workspaceId: String, workspaceRequest: WorkspaceRequest, userId: String): WorkspaceResponse {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Check if user is owner
        if (!workspaceRepository.isOwner(workspaceId, userId)) {
            throw RuntimeException("Only workspace owner can update workspace")
        }

        val updatedWorkspace = workspace.copy(
            name = workspaceRequest.name,
            description = workspaceRequest.description,
            updatedAt = System.currentTimeMillis()
        )

        workspaceRepository.update(workspaceId, updatedWorkspace)

        return workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Failed to retrieve updated workspace")
    }

    @Transactional
    fun deleteWorkspace(workspaceId: String, userId: String) {
        try {
            val workspace = workspaceRepository.findById(workspaceId)
                ?: throw RuntimeException("Workspace not found")

            // Check if user is owner
            if (!workspaceRepository.isOwner(workspaceId, userId)) {
                throw RuntimeException("Only workspace owner can delete workspace")
            }

            workspaceRepository.delete(workspaceId)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            println("Database error during workspace deletion: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to delete workspace: ${e.message}")
        }
    }

    @Transactional
    fun inviteUser(invitationRequest: WorkspaceInvitationRequest): WorkspaceInvitation {
        val workspace = workspaceRepository.findById(invitationRequest.workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Check if inviter is owner or admin
        if (!workspaceRepository.isOwner(invitationRequest.workspaceId, invitationRequest.invitedBy)) {
            throw RuntimeException("Only workspace owner can invite users")
        }

        // Check if user is already a member
        val user = userRepository.findByEmail(invitationRequest.invitedEmail)
        if (user != null && workspaceRepository.isMember(invitationRequest.workspaceId, user.id)) {
            throw RuntimeException("User is already a member of this workspace")
        }

        val invitationId = UUID.randomUUID().toString()
        val expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days

        val invitation = workspaceRepository.createInvitation(invitationRequest, invitationId, expiresAt)

        // Send invitation email
        val inviter = userRepository.findById(invitationRequest.invitedBy)
            ?: throw RuntimeException("Inviter not found")

        try {
            emailService.sendWorkspaceInvitation(
                invitationRequest.invitedEmail,
                workspace.name,
                inviter.name,
                invitationId
            )
        } catch (e: Exception) {
            // Log the error but don't fail the invitation creation
            println("Failed to send invitation email: ${e.message}")
        }

        return invitation
    }

    @Transactional
    fun acceptInvitation(acceptRequest: AcceptInvitationRequest): WorkspaceResponse {
        val invitation = workspaceRepository.getInvitation(acceptRequest.invitationId)
            ?: throw RuntimeException("Invitation not found")

        // Check if invitation is expired
        if (System.currentTimeMillis() > invitation.expiresAt) {
            throw RuntimeException("Invitation has expired")
        }

        // Check if invitation is still pending
        if (invitation.status != "PENDING") {
            throw RuntimeException("Invitation has already been processed")
        }

        // Add user to workspace
        workspaceRepository.addMember(invitation.workspaceId, acceptRequest.userId)

        // Update invitation status
        workspaceRepository.updateInvitationStatus(acceptRequest.invitationId, "ACCEPTED")

        return workspaceRepository.findById(invitation.workspaceId)
            ?: throw RuntimeException("Failed to retrieve workspace")
    }

    @Transactional
    fun declineInvitation(invitationId: String, userId: String) {
        val invitation = workspaceRepository.getInvitation(invitationId)
            ?: throw RuntimeException("Invitation not found")

        // Check if invitation is expired
        if (System.currentTimeMillis() > invitation.expiresAt) {
            throw RuntimeException("Invitation has expired")
        }

        // Check if invitation is still pending
        if (invitation.status != "PENDING") {
            throw RuntimeException("Invitation has already been processed")
        }

        // Update invitation status
        workspaceRepository.updateInvitationStatus(invitationId, "DECLINED")
    }

    fun getPendingInvitations(email: String): List<WorkspaceInvitationResponse> {
        return workspaceRepository.getPendingInvitationsByEmail(email)
    }

    fun getWorkspaceMembers(workspaceId: String, userId: String): List<WorkspaceMemberResponse> {
        // Check if user has access to this workspace
        if (!workspaceRepository.isMember(workspaceId, userId) && !workspaceRepository.isOwner(workspaceId, userId)) {
            throw RuntimeException("Access denied to workspace")
        }

        return workspaceRepository.getMembers(workspaceId)
    }

    @Transactional
    fun removeMember(workspaceId: String, memberUserId: String, requesterUserId: String) {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Check if requester is owner
        if (!workspaceRepository.isOwner(workspaceId, requesterUserId)) {
            throw RuntimeException("Only workspace owner can remove members")
        }

        // Cannot remove the owner
        if (workspace.ownerId == memberUserId) {
            throw RuntimeException("Cannot remove workspace owner")
        }

        workspaceRepository.removeMember(workspaceId, memberUserId)
    }

    @Transactional
    fun leaveWorkspace(workspaceId: String, userId: String) {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Cannot leave if you're the owner
        if (workspace.ownerId == userId) {
            throw RuntimeException("Workspace owner cannot leave. Transfer ownership or delete the workspace.")
        }

        workspaceRepository.removeMember(workspaceId, userId)
    }

    fun cleanupExpiredInvitations() {
        workspaceRepository.deleteExpiredInvitations()
    }

    @Transactional
    fun updateMemberRole(workspaceId: String, memberUserId: String, newRole: String, requesterUserId: String) {
        val workspace = workspaceRepository.findById(workspaceId)
            ?: throw RuntimeException("Workspace not found")

        // Check if requester is owner
        if (!workspaceRepository.isOwner(workspaceId, requesterUserId)) {
            throw RuntimeException("Only workspace owner can update member roles")
        }

        // Cannot change owner's role
        if (workspace.ownerId == memberUserId) {
            throw RuntimeException("Cannot change workspace owner's role")
        }

        // Validate role
        if (!listOf("ADMIN", "MEMBER").contains(newRole)) {
            throw RuntimeException("Invalid role. Must be ADMIN or MEMBER")
        }

        workspaceRepository.updateMemberRole(workspaceId, memberUserId, newRole)
    }
} 