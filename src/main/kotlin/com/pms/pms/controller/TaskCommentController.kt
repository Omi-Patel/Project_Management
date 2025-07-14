package com.pms.pms.controller

import com.pms.pms.model.*
import com.pms.pms.service.TaskCommentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/v1/task-comments")
class TaskCommentController(private val taskCommentService: TaskCommentService) {

    // Create a new comment
    @PostMapping("/create")
    fun createComment(
        @RequestBody comment: TaskCommentRequest,
        @RequestParam userId: String
    ): ResponseEntity<TaskCommentResponse> {
        return try {
            val createdComment = taskCommentService.createComment(comment, userId)
            ResponseEntity.ok(createdComment)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Get all comments for a task
    @PostMapping("/list")
    fun getCommentsForTask(@RequestBody request: ListTaskComments): ResponseEntity<List<TaskCommentResponse>> {
        return try {
            val comments = taskCommentService.getCommentsForTask(request)
            ResponseEntity.ok(comments)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Get comment by ID
    @GetMapping("/get/{id}")
    fun getCommentById(@PathVariable id: String): ResponseEntity<TaskCommentResponse> {
        return try {
            val comment = taskCommentService.getCommentById(id)
            comment?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Update comment
    @PutMapping("/update/{id}")
    fun updateComment(
        @PathVariable id: String,
        @RequestBody content: String,
        @RequestParam userId: String
    ): ResponseEntity<TaskCommentResponse> {
        return try {
            val updatedComment = taskCommentService.updateComment(id, content, userId)
            updatedComment?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Delete comment
    @DeleteMapping("/delete/{id}")
    fun deleteComment(
        @PathVariable id: String,
        @RequestParam userId: String
    ): ResponseEntity<Void> {
        return try {
            val deleted = taskCommentService.deleteComment(id, userId)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Get comment count for a task
    @GetMapping("/count/{taskId}")
    fun getCommentCount(@PathVariable taskId: String): ResponseEntity<Int> {
        return try {
            val count = taskCommentService.getCommentCount(taskId)
            ResponseEntity.ok(count)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
} 