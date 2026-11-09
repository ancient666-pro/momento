package com.momento.data.preview

import com.momento.domain.model.AudioRoom
import com.momento.domain.model.EditorialDispatchCard
import com.momento.domain.model.EssayCard
import com.momento.domain.model.LiveAudioPodCard
import com.momento.domain.model.MediaReelCard
import com.momento.domain.model.MediaType
import com.momento.domain.model.Moment
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodPollOption
import com.momento.domain.model.PodTopic
import com.momento.domain.model.Post
import com.momento.domain.model.SoundtrackCard
import com.momento.domain.model.SparkCard
import com.momento.domain.model.Story
import com.momento.domain.model.StoryStatus
import com.momento.domain.model.UserProfile

/**
 * Design-preview dataset mirroring the four reference screens.
 * Debug builds only — release builds use the real backend repositories.
 */
object DesignPreviewData {

    private fun avatar(seed: String) = "https://picsum.photos/seed/$seed/200/200"
    private fun media(seed: String, w: Int = 600, h: Int = 750) = "https://picsum.photos/seed/$seed/$w/$h"

    val leslie = UserProfile(
        id = "u-leslie", displayName = "Leslie Alexander", username = "leslie_alex007",
        avatarUrl = avatar("leslie"), verified = true,
        followerCount = 2_500_000, followingCount = 120,
        bio = "Visual storyteller & podcaster. Host of Algorithmic Truth.",
        category = "Creator", location = "Brooklyn, NY"
    )
    val elena = UserProfile(
        id = "u-elena", displayName = "Elena Vance", username = "elena_vance",
        avatarUrl = avatar("elena"), verified = true,
        followerCount = 892_000, followingCount = 431,
        bio = "Fashion stylist & photographer", category = "Fashion Stylist & Photographer", online = true
    )
    val guy = UserProfile(
        id = "u-guy", displayName = "Guy Hawkins", username = "guy_hawkins",
        avatarUrl = avatar("guy"), verified = false
    )
    val robert = UserProfile(
        id = "u-robert", displayName = "Robert Fox", username = "robert_fox",
        avatarUrl = avatar("robert"), verified = false
    )
    val bessie = UserProfile(
        id = "u-bessie", displayName = "Bessie Cooper", username = "bessie_cooper",
        avatarUrl = avatar("bessie"), verified = false
    )
    val savannah = UserProfile(
        id = "u-savannah", displayName = "Savannah Nguyen", username = "savannah.nguyen",
        avatarUrl = avatar("savannah"), verified = true
    )
    val brooklyn = UserProfile(
        id = "u-brooklyn", displayName = "Brooklyn Simmons", username = "brooklyn.simmons007",
        avatarUrl = avatar("brooklyn"), verified = false
    )
    val sebastian = UserProfile(
        id = "u-sebastian", displayName = "Sebastian Nguyen", username = "sebastian.nguyen",
        avatarUrl = avatar("sebastian"), verified = true
    )
    val robertEllis = UserProfile(
        id = "u-robert-ellis", displayName = "Robert Ellis", username = "robert.ellis",
        avatarUrl = avatar("robert-ellis"), verified = false
    )
    val devon = UserProfile(
        id = "u-devon", displayName = "Devon Lane", username = "devon_lane",
        avatarUrl = avatar("devon"), verified = true
    )
    val courtney = UserProfile(
        id = "u-courtney", displayName = "Courtney Henry", username = "courtney_henry",
        avatarUrl = avatar("courtney"), verified = false
    )

    val stories = listOf(
        Story("s-1", guy, media("guy-story"), StoryStatus.LIVE, liveViewerCount = 32_500),
        Story("s-2", robert, media("robert-story"), StoryStatus.PREMIERE),
        Story("s-3", bessie, media("bessie-story"), StoryStatus.LIVE, liveViewerCount = 34_600),
        Story("s-4", savannah, media("savannah-story"), isViewed = true),
        Story("s-5", brooklyn, media("brooklyn-story"), isViewed = true)
    )

    val homePosts = listOf(
        Post(
            id = "p-1", author = savannah, mediaUrl = media("savannah-post"), mediaType = MediaType.IMAGE,
            caption = "Morning light, slow streets. Shot on film, no edits.",
            likeCount = 120_000, commentCount = 94_000, shareCount = 34_000,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f
        ),
        Post(
            id = "p-2", author = brooklyn, mediaUrl = media("brooklyn-post"), mediaType = MediaType.IMAGE,
            caption = "Weekend drift through the old quarter.",
            likeCount = 45_800, commentCount = 12_300, shareCount = 8_900,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f
        ),
        Post(
            id = "p-3", author = robert, mediaUrl = media("robert-post"), mediaType = MediaType.VIDEO,
            caption = "35mm revival — behind the scenes cut.",
            likeCount = 18_400, commentCount = 630, shareCount = 1_200,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f, videoDurationSeconds = 42
        )
    )

    val followingPosts = listOf(
        Post(
            id = "pf-1", author = elena, mediaUrl = media("elena-post"), mediaType = MediaType.IMAGE,
            caption = "Studio notes on the geometry of quiet.",
            likeCount = 3_400, commentCount = 184, shareCount = 842,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f
        ),
        Post(
            id = "pf-2", author = guy, mediaUrl = media("guy-post"), mediaType = MediaType.VIDEO,
            caption = "Live room highlights from last night's spatial audio deep dive.",
            likeCount = 9_100, commentCount = 402, shareCount = 1_050,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f, videoDurationSeconds = 58
        )
    )

    val moments = listOf(
        Moment("m-1", leslie, media("leslie-m1", 400, 610), viewCount = 10_500, durationSeconds = 18),
        Moment("m-2", leslie, media("leslie-m2", 400, 610), viewCount = 40_900, durationSeconds = 24),
        Moment("m-3", leslie, media("leslie-m3", 400, 610), viewCount = 334_100, durationSeconds = 31),
        Moment("m-4", leslie, media("leslie-m4", 400, 610), viewCount = 8_200, durationSeconds = 15),
        Moment("m-5", leslie, media("leslie-m5", 400, 610), viewCount = 22_600, durationSeconds = 27),
        Moment("m-6", leslie, media("leslie-m6", 400, 610), viewCount = 61_300, durationSeconds = 21)
    )

    val profilePosts = listOf(
        Post(
            id = "lp-1", author = leslie, mediaUrl = media("leslie-post1"), mediaType = MediaType.IMAGE,
            caption = "Notes from the Algorithmic Truth recording session.",
            likeCount = 84_200, commentCount = 3_100, shareCount = 5_600,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f
        ),
        Post(
            id = "lp-2", author = leslie, mediaUrl = media("leslie-post2"), mediaType = MediaType.IMAGE,
            caption = "Field research for the next dispatch.",
            likeCount = 51_400, commentCount = 2_400, shareCount = 3_900,
            createdAtMillis = 0L, mediaAspectRatio = 0.82f
        )
    )

    val nearbyProfiles = listOf(
        NearbyProfile(
            profile = elena, age = 24, distanceKm = 2.4, matchPercent = 98,
            interests = listOf("Indie Pop", "Film Cameras", "Oat Flat White")
        ),
        NearbyProfile(
            profile = devon, age = 27, distanceKm = 3.1, matchPercent = 91,
            interests = listOf("Jazz", "Architecture", "Espresso")
        ),
        NearbyProfile(
            profile = courtney, age = 22, distanceKm = 4.6, matchPercent = 87,
            interests = listOf("Lo-fi", "Polaroid", "Matcha")
        )
    )

    fun sparksFor(filterName: String): List<SparkCard> = when (filterName) {
        "TRENDING" -> listOf(
            SparkCard("sp-1", "Golden hour on the waterfront", sebastian, media("sebastian-spark"), viewCount = 200_000, likeCount = 45_800, commentCount = 45_800),
            SparkCard("sp-2", "Studio morning ritual", robertEllis, media("robertellis-spark"), viewCount = 88_300, likeCount = 34_600, commentCount = 34_600, durationSeconds = 42)
        )
        "NEW" -> listOf(
            SparkCard("sp-3", "First light, first roll", courtney, media("courtney-spark"), viewCount = 12_400, likeCount = 3_100, commentCount = 980),
            SparkCard("sp-4", "Slow fashion walkthrough", devon, media("devon-spark"), viewCount = 21_900, likeCount = 5_600, commentCount = 1_400, durationSeconds = 33)
        )
        "REELS" -> listOf(
            SparkCard("sp-5", "35mm in the rain", robert, media("robert-reel"), viewCount = 45_900, likeCount = 18_400, commentCount = 630, durationSeconds = 42),
            SparkCard("sp-6", "Backstage at the show", savannah, media("savannah-reel"), viewCount = 67_200, likeCount = 22_100, commentCount = 1_800, durationSeconds = 28)
        )
        "CREATIVE" -> listOf(
            SparkCard("sp-7", "Silent architecture study", elena, media("elena-creative"), viewCount = 33_600, likeCount = 9_200, commentCount = 640),
            SparkCard("sp-8", "Monochrome city walks", guy, media("guy-creative"), viewCount = 15_800, likeCount = 4_300, commentCount = 380)
        )
        else -> listOf(
            SparkCard("sp-9", "Golden Hour Beats & Poetry — live cut", bessie, media("bessie-audio"), viewCount = 19_500, likeCount = 6_800, commentCount = 520, durationSeconds = 64),
            SparkCard("sp-10", "Soundboard session 02", guy, media("guy-audio"), viewCount = 11_200, likeCount = 3_900, commentCount = 290, durationSeconds = 51)
        )
    }

    val audioRooms = listOf(
        AudioRoom(
            id = "ar-1", title = "Golden Hour Beats & Poetry", listenerCount = 142,
            hosts = listOf(guy, bessie), artworkUrl = media("golden-hour-room", 600, 340)
        )
    )

    val podTopics = listOf(
        PodTopic("t-feed", "Feed", participantCount = 0),
        PodTopic("t-algo", "#AlgorithmicTruth", participantCount = 8_400),
        PodTopic("t-spatial", "#SpatialAudio", participantCount = 3_100, live = true)
    )

    val podFeed: List<com.momento.domain.model.PodFeedItem> = listOf(
        PodPollCard(
            id = "poll-1", podId = "pod-algo-truth", author = leslie,
            question = "Is algorithmic curation killing authentic community discoverability?",
            description = "Platforms optimize for instant dopamine loops, while grassroots creators lose organic discovery. Where does the drift head?",
            endsInLabel = "Ends 3h 42m", totalVotes = 4_812, replyCount = 142,
            options = listOf(
                PodPollOption("o-1", "Yes, heavily siloed", voteCount = 2_983, percentage = 62),
                PodPollOption("o-2", "No, it enhances discovery", voteCount = 1_347, percentage = 28),
                PodPollOption("o-3", "Undecided / Mixed", voteCount = 482, percentage = 10)
            ),
            myVoteOptionId = "o-1"
        ),
        LiveAudioPodCard(
            id = "live-1", title = "Deep Dive: The Future of Spatial Audio & Creative Drift.",
            hosts = listOf(elena, guy), listenerCount = 890,
            positionSeconds = 72, durationSeconds = 165
        ),
        MediaReelCard(
            id = "reel-1", author = robert, thumbnailUrl = media("reel-thumb", 600, 800), viewCount = 45_900,
            caption = "Inside the revival of 35mm film culture in 2025. Why analog imperfections spark deeper emotional engagement than hyper-crisp 8K sensors.",
            hashtag = "#IndieCinema", likeCount = 18_400, commentCount = 630, shareCount = 1_200
        ),
        EditorialDispatchCard(
            id = "edit-1", title = "Open Source AI Models: What happens to creator copyright when training sets are communal?",
            snippet = "An exploratory essay on how generative decentralized collectives are restructuring royalty pipelines for independent creators.",
            domain = "dispatch.driftmag.io", authorName = "Sarah Chen", readMinutes = 5,
            footnoteCount = 380, participantCount = 2_400
        ),
        SoundtrackCard(
            id = "track-1", title = "Solar Drift", artist = "Vance & The Groove",
            artworkUrl = media("solar-drift", 200, 200), rank = 1, clipCount = 42_100
        ),
        EssayCard(
            id = "essay-1", author = elena, postedAtLabel = "1h ago",
            title = "The Quiet Geometry of Light.",
            preview = "Can silence in architecture recalibrate our creative focus in an attention-starved era?",
            likeCount = 3_400, commentCount = 184, shareCount = 842
        )
    )
}
