package com.pms.pms.config

import com.pms.pms.model.User
import com.pms.pms.model.UserResponse


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String


    fun generateAccessToken(user: UserResponse, userDetails: UserDetails): String {
        return Jwts.builder()
            .setSubject(userDetails.username) // Username as subject
            .setHeaderParam("alg", "HS256")
            .setHeaderParam("typ", "JWT") // Setting Token Type
            .claim("userId", user.id) // Adding userId
            .claim("email", user.email) // Adding email
            .claim("roles", listOf(user.role)) // Adding roles
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour expiry
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    fun generateRefreshToken(username: String): String {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 days expiry
            .signWith(getSignKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUsername(token: String): String {
        return extractClaims(token).subject
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        return extractUsername(token) == userDetails.username && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractClaims(token).expiration.before(Date())
    }

    private fun extractClaims(token: String): Claims {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).body
    }

    private fun getSignKey(): Key {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun extractRoles(token: String): List<String> {
        val claims = extractClaims(token)
        return claims["roles"] as? List<String> ?: emptyList()
    }

    // Extract the user ID from the token
    fun getUserIdFromToken(token: String): String {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .body

            // Get the userId from the claims
            claims.get("userId", String::class.java) ?: throw Exception("User ID not found in token")
        } catch (e: Exception) {
            println(e.message)
            throw Exception("Invalid token")
        }
    }
}