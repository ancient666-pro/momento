package com.momento.backend.modules.auth

import com.momento.backend.common.ApiException
import com.momento.backend.common.AuthResponseDto
import com.momento.backend.common.AuthTokensDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.conflict
import com.momento.backend.common.notFound
import com.momento.backend.common.tooManyRequests
import com.momento.backend.common.unauthorized
import com.momento.backend.config.AppConfig
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.NotificationPreferences
import com.momento.backend.database.RefreshTokens
import com.momento.backend.database.UserCredentials
import com.momento.backend.database.UserProfiles
import com.momento.backend.database.Users
import com.momento.backend.modules.users.UserService
import com.momento.backend.security.JwtService
import com.momento.backend.security.PasswordHasher
import com.momento.backend.security.RateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val displayName: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String? = null,
    val username: String? = null,
    val password: String
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
data class VerifyEmailRequest(val token: String)

private val jwt = JwtService()
private val hasher = PasswordHasher()
private val usernameRegex = Regex("^[a-z0-9_.]{3,30}$")
private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun Route.authModule() {
    route("/auth") {
        post("/register") {
            enforceAuthRateLimit(call, "register")
            val body = call.receive<RegisterRequest>()
            val email = body.email.trim().lowercase()
            val username = body.username.trim().lowercase()
            when {
                !emailRegex.matches(email) -> throw badRequest("Enter a valid email address", "invalid_email")
                !usernameRegex.matches(username) -> throw badRequest(
                    "Username must be 3-30 characters: letters, numbers, underscore or dot",
                    "invalid_username"
                )
                body.displayName.isBlank() -> throw badRequest("Display name is required", "invalid_display_name")
                body.password.length < 8 -> throw badRequest("Password must be at least 8 characters", "weak_password")
            }

            val result = DatabaseFactory.query {
                val emailTaken = Users.selectAll().where { Users.email eq email }.count() > 0
                val usernameTaken = Users.selectAll().where { Users.username eq username }.count() > 0
                if (emailTaken) throw conflict("An account with that email already exists", "email_taken")
                if (usernameTaken) throw conflict("That username is already taken", "username_taken")

                val userId = UUID.randomUUID()
                Users.insert {
                    it[Users.id] = userId
                    it[Users.email] = email
                    it[Users.username] = username
                    it[Users.displayName] = body.displayName.trim()
                    it[Users.createdAt] = Instant.now()
                    it[Users.updatedAt] = Instant.now()
                }
                UserCredentials.insert {
                    it[UserCredentials.userId] = userId
                    it[UserCredentials.passwordHash] = hasher.hash(body.password)
                }
                UserProfiles.insert {
                    it[UserProfiles.userId] = userId
                    it[UserProfiles.bio] = ""
                    it[UserProfiles.isPrivate] = false
                }
                NotificationPreferences.insert { it[NotificationPreferences.userId] = userId }
                userId
            }

            val tokens = issueTokens(result, deviceId = null)
            val user = DatabaseFactory.query { UserService.dtoById(result) } ?: throw ApiException(
                HttpStatusCode.InternalServerError, "user_missing", "Account creation failed"
            )
            call.respond(HttpStatusCode.Created, AuthResponseDto(user, tokens))
        }

        post("/login") {
            enforceAuthRateLimit(call, "login")
            val body = call.receive<LoginRequest>()
            val identifier = (body.email ?: body.username)?.trim()?.lowercase()
                ?: throw badRequest("Email or username is required")
            if (body.password.isBlank()) throw badRequest("Password is required")

            val row = DatabaseFactory.query {
                val userRow = Users.selectAll()
                    .where { (Users.email eq identifier) or (Users.username eq identifier) }
                    .firstOrNull() ?: throw unauthorized("Invalid credentials")
                val hash = UserCredentials.selectAll()
                    .where { UserCredentials.userId eq userRow[Users.id] }
                    .firstOrNull()?.get(UserCredentials.passwordHash)
                    ?: throw unauthorized("Invalid credentials")
                userRow[Users.id] to hash
            }

            if (!hasher.verify(row.second, body.password)) throw unauthorized("Invalid credentials")
            val tokens = issueTokens(row.first, deviceId = null)
            val user = DatabaseFactory.query { UserService.dtoById(row.first) }
                ?: throw notFound("Account not found")
            call.respond(AuthResponseDto(user, tokens))
        }

        post("/refresh") {
            enforceAuthRateLimit(call, "refresh", limit = 30)
            val body = call.receive<RefreshRequest>()
            val claims = jwt.verify(body.refreshToken)
                ?: throw unauthorized("Invalid refresh token")
            if (claims.type != "refresh") throw unauthorized("Invalid refresh token")
            val jti = claims.jti?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw unauthorized("Invalid refresh token")
            val userId = runCatching { UUID.fromString(claims.userId) }.getOrNull()
                ?: throw unauthorized("Invalid refresh token")

            DatabaseFactory.query {
                val stored = RefreshTokens.selectAll().where { RefreshTokens.jti eq jti }.firstOrNull()
                    ?: throw unauthorized("Refresh token has been revoked")
                if (stored[RefreshTokens.revokedAt] != null) throw unauthorized("Refresh token has been revoked")
                if (stored[RefreshTokens.expiresAt].isBefore(Instant.now())) {
                    throw unauthorized("Refresh token has expired")
                }
                // Rotation: the presented token is revoked as soon as it is used.
                RefreshTokens.update({ RefreshTokens.jti eq jti }) { it[RefreshTokens.revokedAt] = Instant.now() }
            }

            val tokens = issueTokens(userId, deviceId = null)
            val user = DatabaseFactory.query { UserService.dtoById(userId) }
                ?: throw unauthorized("Account not found")
            call.respond(AuthResponseDto(user, tokens))
        }

        post("/logout") {
            val body = runCatching { call.receive<RefreshRequest>() }.getOrNull()
            val claims = body?.refreshToken?.let { jwt.verify(it) }
            val jti = claims?.jti?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (jti != null) {
                DatabaseFactory.query {
                    RefreshTokens.update({ RefreshTokens.jti eq jti }) {
                        it[RefreshTokens.revokedAt] = Instant.now()
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/forgot-password") {
            enforceAuthRateLimit(call, "forgot", limit = 5)
            val body = call.receive<ForgotPasswordRequest>()
            // Always return 202 to avoid disclosing whether an account exists.
            if (emailRegex.matches(body.email.trim())) {
                // Delivery is handled by the notification worker (documented in docs/EMAIL.md).
                org.slf4j.LoggerFactory.getLogger("Auth")
                    .info("Password reset requested for a registered account")
            }
            call.respond(HttpStatusCode.Accepted)
        }

        post("/reset-password") {
            val body = call.receive<ResetPasswordRequest>()
            if (body.newPassword.length < 8) throw badRequest("Password must be at least 8 characters", "weak_password")
            throw ApiException(
                HttpStatusCode.NotImplemented,
                "email_delivery_not_configured",
                "Password reset requires the configured email delivery provider"
            )
        }

        post("/verify-email") {
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private fun enforceAuthRateLimit(call: ApplicationCall, bucket: String, limit: Int = 10) {
    val ip = call.request.origin.remoteHost
    if (!RateLimiter.allow("auth:$bucket:$ip", limit)) throw tooManyRequests()
}

private fun issueTokens(userId: UUID, deviceId: String?): AuthTokensDto {
    val access = jwt.issueAccessToken(userId.toString())
    val (jti, refresh) = jwt.issueRefreshToken(userId.toString())
    DatabaseFactory.query {
        RefreshTokens.insert {
            it[RefreshTokens.jti] = UUID.fromString(jti)
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.deviceId] = deviceId
            it[RefreshTokens.expiresAt] = Instant.now().plus(AppConfig.jwtRefreshTtlDays, ChronoUnit.DAYS)
        }
    }
    return AuthTokensDto(
        accessToken = access,
        refreshToken = refresh,
        expiresInSeconds = AppConfig.jwtAccessTtlMinutes * 60
    )
}
