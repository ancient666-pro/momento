package com.momento.backend.modules.notifications

import com.momento.backend.common.NotificationDto
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.NotificationPreferences
import com.momento.backend.database.Notifications
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class NotificationPreferencesDto(
    val newFollower: Boolean = true,
    val likes: Boolean = true,
    val comments: Boolean = true,
    val mentions: Boolean = true,
    val directMessages: Boolean = true,
    val calls: Boolean = true,
    val podActivity: Boolean = true
)

fun Route.notificationsModule() {
    route("/notifications") {
        get {
            val userId = call.requireUserId()
            val page = call.pageParam()
            val items = DatabaseFactory.query {
                Notifications.selectAll()
                    .where { Notifications.userId eq userId }
                    .orderBy(Notifications.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .map { row ->
                        NotificationDto(
                            id = row[Notifications.id].toString(),
                            kind = row[Notifications.kind],
                            payload = row[Notifications.payload],
                            readAtMillis = row[Notifications.readAt]?.toEpochMilli(),
                            createdAtMillis = row[Notifications.createdAt].toEpochMilli()
                        )
                    }
            }
            call.respond(paged(items, page))
        }

        get("/preferences") {
            val userId = call.requireUserId()
            val dto = DatabaseFactory.query {
                val row = NotificationPreferences.selectAll()
                    .where { NotificationPreferences.userId eq userId }
                    .firstOrNull()
                if (row == null) {
                    NotificationPreferences.insert { it[NotificationPreferences.userId] = userId }
                    NotificationPreferencesDto()
                } else {
                    NotificationPreferencesDto(
                        newFollower = row[NotificationPreferences.newFollower],
                        likes = row[NotificationPreferences.likes],
                        comments = row[NotificationPreferences.comments],
                        mentions = row[NotificationPreferences.mentions],
                        directMessages = row[NotificationPreferences.directMessages],
                        calls = row[NotificationPreferences.calls],
                        podActivity = row[NotificationPreferences.podActivity]
                    )
                }
            }
            call.respond(dto)
        }

        patch("/preferences") {
            val userId = call.requireUserId()
            val body = call.receive<NotificationPreferencesDto>()
            DatabaseFactory.query {
                val exists = NotificationPreferences.selectAll()
                    .where { NotificationPreferences.userId eq userId }.count() > 0
                if (!exists) {
                    NotificationPreferences.insert { it[NotificationPreferences.userId] = userId }
                }
                NotificationPreferences.update({ NotificationPreferences.userId eq userId }) {
                    it[NotificationPreferences.newFollower] = body.newFollower
                    it[NotificationPreferences.likes] = body.likes
                    it[NotificationPreferences.comments] = body.comments
                    it[NotificationPreferences.mentions] = body.mentions
                    it[NotificationPreferences.directMessages] = body.directMessages
                    it[NotificationPreferences.calls] = body.calls
                    it[NotificationPreferences.podActivity] = body.podActivity
                }
            }
            call.respond(body)
        }

        post("/read-all") {
            val userId = call.requireUserId()
            DatabaseFactory.query {
                Notifications.update({ (Notifications.userId eq userId) and Notifications.readAt.isNull() }) {
                    it[readAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        patch("/{notificationId}/read") {
            val userId = call.requireUserId()
            val id = call.pathUuid("notificationId")
            DatabaseFactory.query {
                Notifications.update({
                    (Notifications.id eq id) and (Notifications.userId eq userId)
                }) {
                    it[readAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
