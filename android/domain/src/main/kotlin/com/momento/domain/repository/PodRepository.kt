package com.momento.domain.repository

import com.momento.core.common.AppResult
import com.momento.domain.model.PodFeedItem
import com.momento.domain.model.PodPollCard
import com.momento.domain.model.PodTopic

interface PodRepository {
    suspend fun getPodFeed(): AppResult<List<PodFeedItem>>
    suspend fun getTopics(): AppResult<List<PodTopic>>
    suspend fun getLiveListenerCount(): AppResult<Long>
    suspend fun vote(podId: String, pollId: String, optionId: String): AppResult<PodPollCard>
    suspend fun joinPod(podId: String): AppResult<Unit>
}
