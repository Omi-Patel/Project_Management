package com.pms.pms.model

import org.springframework.data.annotation.Id
import java.time.Instant

data class Workspace(
    @Id
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

data class WorkspaceRequest(
    val name: String,
    val description: String?,
    val ownerId: String
)

data class WorkspaceResponse(
    val id: String,
    val name: String,
    val description: String?,
    val ownerId: String,
    val memberCount: Int,
    val projectCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class WorkspaceInvitation(
    @Id
    val id: String,
    val workspaceId: String,
    val invitedEmail: String,
    val invitedBy: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED
    val expiresAt: Long,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

data class WorkspaceInvitationRequest(
    val workspaceId: String,
    val invitedEmail: String,
    val invitedBy: String
)

data class WorkspaceInvitationResponse(
    val id: String,
    val workspaceId: String,
    val workspaceName: String,
    val invitedEmail: String,
    val invitedBy: String,
    val inviterName: String,
    val status: String,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class WorkspaceMember(
    @Id
    val id: String,
    val workspaceId: String,
    val userId: String,
    val role: String = "MEMBER", // OWNER, ADMIN, MEMBER
    val joinedAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli()
)

data class WorkspaceMemberResponse(
    val id: String,
    val workspaceId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val role: String,
    val joinedAt: Long,
    val updatedAt: Long
)

data class AcceptInvitationRequest(
    val invitationId: String,
    val userId: String
)

data class WorkspaceListRequest(
    val userId: String,
    val search: String? = null,
    val page: Int = 1,
    val size: Int = 10
) 