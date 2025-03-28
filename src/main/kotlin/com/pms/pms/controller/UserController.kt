package com.pms.pms.controller


import com.pms.pms.model.LoginInput
import com.pms.pms.model.User
import com.pms.pms.model.UserInput
import com.pms.pms.model.UserResponse
import com.pms.pms.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @PostMapping("/create")
    fun createUser(@RequestBody request: UserInput): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.createUser(request))

    @PostMapping("/login")
    fun loginUser(@RequestBody request: LoginInput): ResponseEntity<String> {

        return ResponseEntity.ok(userService.login(request))
    }

    @GetMapping("/list")
    fun getAllUsers(): ResponseEntity<List<UserResponse>> =
        ResponseEntity.ok(userService.getAllUsers())

    @GetMapping("/get/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> =
        userService.getUserById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @PostMapping("/update")
    fun updateUser(@RequestBody user: User): ResponseEntity<UserResponse> =
        userService.updateUser(user)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @DeleteMapping("/delete/{id}")
    fun deleteUser(@PathVariable id: String): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}
