package com.momento.app

import com.momento.core.datastore.SettingsDataStore
import com.momento.data.remote.LoginRequestDto
import com.momento.data.remote.MomentoApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Debug-only convenience: signs in to the seeded demo account so the four
 * primary screens exercise the real API on a fresh install. Release builds
 * never call this, and the real auth screens replace it when login UI lands.
 */
@Singleton
class DemoSessionBootstrap @Inject constructor(
    private val api: MomentoApi,
    private val settings: SettingsDataStore
) {

    suspend fun ensureSession() {
        if (!BuildConfig.DEBUG) return
        if (settings.accessToken.first() != null) return
        runCatching {
            val response = api.login(
                LoginRequestDto(
                    email = DEMO_EMAIL,
                    password = DEMO_PASSWORD
                )
            )
            settings.saveTokens(response.tokens.accessToken, response.tokens.refreshToken)
            settings.saveCurrentUserId(response.user.id)
        }
    }

    companion object {
        private const val DEMO_EMAIL = "leslie_alex007@momento.dev"
        private const val DEMO_PASSWORD = "Momento123!"
    }
}
