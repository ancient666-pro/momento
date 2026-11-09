package com.momento.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val currentUserId = stringPreferencesKey("current_user_id")
        val hasCompletedOnboarding = stringPreferencesKey("has_completed_onboarding")
    }

    val accessToken: Flow<String?> = context.settingsDataStore.data.map { it[Keys.accessToken] }
    val refreshToken: Flow<String?> = context.settingsDataStore.data.map { it[Keys.refreshToken] }
    val currentUserId: Flow<String?> = context.settingsDataStore.data.map { it[Keys.currentUserId] }

    suspend fun saveTokens(access: String, refresh: String) {
        context.settingsDataStore.edit {
            it[Keys.accessToken] = access
            it[Keys.refreshToken] = refresh
        }
    }

    suspend fun saveCurrentUserId(userId: String) {
        context.settingsDataStore.edit { it[Keys.currentUserId] = userId }
    }

    suspend fun clearSession() {
        context.settingsDataStore.edit {
            it.remove(Keys.accessToken)
            it.remove(Keys.refreshToken)
            it.remove(Keys.currentUserId)
        }
    }

    suspend fun markOnboardingComplete() {
        context.settingsDataStore.edit { it[Keys.hasCompletedOnboarding] = "true" }
    }
}
