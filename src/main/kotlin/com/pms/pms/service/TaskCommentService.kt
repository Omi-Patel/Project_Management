package com.pms.pms.service

import com.pms.pms.model.*
import com.pms.pms.repository.TaskCommentRepository
import org.springframework.stereotype.Service

@Service
class TaskCommentService(private val taskCommentRepository: TaskCommentRepository) {

    // Create a new comment
    fun createComment(comment: TaskCommentRequest, userId: String): TaskCommentResponse {
        return taskCommentRepository.createComment(comment, userId)
    }

    // Get all comments for a task
    fun getCommentsForTask(request: ListTaskComments): List<TaskCommentResponse> {
        return taskCommentRepository.findByTaskId(request)
    }

    // Get comment by ID
    fun getCommentById(id: String): TaskCommentResponse? {
        return taskCommentRepository.findById(id)
    }

    // Update comment
    fun updateComment(id: String, content: String, userId: String): TaskCommentResponse? {
        // Check if user can modify this comment
        if (!taskCommentRepository.canUserModifyComment(id, userId)) {
            throw RuntimeException("You don't have permission to modify this comment")
        }
        
        return taskCommentRepository.updateComment(id, content)
    }

    // Delete comment
    fun deleteComment(id: String, userId: String): Boolean {
        // Check if user can modify this comment
        if (!taskCommentRepository.canUserModifyComment(id, userId)) {
            throw RuntimeException("You don't have permission to delete this comment")
        }
        
        return taskCommentRepository.deleteComment(id)
    }

    // Get comment count for a task
    fun getCommentCount(taskId: String): Int {
        return taskCommentRepository.getCommentCount(taskId)
    }
} 