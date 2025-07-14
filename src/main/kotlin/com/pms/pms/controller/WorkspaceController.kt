package com.pms.pms.controller

import com.pms.pms.model.*
import com.pms.pms.service.WorkspaceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceController(private val workspaceService: WorkspaceService) {

    @PostMapping
    fun createWorkspace(@RequestBody workspaceRequest: WorkspaceRequest): ResponseEntity<WorkspaceResponse> {
        return try {
            val workspace = workspaceService.createWorkspace(workspaceRequest)
            ResponseEntity.ok(workspace)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{workspaceId}")
    fun getWorkspace(
        @PathVariable workspaceId: String,
        @RequestParam userId: String
    ): ResponseEntity<WorkspaceResponse> {
        return try {
            val workspace = workspaceService.getWorkspace(workspaceId, userId)
            ResponseEntity.ok(workspace)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping
    fun getWorkspacesForUser(
        @RequestParam userId: String,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<List<WorkspaceResponse>> {
        return try {
            val workspaces = workspaceService.getWorkspacesForUser(userId, search)
            ResponseEntity.ok(workspaces)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/admin/all")
    fun getAllWorkspaces(): ResponseEntity<List<WorkspaceResponse>> {
        return try {
            val workspaces = workspaceService.getAllWorkspaces()
            ResponseEntity.ok(workspaces)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{workspaceId}")
    fun updateWorkspace(
        @PathVariable workspaceId: String,
        @RequestBody workspaceRequest: WorkspaceRequest,
        @RequestParam userId: String
    ): ResponseEntity<WorkspaceResponse> {
        return try {
            val workspace = workspaceService.updateWorkspace(workspaceId, workspaceRequest, userId)
            ResponseEntity.ok(workspace)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{workspaceId}")
    fun deleteWorkspace(
        @PathVariable workspaceId: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        return try {
            workspaceService.deleteWorkspace(workspaceId, userId)
            ResponseEntity.ok().build()
        } catch (e: RuntimeException) {
            println("Delete workspace error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            println("Unexpected error deleting workspace: ${e.message}")
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{workspaceId}/invite")
    fun inviteUser(
        @PathVariable workspaceId: String,
        @RequestBody invitationRequest: WorkspaceInvitationRequest
    ): ResponseEntity<WorkspaceInvitation> {
        return try {
            val invitation = workspaceService.inviteUser(invitationRequest)
            ResponseEntity.ok(invitation)
        } catch (e: Exception) {
            println("Invitation error: ${e.message}")
            e.printStackTrace()
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/invitations/{invitationId}/accept")
    fun acceptInvitation(@RequestBody acceptRequest: AcceptInvitationRequest): ResponseEntity<WorkspaceResponse> {
        return try {
            val workspace = workspaceService.acceptInvitation(acceptRequest)
            ResponseEntity.ok(workspace)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/invitations/{invitationId}/decline")
    fun declineInvitation(
        @PathVariable invitationId: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        return try {
            workspaceService.declineInvitation(invitationId, userId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/invitations/pending")
    fun getPendingInvitations(@RequestParam email: String): ResponseEntity<List<WorkspaceInvitationResponse>> {
        return try {
            val invitations = workspaceService.getPendingInvitations(email)
            ResponseEntity.ok(invitations)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{workspaceId}/members/{memberUserId}")
    fun removeMember(
        @PathVariable workspaceId: String,
        @PathVariable memberUserId: String,
        @RequestParam requesterUserId: String
    ): ResponseEntity<Void> {
        return try {
            workspaceService.removeMember(workspaceId, memberUserId, requesterUserId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{workspaceId}/leave")
    fun leaveWorkspace(
        @PathVariable workspaceId: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        return try {
            workspaceService.leaveWorkspace(workspaceId, userId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{workspaceId}/projects")
    fun getWorkspaceProjects(
        @PathVariable workspaceId: String,
        @RequestParam userId: String
    ): ResponseEntity<List<Project>> {
        return try {
            val projects = workspaceService.getWorkspaceProjects(workspaceId, userId)
            ResponseEntity.ok(projects)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/admin/{workspaceId}/projects")
    fun getWorkspaceProjectsForAdmin(
        @PathVariable workspaceId: String
    ): ResponseEntity<List<Project>> {
        return try {
            val projects = workspaceService.getWorkspaceProjectsForAdmin(workspaceId)
            ResponseEntity.ok(projects)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/admin/{workspaceId}/members")
    fun getWorkspaceMembersForAdmin(
        @PathVariable workspaceId: String
    ): ResponseEntity<List<WorkspaceMemberResponse>> {
        return try {
            val members = workspaceService.getWorkspaceMembersForAdmin(workspaceId)
            ResponseEntity.ok(members)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{workspaceId}/members")
    fun getWorkspaceMembers(
        @PathVariable workspaceId: String,
        @RequestParam userId: String
    ): ResponseEntity<List<WorkspaceMemberResponse>> {
        return try {
            val members = workspaceService.getWorkspaceMembers(workspaceId, userId)
            ResponseEntity.ok(members)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{workspaceId}/members/{memberUserId}/role")
    fun updateMemberRole(
        @PathVariable workspaceId: String,
        @PathVariable memberUserId: String,
        @RequestParam newRole: String,
        @RequestParam requesterUserId: String
    ): ResponseEntity<Void> {
        return try {
            workspaceService.updateMemberRole(workspaceId, memberUserId, newRole, requesterUserId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
} 