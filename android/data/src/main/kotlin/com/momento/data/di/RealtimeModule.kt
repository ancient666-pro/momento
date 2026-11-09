package com.momento.data.di

import com.momento.data.realtime.NexconnRealtime
import com.momento.domain.realtime.RealtimeSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    @Singleton
    abstract fun bindRealtimeSession(impl: NexconnRealtime): RealtimeSession
}
