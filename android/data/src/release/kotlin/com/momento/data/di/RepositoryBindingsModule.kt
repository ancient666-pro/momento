package com.momento.data.di

import com.momento.data.remote.RemoteDiscoverRepository
import com.momento.data.remote.RemoteFeedRepository
import com.momento.data.remote.RemotePodRepository
import com.momento.data.remote.RemoteProfileRepository
import com.momento.data.remote.RemoteStoryRepository
import com.momento.domain.repository.DiscoverRepository
import com.momento.domain.repository.FeedRepository
import com.momento.domain.repository.PodRepository
import com.momento.domain.repository.ProfileRepository
import com.momento.domain.repository.StoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Release-variant bindings: the real backend-backed repositories.
 * Compiled into release builds only.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    abstract fun bindFeedRepository(impl: RemoteFeedRepository): FeedRepository

    @Binds
    abstract fun bindStoryRepository(impl: RemoteStoryRepository): StoryRepository

    @Binds
    abstract fun bindProfileRepository(impl: RemoteProfileRepository): ProfileRepository

    @Binds
    abstract fun bindDiscoverRepository(impl: RemoteDiscoverRepository): DiscoverRepository

    @Binds
    abstract fun bindPodRepository(impl: RemotePodRepository): PodRepository
}
