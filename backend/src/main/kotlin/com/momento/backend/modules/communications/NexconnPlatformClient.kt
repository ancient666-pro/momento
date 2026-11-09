package com.momento.backend.modules.communications

import com.momento.backend.common.ApiException
import com.momento.backend.common.UserDto
import com.momento.backend.config.AppConfig
import io.ktor.http.HttpStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Server-only Nexconn Platform Chat API integration.
 *
 * The App Secret never leaves this process: requests are signed here and only
 * the issued access token is returned to the Android client. See
 * https://docs.nexconn.ai/platform-chat-api/user/register for the endpoint and
 * https://docs.nexconn.ai/platform-chat-api/webhook/overview for the signature
 * scheme (lowercase hex SHA-1 over AppSecret + Nonce + Timestamp).
 */
object NexconnPlatformClient {

    data class IssuedToken(val userId: String, val accessToken: String)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun issueAccessToken(user: UserDto): IssuedToken {
        if (!AppConfig.nexconnConfigured) {
            throw ApiException(
                HttpStatusCode.ServiceUnavailable,
                "nexconn_not_configured",
                "Nexconn is not configured on the server. Set NEXCONN_APP_ID, NEXCONN_APP_SECRET and " +
                    "NEXCONN_API_BASE_URL (regional base URL from the Nexconn dashboard)."
            )
        }
        val baseUrl = AppConfig.nexconnApiBaseUrl!!.trimEnd('/')
        val body = buildJsonObject {
            put("userId", user.id)
            put("name", user.displayName.take(64))
            if (user.avatarUrl.isNotBlank()) put("avatarUrl", user.avatarUrl)
        }.toString()

        val nonce = UUID.randomUUID().toString().replace("-", "")
        val timestamp = System.currentTimeMillis().toString()
        val signature = signature(AppConfig.nexconnAppSecret!!, nonce, timestamp)

        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/v4/auth/access-token/issue"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("App-Key", AppConfig.nexconnAppId!!)
            .header("Nonce", nonce)
            .header("Timestamp", timestamp)
            .header("Signature", signature)
            .header("X-Request-ID", UUID.randomUUID().toString())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw ApiException(
                HttpStatusCode.BadGateway,
                "nexconn_unreachable",
                "Could not reach the Nexconn platform API"
            )
        }

        if (response.statusCode() !in 200..299) {
            throw ApiException(
                HttpStatusCode.BadGateway,
                "nexconn_token_failed",
                "Nexconn rejected the token request (HTTP ${response.statusCode()})"
            )
        }

        val root = runCatching { json.parseToJsonElement(response.body()).jsonObject }.getOrNull()
            ?: throw ApiException(
                HttpStatusCode.BadGateway,
                "nexconn_invalid_response",
                "Nexconn returned an unreadable response"
            )
        val result = (root["result"] as? JsonObject)
            ?: throw ApiException(
                HttpStatusCode.BadGateway,
                "nexconn_invalid_response",
                "Nexconn response is missing the result object"
            )
        val token = result["accessToken"]?.jsonPrimitive?.content
            ?: throw ApiException(
                HttpStatusCode.BadGateway,
                "nexconn_invalid_response",
                "Nexconn response is missing an access token"
            )
        val nexconnUserId = result["userId"]?.jsonPrimitive?.content ?: user.id
        return IssuedToken(nexconnUserId, token)
    }

    /** lowercase hex SHA-1(AppSecret + Nonce + Timestamp) */
    fun signature(appSecret: String, nonce: String, timestamp: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest((appSecret + nonce + timestamp).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifySignature(appSecret: String, nonce: String, timestamp: String, provided: String): Boolean {
        val expected = signature(appSecret, nonce, timestamp)
        return MessageDigest.isEqual(expected.toByteArray(), provided.lowercase().toByteArray())
    }
}
