package com.momento.domain.repository

import com.momento.core.common.AppResult
import com.momento.domain.model.Moment
import com.momento.domain.model.Post
import com.momento.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(userId: String): AppResult<UserProfile>
    suspend fun getMoments(userId: String): AppResult<List<Moment>>
    suspend fun getPosts(userId: String): AppResult<List<Post>>
    suspend fun follow(userId: String): AppResult<Unit>
    suspend fun unfollow(userId: String): AppResult<Unit>
}
