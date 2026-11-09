package com.momento.backend.modules.webhooks

import com.momento.backend.common.ApiException
import com.momento.backend.common.badRequest
import com.momento.backend.config.AppConfig
import com.momento.backend.database.CommunicationEvents
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.WebhookEvents
import com.momento.backend.modules.communications.NexconnPlatformClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun Route.webhooksModule() {
    route("/webhooks/nexconn") {
        post("/chat") { handleWebhook(call, "chat") }
        post("/call") { handleWebhook(call, "call") }
    }
}

private suspend fun handleWebhook(call: ApplicationCall, source: String) {
    if (!AppConfig.nexconnConfigured) {
        throw ApiException(
            HttpStatusCode.ServiceUnavailable,
            "nexconn_not_configured",
            "Nexconn webhooks require NEXCONN_APP_ID, NEXCONN_APP_SECRET and NEXCONN_API_BASE_URL"
        )
    }

    val appKey = call.request.header("AppKey") ?: call.request.header("App-Key")
        ?: throw badRequest("Missing AppKey header")
    if (appKey != AppConfig.nexconnAppId) throw ApiException(
        HttpStatusCode.Unauthorized, "invalid_app_key", "Unknown AppKey"
    )

    val nonce = call.request.header("Nonce") ?: throw badRequest("Missing Nonce header")
    val timestamp = call.request.header("Timestamp") ?: throw badRequest("Missing Timestamp header")
    val signature = call.request.header("Signature") ?: throw badRequest("Missing Signature header")

    // Timestamp is Unix milliseconds; reject stale or future-dated deliveries.
    val timestampMillis = timestamp.toLongOrNull() ?: throw badRequest("Invalid Timestamp header")
    val skew = kotlin.math.abs(System.currentTimeMillis() - timestampMillis)
    if (skew > 5 * 60 * 1000) throw ApiException(
        HttpStatusCode.Unauthorized, "stale_webhook", "Webhook timestamp is outside the allowed window"
    )

    if (!NexconnPlatformClient.verifySignature(AppConfig.nexconnAppSecret!!, nonce, timestamp, signature)) {
        throw ApiException(HttpStatusCode.Unauthorized, "invalid_signature", "Webhook signature mismatch")
    }

    val body = call.receiveText()
    val root: JsonObject = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        ?: throw badRequest("Webhook body must be a JSON object")

    val eventId = call.request.header("X-Event-ID")
        ?: call.request.header("Event-Id")
        ?: sequenceOf("eventId", "event_id", "id")
            .mapNotNull { root[it]?.jsonPrimitive?.content }
            .firstOrNull()
        ?: throw badRequest("Webhook is missing an event identifier")

    val eventType = sequenceOf("eventType", "event_type", "type", "event")
        .mapNotNull { root[it]?.jsonPrimitive?.content }
        .firstOrNull() ?: "unknown"

    val duplicate = DatabaseFactory.query {
        val exists = WebhookEvents.selectAll().where { WebhookEvents.eventId eq eventId }.count() > 0
        if (!exists) {
            WebhookEvents.insert {
                it[WebhookEvents.eventId] = eventId
                it[WebhookEvents.webhookSource] = source
                it[WebhookEvents.receivedAt] = Instant.now()
                it[WebhookEvents.attempts] = 1
                it[WebhookEvents.status] = "processing"
            }
            CommunicationEvents.insert {
                it[CommunicationEvents.eventType] = "$source:$eventType"
                it[CommunicationEvents.payload] = body
                it[CommunicationEvents.createdAt] = Instant.now()
            }
            false
        } else {
            true
        }
    }

    if (!duplicate) {
        DatabaseFactory.query {
            WebhookEvents.update({ WebhookEvents.eventId eq eventId }) {
                it[processedAt] = Instant.now()
                it[status] = "processed"
            }
        }
    }

    call.respond(HttpStatusCode.OK, mapOf("status" to if (duplicate) "duplicate" else "accepted"))
}
