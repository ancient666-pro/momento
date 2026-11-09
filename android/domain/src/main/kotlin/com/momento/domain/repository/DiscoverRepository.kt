package com.momento.domain.repository

import com.momento.core.common.AppResult
import com.momento.domain.model.AudioRoom
import com.momento.domain.model.NearbyProfile
import com.momento.domain.model.SparkCard

enum class SparkFilter { TRENDING, NEW, REELS, CREATIVE, AUDIO_SESSION }

interface DiscoverRepository {
    suspend fun getNearbyProfiles(): AppResult<List<NearbyProfile>>
    suspend fun getTrendingSparks(filter: SparkFilter): AppResult<List<SparkCard>>
    suspend fun getAudioRooms(): AppResult<List<AudioRoom>>
    suspend fun pass(profileId: String): AppResult<Unit>
    suspend fun like(profileId: String): AppResult<Unit>
    suspend fun superLike(profileId: String): AppResult<Unit>
}
