package com.momento.backend.database

import com.momento.backend.config.AppConfig
import com.momento.backend.security.PasswordHasher
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/**
 * Development-only dataset. Every value below is stored as a normal database
 * row and served by the same queries as user-generated content; nothing bypasses
 * the API. Production disables this (`SEED_DEMO_DATA=false`).
 */
object SeedData {

    private const val DEMO_PASSWORD = "Momento123!"

    fun seedIfEmpty() {
        if (!AppConfig.seedDemoData) return
        DatabaseFactory.query {
            if (Users.selectAll().count() > 0) return@query
            val now = Instant.now()
            val hasher = PasswordHasher()
            val passwordHash = hasher.hash(DEMO_PASSWORD)

            fun user(
                username: String,
                displayName: String,
                avatarSeed: String,
                verified: Boolean,
                bio: String,
                category: String?,
                location: String?,
                age: Int?,
                interests: String
            ): UUID {
                val id = UUID.randomUUID()
                Users.insert {
                    it[Users.id] = id
                    it[Users.email] = "$username@momento.dev"
                    it[Users.username] = username
                    it[Users.displayName] = displayName
                    it[Users.avatarUrl] = avatar(avatarSeed)
                    it[Users.verified] = verified
                    it[Users.createdAt] = now.minus(120, ChronoUnit.DAYS)
                    it[Users.updatedAt] = now
                }
                UserCredentials.insert {
                    it[UserCredentials.userId] = id
                    it[UserCredentials.passwordHash] = passwordHash
                }
                UserProfiles.insert {
                    it[UserProfiles.userId] = id
                    it[UserProfiles.bio] = bio
                    it[UserProfiles.category] = category
                    it[UserProfiles.location] = location
                    it[UserProfiles.age] = age
                }
                NotificationPreferences.insert { it[NotificationPreferences.userId] = id }
                exec(
                    "UPDATE user_profiles SET interest_tags = ?::text[] WHERE user_id = '$id'",
                    listOf(org.jetbrains.exposed.sql.VarCharColumnType() to interests)
                )
                UserLocationData.insert {
                    it[UserLocationData.userId] = id
                    it[UserLocationData.latitude] = 28.6139 + (Math.random() - 0.5) * 0.04
                    it[UserLocationData.longitude] = 77.2090 + (Math.random() - 0.5) * 0.04
                    it[UserLocationData.privacyBandKm] = 3
                    it[UserLocationData.updatedAt] = now
                }
                return id
            }

            val leslie = user(
                "leslie_alex007", "Leslie Alexander", "leslie", true,
                "Visual storyteller. Fashion, film and quiet corners of big cities.",
                "Creator", "New Delhi", 27, "{\"Indie Pop\",\"Film Camera\",\"Bauhaus\"}"
            )
            val savannah = user(
                "savannah_nguyen", "Savannah Nguyen", "savannah", true,
                "Editorial stylist capturing light and texture.",
                "Fashion", "Mumbai", 29, "{\"Fashion\",\"Bauhaus\",\"Vintage\"}"
            )
            val robert = user(
                "robert_fox", "Robert Fox", "robert", false,
                "35mm and digital. Chasing honest frames.",
                "Filmmaker", "Bengaluru", 31, "{\"Film Camera\",\"Indie Cinema\",\"Travel\"}"
            )
            val elena = user(
                "elena_vance", "Elena Vance", "elena", true,
                "Spatial audio producer. Founder of Golden Hour.",
                "Music", "Delhi", 24, "{\"Indie Pop\",\"Spatial Audio\",\"Poetry\"}"
            )
            val guy = user(
                "guy_hawkins", "Guy Hawkins", "guy", false,
                "Streetwear, sneakers and city light.",
                "Streetwear", "Gurugram", 26, "{\"Streetwear\",\"Basketball\",\"Film Camera\"}"
            )
            val bessie = user(
                "bessie_cooper", "Bessie Cooper", "bessie", false,
                "Photographer of small, overlooked moments.",
                "Photography", "Noida", 30, "{\"Film Camera\",\"Indie Pop\",\"Coffee\"}"
            )
            val brooklyn = user(
                "brooklyn_sim007", "Brooklyn Simmons", "brooklyn", false,
                "Coffee, courtyards and long walks.",
                "Lifestyle", "Delhi", 28, "{\"Coffee\",\"Travel\",\"Bauhaus\"}"
            )
            val sarah = user(
                "sarah_chen", "Sarah Chen", "sarah", true,
                "Writing about culture, platforms and creative labour.",
                "Journalist", "Remote", 34, "{\"Indie Cinema\",\"Poetry\",\"AI Ethics\"}"
            )

            // Follow graph
            listOf(savannah, robert, elena, guy, bessie, brooklyn, sarah).forEach { follower ->
                Follows.insert {
                    it[Follows.followerId] = follower
                    it[Follows.followeeId] = leslie
                    it[Follows.createdAt] = now.minus(30, ChronoUnit.DAYS)
                }
            }
            listOf(savannah, robert, elena).forEach { target ->
                Follows.insert {
                    it[Follows.followerId] = leslie
                    it[Follows.followeeId] = target
                    it[Follows.createdAt] = now.minus(20, ChronoUnit.DAYS)
                }
            }

            fun media(owner: UUID, url: String, mime: String = "image/jpeg"): UUID {
                val id = UUID.randomUUID()
                MediaAssets.insert {
                    it[MediaAssets.id] = id
                    it[MediaAssets.ownerId] = owner
                    it[MediaAssets.bucket] = "seed"
                    it[MediaAssets.objectKey] = url
                    it[MediaAssets.url] = url
                    it[MediaAssets.mimeType] = mime
                    it[MediaAssets.sizeBytes] = 0
                    it[MediaAssets.status] = "ready"
                }
                return id
            }

            fun post(
                author: UUID,
                caption: String,
                mediaUrl: String,
                video: Boolean = false,
                kind: String = "post",
                hashtag: String? = null,
                aspect: Float = 0.75f,
                duration: Int? = null,
                minutesAgo: Long = 60,
                likesFrom: List<UUID> = emptyList()
            ): UUID {
                val id = UUID.randomUUID()
                Posts.insert {
                    it[Posts.id] = id
                    it[Posts.authorId] = author
                    it[Posts.caption] = caption
                    it[Posts.visibility] = "public"
                    it[Posts.kind] = kind
                    it[Posts.hashtag] = hashtag
                    it[Posts.createdAt] = now.minus(minutesAgo, ChronoUnit.MINUTES)
                    it[Posts.updatedAt] = now.minus(minutesAgo, ChronoUnit.MINUTES)
                }
                val assetId = media(author, mediaUrl, if (video) "video/mp4" else "image/jpeg")
                PostMedia.insert {
                    it[PostMedia.postId] = id
                    it[PostMedia.mediaAssetId] = assetId
                    it[PostMedia.mediaType] = if (video) "video" else "image"
                    it[PostMedia.position] = 0
                    it[PostMedia.aspectRatio] = aspect
                    it[PostMedia.durationSeconds] = duration
                }
                likesFrom.forEach { liker ->
                    PostLikes.insert {
                        it[PostLikes.postId] = id
                        it[PostLikes.userId] = liker
                        it[PostLikes.createdAt] = now.minus(minutesAgo - 5, ChronoUnit.MINUTES)
                    }
                }
                if (hashtag != null) {
                    Hashtags.insert {
                        it[Hashtags.id] = UUID.randomUUID()
                        it[Hashtags.name] = hashtag
                        it[Hashtags.postCount] = 1
                    }
                }
                return id
            }

            val everyone = listOf(leslie, savannah, robert, elena, guy, bessie, brooklyn, sarah)

            val savannahPost = post(
                savannah,
                "Layers, light and a little bit of city noise.",
                media("savannah-post"), minutesAgo = 45, likesFrom = everyone.take(6)
            )
            val brooklynPost = post(
                brooklyn,
                "Found a courtyard that feels like a different decade.",
                media("brooklyn-post"), minutesAgo = 180, likesFrom = everyone.take(4)
            )
            val robertPost = post(
                robert,
                "Inside the revival of 35mm film culture. Analog imperfections and honest grain.",
                media("robert-post"), video = true, duration = 42, minutesAgo = 300,
                likesFrom = everyone.take(5)
            )
            listOf(
                "leslie-post1" to "Soft tailoring, softer light.",
                "leslie-post2" to "Studio day. Yellow walls, long shadows.",
                "leslie-post3" to "Sunday market notes.",
                "leslie-post4" to "A study in blue and white."
            ).forEachIndexed { index, (seed, caption) ->
                post(
                    leslie, caption, media(seed), minutesAgo = 600L + index * 240,
                    likesFrom = everyone.take(3 + index)
                )
            }

            // Short-form reels ("Moments" and Pod media reels)
            val reelSeeds = listOf(
                Triple("reel-thumb", "Movement study, take 4.", 42_000),
                Triple("elena-post", "Golden hour on the terrace.", 34_600),
                Triple("guy-post", "Sneaker rotation, this week.", 88_200),
                Triple("bessie-post", "Film scan, unretouched.", 21_300)
            )
            val reelAuthors = listOf(robert, elena, guy, bessie)
            val reelHashtags = listOf("IndieCinema", "SpatialAudio", "Streetwear", "35mmFilm")
            reelSeeds.forEachIndexed { index, (seed, caption, _) ->
                post(
                    reelAuthors[index], caption, media(seed, 600, 900), video = true,
                    kind = "reel", hashtag = reelHashtags[index], aspect = 0.65f,
                    duration = 30 + index * 7, minutesAgo = 90L + index * 120,
                    likesFrom = everyone.take(5)
                )
            }
            // likes on savannah's hero post so For You has engagement signal
            listOf(leslie, robert, elena, guy, bessie, brooklyn, sarah).forEach { liker ->
                if (liker !in everyone.take(6)) {
                    PostLikes.insert {
                        it[PostLikes.postId] = savannahPost
                        it[PostLikes.userId] = liker
                        it[PostLikes.createdAt] = now.minus(30, ChronoUnit.MINUTES)
                    }
                }
            }

            // Comments
            fun comment(postId: UUID, author: UUID, body: String, minutesAgo: Long) {
                Comments.insert {
                    it[Comments.id] = UUID.randomUUID()
                    it[Comments.postId] = postId
                    it[Comments.authorId] = author
                    it[Comments.body] = body
                    it[Comments.createdAt] = now.minus(minutesAgo, ChronoUnit.MINUTES)
                }
            }
            comment(savannahPost, leslie, "That trench is perfect.", 30)
            comment(savannahPost, guy, "The colour grading here is unreal.", 25)
            comment(robertPost, sarah, "Grain over pixels, every time.", 200)
            comment(brooklynPost, elena, "Saving this for a shoot.", 120)

            // Stories
            fun story(
                author: UUID,
                seed: String,
                caption: String,
                liveViewers: Long,
                hoursAgo: Long
            ) {
                Stories.insert {
                    it[Stories.id] = UUID.randomUUID()
                    it[Stories.authorId] = author
                    it[Stories.mediaAssetId] = media(author, media(seed, 500, 800))
                    it[Stories.mediaType] = "image"
                    it[Stories.caption] = caption
                    it[Stories.liveViewerCount] = liveViewers
                    it[Stories.createdAt] = now.minus(hoursAgo, ChronoUnit.HOURS)
                    it[Stories.expiresAt] = now.plus(24 - hoursAgo, ChronoUnit.HOURS)
                }
            }
            story(guy, "story-guy", "LIVE: studio tour", 20_500, 2)
            story(robert, "story-robert", "PREMIERE: 35mm Diaries", 0, 3)
            story(bessie, "story-bessie", "LIVE: morning walk", 34_600, 1)
            story(elena, "story-elena", "Tonight, Golden Hour Beats", 0, 4)

            // Moments for Leslie's profile
            listOf("moment-1", "moment-2", "moment-3", "moment-4", "moment-5", "moment-6")
                .forEachIndexed { index, seed ->
                    Moments.insert {
                        it[Moments.id] = UUID.randomUUID()
                        it[Moments.authorId] = leslie
                        it[Moments.mediaAssetId] = media(leslie, media(seed, 600, 920), "video/mp4")
                        it[Moments.caption] = "Moment ${index + 1}"
                        it[Moments.thumbnailUrl] = media(seed, 600, 920)
                        it[Moments.viewCount] = (20_500L + index * 43_000L)
                        it[Moments.durationSeconds] = 15 + index * 4
                        it[Moments.createdAt] = now.minus((index + 1) * 12L, ChronoUnit.HOURS)
                    }
                }

            // Pods
            val pollPod = UUID.randomUUID()
            Pods.insert {
                it[Pods.id] = pollPod
                it[Pods.hostId] = leslie
                it[Pods.title] = "Community Pulse"
                it[Pods.description] = "A daily point of discussion for the community."
                it[Pods.topicHashtag] = "algorithmictruth"
                it[Pods.visibility] = "public"
                it[Pods.live] = false
                it[Pods.createdAt] = now.minus(2, ChronoUnit.DAYS)
            }
            PodMembers.insert {
                it[PodMembers.podId] = pollPod
                it[PodMembers.userId] = leslie
                it[PodMembers.role] = "host"
                it[PodMembers.joinedAt] = now.minus(2, ChronoUnit.DAYS)
            }

            val pollId = UUID.randomUUID()
            PodPolls.insert {
                it[PodPolls.id] = pollId
                it[PodPolls.podId] = pollPod
                it[PodPolls.authorId] = leslie
                it[PodPolls.question] = "Is algorithmic curation killing authentic community discoverability?"
                it[PodPolls.description] =
                    "Platforms optimize for instant dopamine loops, while grassroots creators lose organic discovery. Where does the drift head?"
                it[PodPolls.replyCount] = 142
                it[PodPolls.createdAt] = now.minus(42, ChronoUnit.MINUTES)
                it[PodPolls.endsAt] = now.plus(4, ChronoUnit.HOURS)
            }
            val optionYes = UUID.randomUUID()
            val optionNo = UUID.randomUUID()
            val optionMixed = UUID.randomUUID()
            listOf(
                optionYes to "Yes, heavily siloed",
                optionNo to "No, it enhances discovery",
                optionMixed to "Undecided / Mixed"
            ).forEachIndexed { index, (optionId, text) ->
                PollOptions.insert {
                    it[PollOptions.id] = optionId
                    it[PollOptions.pollId] = pollId
                    it[PollOptions.text] = text
                    it[PollOptions.position] = index
                }
            }
            listOf(everyone[1] to optionYes, everyone[2] to optionYes, everyone[3] to optionYes,
                everyone[4] to optionYes, everyone[5] to optionNo, everyone[6] to optionNo,
                everyone[7] to optionMixed).forEach { (voter, option) ->
                PollVotes.insert {
                    it[PollVotes.pollId] = pollId
                    it[PollVotes.optionId] = option
                    it[PollVotes.userId] = voter
                    it[PollVotes.createdAt] = now.minus(20, ChronoUnit.MINUTES)
                }
            }

            val livePod = UUID.randomUUID()
            Pods.insert {
                it[Pods.id] = livePod
                it[Pods.hostId] = elena
                it[Pods.title] = "Deep Dive: The Future of Spatial Audio & Creative Drift"
                it[Pods.description] = "Elena and Guy unpack spatial audio, field recording and creative drift."
                it[Pods.topicHashtag] = "spatialaudio"
                it[Pods.visibility] = "public"
                it[Pods.live] = true
                it[Pods.listenerCount] = 890
                it[Pods.artworkUrl] = media("golden-hour-room", 600, 340)
                it[Pods.createdAt] = now.minus(1, ChronoUnit.HOURS)
            }
            listOf(elena to "host", guy to "co_host").forEach { (member, role) ->
                PodMembers.insert {
                    it[PodMembers.podId] = livePod
                    it[PodMembers.userId] = member
                    it[PodMembers.role] = role
                    it[PodMembers.joinedAt] = now.minus(1, ChronoUnit.HOURS)
                }
            }
            PodSessions.insert {
                it[PodSessions.podId] = livePod
                it[PodSessions.startedAt] = now.minus(72, ChronoUnit.SECONDS)
            }

            listOf(
                Triple("algorithmictruth", 4_812L, true),
                Triple("spatialaudio", 3_200L, true),
                Triple("indiecinema", 1_900L, false),
                Triple("aiethics", 14_200L, false),
                Triple("solardrift", 2_600L, false)
            ).forEach { (name, participants, live) ->
                PodTopics.insert {
                    it[PodTopics.id] = UUID.randomUUID()
                    it[PodTopics.name] = name
                    it[PodTopics.participantCount] = participants
                    it[PodTopics.live] = live
                    it[PodTopics.createdAt] = now
                }
            }

            EditorialDispatches.insert {
                it[EditorialDispatches.id] = UUID.randomUUID()
                it[EditorialDispatches.title] =
                    "Open Source AI Models: What happens to creator copyright when training sets are communal?"
                it[EditorialDispatches.snippet] =
                    "An exploratory essay on how generative decentralized collectives are restructuring royalty pipelines for digital artists."
                it[EditorialDispatches.domain] = "dispatch.driftmag.io"
                it[EditorialDispatches.authorName] = "Sarah Chen"
                it[EditorialDispatches.readMinutes] = 5
                it[EditorialDispatches.footnoteCount] = 380
                it[EditorialDispatches.participantCount] = 2_400
                it[EditorialDispatches.createdAt] = now.minus(6, ChronoUnit.HOURS)
            }

            Soundtracks.insert {
                it[Soundtracks.id] = UUID.randomUUID()
                it[Soundtracks.title] = "Solar Drift"
                it[Soundtracks.artist] = "Vance & The Groove"
                it[Soundtracks.artworkUrl] = media("solar-drift", 200, 200)
                it[Soundtracks.rank] = 1
                it[Soundtracks.clipCount] = 42_100
                it[Soundtracks.createdAt] = now
            }

            Essays.insert {
                it[Essays.id] = UUID.randomUUID()
                it[Essays.authorId] = elena
                it[Essays.title] = "The Quiet Geometry of Light"
                it[Essays.preview] =
                    "Can silence in architecture recalibrate our creative focus in an attention-starved era?"
                it[Essays.likeCount] = 3_400
                it[Essays.commentCount] = 184
                it[Essays.shareCount] = 842
                it[Essays.createdAt] = now.minus(1, ChronoUnit.HOURS)
            }
        }
    }

    private fun avatar(seed: String) = "https://picsum.photos/seed/$seed/200/200"
    private fun media(seed: String, w: Int = 600, h: Int = 750) =
        "https://picsum.photos/seed/$seed/$w/$h"
}
