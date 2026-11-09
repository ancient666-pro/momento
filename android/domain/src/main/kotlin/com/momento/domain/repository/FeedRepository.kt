package com.momento.domain.repository

import com.momento.core.common.AppResult
import com.momento.domain.model.Post
import com.momento.domain.model.Story

interface FeedRepository {
    suspend fun getForYouFeed(page: Int): AppResult<List<Post>>
    suspend fun getFollowingFeed(page: Int): AppResult<List<Post>>
    suspend fun toggleLike(postId: String, liked: Boolean): AppResult<Unit>
    suspend fun toggleBookmark(postId: String, bookmarked: Boolean): AppResult<Unit>
}

interface StoryRepository {
    suspend fun getStories(): AppResult<List<Story>>
}
