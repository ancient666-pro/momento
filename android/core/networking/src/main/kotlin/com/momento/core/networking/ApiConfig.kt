package com.momento.core.networking

/**
 * Runtime API configuration, initialized by the app module at startup
 * (base URL comes from app BuildConfig, which is flavor/env aware).
 */
object ApiConfig {
    var baseUrl: String = "http://127.0.0.1:8080/"
    var isDebug: Boolean = false
}
