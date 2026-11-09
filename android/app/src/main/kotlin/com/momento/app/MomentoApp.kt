package com.momento.app

import android.app.Application
import com.momento.app.BuildConfig
import com.momento.core.networking.ApiConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MomentoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiConfig.baseUrl = BuildConfig.API_BASE_URL
        ApiConfig.isDebug = BuildConfig.DEBUG
    }
}
