package com.pms.pms.service

import com.pms.pms.repository.AuthRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val authRepository: AuthRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        // Fetch user details from DB using the existing function
        val user = authRepository.findUserAuthByUsername(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")

        // Return UserDetails object
        return User(
            user.email,
            user.hashPassword,
            emptyList() // Assuming roles are not yet implemented in `user_auth` table
        )
    }
}