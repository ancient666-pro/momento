package com.momento.data.preview

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
 * Debug builds use the same remote repositories as release builds. The
 * design-preview dataset in this package is retained only for screenshot
 * regression work and is no longer bound into the running app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugRepositoryBindingsModule {

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
