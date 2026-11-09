package com.momento.data.di

import android.content.Context
import com.momento.core.datastore.SettingsDataStore
import com.momento.core.networking.ApiClient
import com.momento.core.networking.ApiConfig
import com.momento.core.networking.TokenProvider
import com.momento.data.remote.MomentoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideTokenProvider(settings: SettingsDataStore): TokenProvider =
        object : TokenProvider {
            override suspend fun accessToken(): String? = settings.accessToken.first()
        }

    @Provides
    @Singleton
    fun provideRetrofit(tokenProvider: TokenProvider): Retrofit =
        ApiClient.create(ApiConfig.baseUrl, tokenProvider, ApiConfig.isDebug)

    @Provides
    @Singleton
    fun provideMomentoApi(retrofit: Retrofit): MomentoApi = retrofit.create(MomentoApi::class.java)
}
