package com.momento.backend.common

import com.momento.backend.security.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.http.HttpHeaders
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String
)

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String
) : RuntimeException(message)

fun badRequest(message: String, code: String = "bad_request") =
    ApiException(HttpStatusCode.BadRequest, code, message)

fun unauthorized(message: String = "Authentication required") =
    ApiException(HttpStatusCode.Unauthorized, "unauthorized", message)

fun forbidden(message: String = "You do not have permission to do that") =
    ApiException(HttpStatusCode.Forbidden, "forbidden", message)

fun notFound(message: String = "Not found") =
    ApiException(HttpStatusCode.NotFound, "not_found", message)

fun conflict(message: String, code: String = "conflict") =
    ApiException(HttpStatusCode.Conflict, code, message)

fun tooManyRequests(message: String = "Too many requests") =
    ApiException(HttpStatusCode.TooManyRequests, "rate_limited", message)

/** Reads a page index (0-based) from the query string. */
fun ApplicationCall.pageParam(): Int =
    request.queryParameters["page"]?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0

const val PAGE_SIZE = 20

fun <T> paged(items: List<T>, page: Int, pageSize: Int = PAGE_SIZE): PageResponse<T> =
    PageResponse(
        items = items,
        nextCursor = if (items.size >= pageSize) (page + 1).toString() else null
    )

private val jwt by lazy { JwtService() }

/** Resolves the authenticated user from the Bearer access token, if present and valid. */
fun ApplicationCall.currentUserIdOrNull(): UUID? {
    val header = request.header(HttpHeaders.Authorization) ?: return null
    if (!header.startsWith("Bearer ", ignoreCase = true)) return null
    val token = header.substringAfter(' ').trim()
    val claims = jwt.verify(token) ?: return null
    if (claims.type != "access") return null
    return runCatching { UUID.fromString(claims.userId) }.getOrNull()
}

fun ApplicationCall.requireUserId(): UUID = currentUserIdOrNull()
    ?: throw unauthorized()

fun ApplicationCall.pathUuid(name: String): UUID {
    val raw = parameters[name] ?: throw badRequest("Missing path parameter '$name'")
    return runCatching { UUID.fromString(raw) }
        .getOrElse { throw badRequest("Invalid identifier '$name'") }
}
