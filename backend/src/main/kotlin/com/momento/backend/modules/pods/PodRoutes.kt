package com.momento.backend.modules.pods

import com.momento.backend.common.EditorialDispatchCardDto
import com.momento.backend.common.EssayCardDto
import com.momento.backend.common.LiveAudioPodCardDto
import com.momento.backend.common.LiveCountDto
import com.momento.backend.common.MediaReelCardDto
import com.momento.backend.common.PodFeedDto
import com.momento.backend.common.PodPollCardDto
import com.momento.backend.common.PodPollOptionDto
import com.momento.backend.common.PodTopicDto
import com.momento.backend.common.SoundtrackCardDto
import com.momento.backend.common.UserDto
import com.momento.backend.common.badRequest
import com.momento.backend.common.conflict
import com.momento.backend.common.currentUserIdOrNull
import com.momento.backend.common.forbidden
import com.momento.backend.common.notFound
import com.momento.backend.common.pageParam
import com.momento.backend.common.paged
import com.momento.backend.common.pathUuid
import com.momento.backend.common.requireUserId
import com.momento.backend.database.ContentReports
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.database.EditorialDispatches
import com.momento.backend.database.Essays
import com.momento.backend.database.PodMembers
import com.momento.backend.database.PodPolls
import com.momento.backend.database.PodSessions
import com.momento.backend.database.PodTopics
import com.momento.backend.database.Pods
import com.momento.backend.database.PollOptions
import com.momento.backend.database.PollVotes
import com.momento.backend.database.Posts
import com.momento.backend.database.Soundtracks
import com.momento.backend.database.Users
import com.momento.backend.modules.posts.PostService
import com.momento.backend.modules.users.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

@Serializable
data class CreatePodRequest(
    val title: String,
    val description: String = "",
    val topicHashtag: String? = null,
    val visibility: String = "public",
    val artworkUrl: String? = null,
    val audioOnly: Boolean = true,
    val coHostIds: List<String> = emptyList(),
    val startLive: Boolean = false
)

@Serializable
data class UpdatePodRequest(
    val title: String? = null,
    val description: String? = null,
    val topicHashtag: String? = null,
    val visibility: String? = null,
    val artworkUrl: String? = null
)

@Serializable
data class CreatePollRequest(
    val question: String,
    val description: String = "",
    val options: List<String>,
    val durationHours: Int = 24
)

@Serializable
data class VoteRequest(val optionId: String)

@Serializable
data class PodDto(
    val id: String,
    val title: String,
    val description: String,
    val host: UserDto,
    val live: Boolean,
    val listenerCount: Long,
    val topicHashtag: String? = null,
    val visibility: String,
    val artworkUrl: String? = null,
    val audioOnly: Boolean,
    val role: String? = null
)

fun Route.podsModule() {
    route("/pods") {
        get("/feed") {
            val viewer = call.currentUserIdOrNull()
            val feed = DatabaseFactory.query { buildPodFeed(viewer) }
            call.respond(feed)
        }

        get("/topics") {
            val page = call.pageParam()
            val topics = DatabaseFactory.query {
                PodTopics.selectAll()
                    .orderBy(PodTopics.participantCount, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(com.momento.backend.common.PAGE_SIZE).offset((page * com.momento.backend.common.PAGE_SIZE).toLong())
                    .map { row ->
                        PodTopicDto(
                            id = row[PodTopics.id].toString(),
                            name = row[PodTopics.name],
                            participantCount = row[PodTopics.participantCount],
                            live = row[PodTopics.live]
                        )
                    }
            }
            call.respond(paged(topics, page))
        }

        get("/live-count") {
            val count = DatabaseFactory.query {
                Pods.selectAll().where { Pods.live eq true }.sumOf { it[Pods.listenerCount] }
            }
            call.respond(LiveCountDto(count))
        }

        post {
            val userId = call.requireUserId()
            val body = call.receive<CreatePodRequest>()
            if (body.title.isBlank()) throw badRequest("Pod title is required")
            if (body.title.length > 120) throw badRequest("Pod title is too long")
            if (body.visibility !in setOf("public", "private")) throw badRequest("Invalid visibility")
            val podId = DatabaseFactory.query {
                val id = UUID.randomUUID()
                Pods.insert {
                    it[Pods.id] = id
                    it[Pods.hostId] = userId
                    it[Pods.title] = body.title.trim()
                    it[Pods.description] = body.description.take(1000)
                    it[Pods.topicHashtag] = body.topicHashtag?.removePrefix("#")?.lowercase()
                    it[Pods.visibility] = body.visibility
                    it[Pods.artworkUrl] = body.artworkUrl
                    it[Pods.audioOnly] = body.audioOnly
                    it[Pods.live] = body.startLive
                    it[Pods.createdAt] = Instant.now()
                }
                PodMembers.insert {
                    it[PodMembers.podId] = id
                    it[PodMembers.userId] = userId
                    it[PodMembers.role] = "host"
                    it[PodMembers.joinedAt] = Instant.now()
                }
                body.coHostIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                    .filter { it != userId }
                    .distinct()
                    .forEach { coHost ->
                        val exists = Users.selectAll().where { Users.id eq coHost }.count() > 0
                        if (exists) {
                            PodMembers.insert {
                                it[PodMembers.podId] = id
                                it[PodMembers.userId] = coHost
                                it[PodMembers.role] = "co_host"
                                it[PodMembers.joinedAt] = Instant.now()
                            }
                        }
                    }
                if (body.startLive) {
                    PodSessions.insert {
                        it[PodSessions.podId] = id
                        it[PodSessions.startedAt] = Instant.now()
                    }
                }
                body.topicHashtag?.removePrefix("#")?.lowercase()?.takeIf { it.isNotBlank() }?.let { tag ->
                    ensureTopic(tag, live = body.startLive)
                }
                id
            }
            val created = DatabaseFactory.query { podDto(podId, userId) } ?: throw notFound("Pod not found")
            call.respond(HttpStatusCode.Created, created)
        }

        get("/{podId}") {
            val podId = call.pathUuid("podId")
            val viewer = call.currentUserIdOrNull()
            val dto = DatabaseFactory.query { podDto(podId, viewer) } ?: throw notFound("Pod not found")
            call.respond(dto)
        }

        patch("/{podId}") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            val body = call.receive<UpdatePodRequest>()
            DatabaseFactory.query {
                requireHost(podId, userId)
                Pods.update({ Pods.id eq podId }) {
                    body.title?.let { t -> it[Pods.title] = t.take(120) }
                    body.description?.let { d -> it[Pods.description] = d.take(1000) }
                    body.topicHashtag?.let { t -> it[Pods.topicHashtag] = t.removePrefix("#").lowercase() }
                    body.visibility?.let { v ->
                        if (v !in setOf("public", "private")) throw badRequest("Invalid visibility")
                        it[Pods.visibility] = v
                    }
                    body.artworkUrl?.let { a -> it[Pods.artworkUrl] = a }
                }
            }
            call.respond(DatabaseFactory.query { podDto(podId, userId) } ?: throw notFound("Pod not found"))
        }

        delete("/{podId}") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            DatabaseFactory.query {
                requireHost(podId, userId)
                Pods.update({ Pods.id eq podId }) {
                    it[deletedAt] = Instant.now()
                    it[live] = false
                }
                PodSessions.update({ (PodSessions.podId eq podId) and PodSessions.endedAt.isNull() }) {
                    it[endedAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{podId}/join") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            val result = DatabaseFactory.query {
                val pod = Pods.selectAll()
                    .where { (Pods.id eq podId) and Pods.deletedAt.isNull() }
                    .firstOrNull() ?: throw notFound("Pod not found")
                val member = PodMembers.selectAll()
                    .where { (PodMembers.podId eq podId) and (PodMembers.userId eq userId) }
                    .firstOrNull()
                if (member == null && pod[Pods.visibility] == "private") {
                    throw forbidden("This Pod is private")
                }
                if (member == null) {
                    PodMembers.insert {
                        it[PodMembers.podId] = podId
                        it[PodMembers.userId] = userId
                        it[PodMembers.role] = "listener"
                        it[PodMembers.joinedAt] = Instant.now()
                    }
                    val openSession = PodSessions.selectAll()
                        .where { (PodSessions.podId eq podId) and PodSessions.endedAt.isNull() }
                        .count() > 0
                    if (!openSession) {
                        PodSessions.insert {
                            it[PodSessions.podId] = podId
                            it[PodSessions.startedAt] = Instant.now()
                        }
                    }
                    Pods.update({ Pods.id eq podId }) {
                        it[listenerCount] = Pods.listenerCount + 1
                        it[live] = true
                    }
                }
                podDto(podId, userId) ?: throw notFound("Pod not found")
            }
            call.respond(result)
        }

        post("/{podId}/leave") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            DatabaseFactory.query {
                val member = PodMembers.selectAll()
                    .where { (PodMembers.podId eq podId) and (PodMembers.userId eq userId) }
                    .firstOrNull() ?: throw notFound("You are not in this Pod")
                val role = member[PodMembers.role]
                if (role == "host") {
                    PodSessions.update({ (PodSessions.podId eq podId) and PodSessions.endedAt.isNull() }) {
                        it[endedAt] = Instant.now()
                    }
                    Pods.update({ Pods.id eq podId }) {
                        it[live] = false
                        it[listenerCount] = 0
                    }
                    PodMembers.deleteWhere { PodMembers.podId eq podId }
                } else {
                    PodMembers.deleteWhere { (PodMembers.podId eq podId) and (PodMembers.userId eq userId) }
                    Pods.update({ Pods.id eq podId }) {
                        it[Pods.listenerCount] = Pods.listenerCount - 1
                    }
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{podId}/polls") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            val body = call.receive<CreatePollRequest>()
            if (body.question.isBlank()) throw badRequest("Poll question is required")
            val options = body.options.map { it.trim() }.filter { it.isNotBlank() }
            if (options.size < 2) throw badRequest("A poll needs at least two options")
            if (options.size > 6) throw badRequest("A poll can have at most six options")
            val pollId = DatabaseFactory.query {
                val pod = Pods.selectAll().where { (Pods.id eq podId) and Pods.deletedAt.isNull() }
                    .firstOrNull() ?: throw notFound("Pod not found")
                if (pod[Pods.hostId] != userId) throw forbidden("Only the host can add polls")
                val id = UUID.randomUUID()
                PodPolls.insert {
                    it[PodPolls.id] = id
                    it[PodPolls.podId] = podId
                    it[PodPolls.authorId] = userId
                    it[PodPolls.question] = body.question.take(300)
                    it[PodPolls.description] = body.description.take(600)
                    it[PodPolls.endsAt] = Instant.now().plusSeconds(body.durationHours.coerceIn(1, 168) * 3600L)
                    it[PodPolls.createdAt] = Instant.now()
                }
                options.forEachIndexed { index, option ->
                    PollOptions.insert {
                        it[PollOptions.pollId] = id
                        it[PollOptions.text] = option.take(120)
                        it[PollOptions.position] = index
                    }
                }
                id
            }
            call.respond(HttpStatusCode.Created, DatabaseFactory.query { pollCard(pollId, userId) })
        }

        post("/{podId}/polls/{pollId}/vote") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            val pollId = call.pathUuid("pollId")
            val body = call.receive<VoteRequest>()
            val optionId = runCatching { UUID.fromString(body.optionId) }.getOrElse {
                throw badRequest("Invalid option")
            }
            val card = DatabaseFactory.query {
                val poll = PodPolls.selectAll()
                    .where { (PodPolls.id eq pollId) and (PodPolls.podId eq podId) }
                    .firstOrNull() ?: throw notFound("Poll not found")
                poll[PodPolls.endsAt]?.let { if (it.isBefore(Instant.now())) throw conflict("This poll has ended", "poll_ended") }
                val optionValid = PollOptions.selectAll()
                    .where { (PollOptions.id eq optionId) and (PollOptions.pollId eq pollId) }
                    .count() > 0
                if (!optionValid) throw badRequest("That option does not belong to this poll")
                val existing = PollVotes.selectAll().where { PollVotes.pollId eq pollId and (PollVotes.userId eq userId) }
                    .firstOrNull()
                if (existing != null) {
                    throw conflict("You have already voted in this poll", "already_voted")
                }
                PollVotes.insert {
                    it[PollVotes.pollId] = pollId
                    it[PollVotes.optionId] = optionId
                    it[PollVotes.userId] = userId
                    it[PollVotes.createdAt] = Instant.now()
                }
                pollCard(pollId, userId)
            }
            call.respond(card)
        }

        get("/{podId}/polls/{pollId}/results") {
            val podId = call.pathUuid("podId")
            val pollId = call.pathUuid("pollId")
            val viewer = call.currentUserIdOrNull()
            val exists = DatabaseFactory.query {
                PodPolls.selectAll().where { (PodPolls.id eq pollId) and (PodPolls.podId eq podId) }.count() > 0
            }
            if (!exists) throw notFound("Poll not found")
            call.respond(DatabaseFactory.query { pollCard(pollId, viewer) })
        }

        post("/{podId}/report") {
            val userId = call.requireUserId()
            val podId = call.pathUuid("podId")
            val body = call.receive<ReportRequest>()
            DatabaseFactory.query {
                ContentReports.insert {
                    it[ContentReports.reporterId] = userId
                    it[ContentReports.targetType] = "pod"
                    it[ContentReports.targetId] = podId
                    it[ContentReports.reason] = body.reason.take(120)
                    it[ContentReports.details] = body.details?.take(1000)
                    it[ContentReports.status] = "open"
                    it[ContentReports.createdAt] = Instant.now()
                }
            }
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

@Serializable
data class ReportRequest(val reason: String, val details: String? = null)

private fun requireHost(podId: UUID, userId: UUID) {
    val pod = Pods.selectAll().where { (Pods.id eq podId) and Pods.deletedAt.isNull() }
        .firstOrNull() ?: throw notFound("Pod not found")
    if (pod[Pods.hostId] != userId) throw forbidden("Only the host can do that")
}

private fun ensureTopic(tag: String, live: Boolean) {
    val existing = PodTopics.selectAll().where { PodTopics.name eq tag }.firstOrNull()
    if (existing == null) {
        PodTopics.insert {
            it[PodTopics.name] = tag
            it[PodTopics.participantCount] = 0
            it[PodTopics.live] = live
            it[PodTopics.createdAt] = Instant.now()
        }
    } else {
        PodTopics.update({ PodTopics.name eq tag }) {
            if (live) it[PodTopics.live] = true
        }
    }
}

private fun podDto(podId: UUID, viewer: UUID?): PodDto? {
    val row = Pods.selectAll().where { (Pods.id eq podId) and Pods.deletedAt.isNull() }
        .firstOrNull() ?: return null
    val host = UserService.dtoById(row[Pods.hostId]) ?: return null
    val role = viewer?.let { v ->
        PodMembers.selectAll()
            .where { (PodMembers.podId eq podId) and (PodMembers.userId eq v) }
            .firstOrNull()?.get(PodMembers.role)
    }
    return PodDto(
        id = podId.toString(),
        title = row[Pods.title],
        description = row[Pods.description],
        host = host,
        live = row[Pods.live],
        listenerCount = row[Pods.listenerCount],
        topicHashtag = row[Pods.topicHashtag],
        visibility = row[Pods.visibility],
        artworkUrl = row[Pods.artworkUrl],
        audioOnly = row[Pods.audioOnly],
        role = role
    )
}

private fun buildPodFeed(viewer: UUID?): PodFeedDto {
    val polls = PodPolls.selectAll()
        .orderBy(PodPolls.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(10)
        .map { it[PodPolls.id] }
        .mapNotNull { runCatching { pollCard(it, viewer) }.getOrNull() }

    val livePods = Pods.selectAll()
        .where { (Pods.live eq true) and Pods.deletedAt.isNull() }
        .orderBy(Pods.listenerCount, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(5)
        .mapNotNull { row ->
            val podId = row[Pods.id]
            val hosts = PodMembers.selectAll()
                .where { (PodMembers.podId eq podId) and (PodMembers.role inList listOf("host", "co_host")) }
                .map { it[PodMembers.userId] }
            val session = PodSessions.selectAll()
                .where { (PodSessions.podId eq podId) and PodSessions.endedAt.isNull() }
                .firstOrNull()
            val elapsed = session?.get(PodSessions.startedAt)
                ?.let { Duration.between(it, Instant.now()).seconds.toInt() } ?: 0
            LiveAudioPodCardDto(
                id = podId.toString(),
                title = row[Pods.title],
                hosts = UserService.dtosByIds(hosts).values.toList(),
                listenerCount = row[Pods.listenerCount],
                positionSeconds = elapsed,
                durationSeconds = 0
            )
        }

    val reelRows = Posts.selectAll()
        .where { (Posts.kind eq "reel") and Posts.deletedAt.isNull() }
        .orderBy(Posts.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(6)
        .toList()
    val mediaReels = PostService.mapRows(reelRows, viewer).mapNotNull { post ->
        if (post.mediaUrl.isBlank()) return@mapNotNull null
        MediaReelCardDto(
            id = post.id,
            author = post.author,
            thumbnailUrl = post.mediaUrl,
            viewCount = post.likeCount + post.commentCount + post.shareCount,
            caption = post.caption,
            hashtag = reelRows.firstOrNull { it[Posts.id].toString() == post.id }?.get(Posts.hashtag)
                ?.let { "#$it" } ?: "",
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            shareCount = post.shareCount
        )
    }

    val editorials = EditorialDispatches.selectAll()
        .orderBy(EditorialDispatches.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(5)
        .map { row ->
            EditorialDispatchCardDto(
                id = row[EditorialDispatches.id].toString(),
                title = row[EditorialDispatches.title],
                snippet = row[EditorialDispatches.snippet],
                domain = row[EditorialDispatches.domain],
                authorName = row[EditorialDispatches.authorName],
                readMinutes = row[EditorialDispatches.readMinutes],
                footnoteCount = row[EditorialDispatches.footnoteCount],
                participantCount = row[EditorialDispatches.participantCount]
            )
        }

    val soundtracks = Soundtracks.selectAll()
        .orderBy(Soundtracks.rank, org.jetbrains.exposed.sql.SortOrder.ASC)
        .limit(5)
        .map { row ->
            SoundtrackCardDto(
                id = row[Soundtracks.id].toString(),
                title = row[Soundtracks.title],
                artist = row[Soundtracks.artist],
                artworkUrl = row[Soundtracks.artworkUrl],
                rank = row[Soundtracks.rank],
                clipCount = row[Soundtracks.clipCount]
            )
        }

    val essayRows = Essays.selectAll()
        .orderBy(Essays.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(5)
        .toList()
    val essayAuthors = UserService.dtosByIds(essayRows.map { it[Essays.authorId] })
    val essays = essayRows.mapNotNull { row ->
        val author = essayAuthors[row[Essays.authorId]] ?: return@mapNotNull null
        EssayCardDto(
            id = row[Essays.id].toString(),
            author = author,
            postedAtLabel = relativeTime(row[Essays.createdAt]),
            title = row[Essays.title],
            preview = row[Essays.preview],
            likeCount = row[Essays.likeCount],
            commentCount = row[Essays.commentCount],
            shareCount = row[Essays.shareCount]
        )
    }

    return PodFeedDto(
        polls = polls,
        liveAudio = livePods,
        mediaReels = mediaReels,
        editorials = editorials,
        soundtracks = soundtracks,
        essays = essays
    )
}

private fun pollCard(pollId: UUID, viewer: UUID?): PodPollCardDto {
    val poll = PodPolls.selectAll().where { PodPolls.id eq pollId }.firstOrNull()
        ?: throw notFound("Poll not found")
    val author = UserService.dtoById(poll[PodPolls.authorId]) ?: throw notFound("Poll author not found")
    val options = PollOptions.selectAll()
        .where { PollOptions.pollId eq pollId }
        .orderBy(PollOptions.position, org.jetbrains.exposed.sql.SortOrder.ASC)
        .toList()
    val counts = PollVotes.select(PollVotes.optionId, PollVotes.optionId.count())
        .where { PollVotes.pollId eq pollId }
        .groupBy(PollVotes.optionId)
        .associate { it[PollVotes.optionId] to it[PollVotes.optionId.count()] }
    val total = counts.values.sum()
    val myVote = viewer?.let {
        PollVotes.selectAll()
            .where { (PollVotes.pollId eq pollId) and (PollVotes.userId eq it) }
            .firstOrNull()?.get(PollVotes.optionId)
    }
    val optionDtos = options.map { row ->
        val votes = counts[row[PollOptions.id]] ?: 0
        PodPollOptionDto(
            id = row[PollOptions.id].toString(),
            text = row[PollOptions.text],
            voteCount = votes,
            percentage = if (total == 0L) 0 else (votes * 100.0 / total).roundToInt()
        )
    }
    val endsIn = poll[PodPolls.endsAt]?.let { deadline ->
        val seconds = Duration.between(Instant.now(), deadline).seconds
        when {
            seconds <= 0 -> "Ended"
            seconds >= 3600 -> "Ends ${seconds / 3600}h ${(seconds % 3600) / 60}m"
            else -> "Ends ${seconds / 60}m"
        }
    } ?: "No deadline"
    return PodPollCardDto(
        id = pollId.toString(),
        podId = poll[PodPolls.podId].toString(),
        author = author,
        question = poll[PodPolls.question],
        description = poll[PodPolls.description],
        endsInLabel = endsIn,
        totalVotes = total,
        replyCount = poll[PodPolls.replyCount],
        options = optionDtos,
        myVoteOptionId = myVote?.toString()
    )
}

private fun relativeTime(instant: Instant): String {
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86_400}d ago"
    }
}
