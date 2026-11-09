package com.momento.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.momento.backend.config.AppConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

enum class TokenType { ACCESS, REFRESH }

class JwtService {

    private val algorithm: Algorithm = Algorithm.HMAC256(AppConfig.jwtSecret)

    fun issueAccessToken(userId: String): String =
        issue(userId, TokenType.ACCESS, AppConfig.jwtAccessTtlMinutes * 60)

    fun issueRefreshToken(userId: String): Pair<String, String> {
        val jti = UUID.randomUUID().toString()
        return jti to issue(userId, TokenType.REFRESH, AppConfig.jwtRefreshTtlDays * 24 * 60 * 60, jti)
    }

    fun verify(token: String): JwtClaims? = try {
        val decoded = JWT.require(algorithm)
            .withIssuer(AppConfig.jwtIssuer)
            .build()
            .verify(token)
        JwtClaims(
            userId = decoded.subject,
            type = decoded.getClaim("typ").asString(),
            jti = decoded.id
        )
    } catch (e: Exception) {
        null
    }

    private fun issue(userId: String, type: TokenType, ttlSeconds: Long, jti: String? = null): String =
        JWT.create()
            .withIssuer(AppConfig.jwtIssuer)
            .withSubject(userId)
            .withClaim("typ", type.name.lowercase())
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(ttlSeconds)))
            .let { builder -> if (jti != null) builder.withJWTId(jti) else builder }
            .sign(algorithm)
}

data class JwtClaims(
    val userId: String,
    val type: String,
    val jti: String?
)
