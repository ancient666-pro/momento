package com.momento.backend

import com.momento.backend.config.AppConfig
import com.momento.backend.database.DatabaseFactory
import com.momento.backend.modules.auth.authModule
import com.momento.backend.modules.communications.communicationsModule
import com.momento.backend.modules.discover.discoverModule
import com.momento.backend.modules.feed.feedModule
import com.momento.backend.database.SeedData
import com.momento.backend.modules.media.mediaModule
import com.momento.backend.modules.moderation.moderationModule
import com.momento.backend.modules.moments.momentsModule
import com.momento.backend.modules.notifications.notificationsModule
import com.momento.backend.modules.pods.podsModule
import com.momento.backend.modules.posts.postsModule
import com.momento.backend.modules.stories.storiesModule
import com.momento.backend.modules.users.usersModule
import com.momento.backend.modules.webhooks.webhooksModule
import com.momento.backend.plugins.configureCORS
import com.momento.backend.plugins.configureCallLogging
import com.momento.backend.plugins.configureContentNegotiation
import com.momento.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

fun main() {
    val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::momentoModule)
        .start(wait = true)
}

fun Application.momentoModule() {
    val log = LoggerFactory.getLogger("Momento")

    configureContentNegotiation()
    configureStatusPages()
    configureCallLogging()
    configureCORS()

    // Lazy database initialization: the service boots (and health reports) even
    // when PostgreSQL is unavailable; Flyway migrations run on first connect.
    DatabaseFactory.init(AppConfig)
    runCatching { SeedData.seedIfEmpty() }
        .onFailure { log.warn("Demo data seed skipped: {}", it.message) }

    routing {
        get("/health") {
            val dbOk = runCatching { DatabaseFactory.isConnected() }.getOrDefault(false)
            val status = if (dbOk) "ok" else "degraded"
            call.respondText(
                "{\"status\":\"$status\",\"database\":\"${if (dbOk) "up" else "down"}\"}"
            )
        }

        route("/api/v1") {
            authModule()
            usersModule()
            postsModule()
            feedModule()
            storiesModule()
            momentsModule()
            discoverModule()
            podsModule()
            mediaModule()
            communicationsModule()
            notificationsModule()
            moderationModule()
            webhooksModule()
        }
    }

    log.info("Momento backend started on port ${AppConfig.serverPort} (env: ${AppConfig.environment})")
}
