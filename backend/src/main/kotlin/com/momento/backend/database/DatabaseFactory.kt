package com.momento.backend.database

import com.momento.backend.common.ApiException
import com.momento.backend.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import io.ktor.http.HttpStatusCode
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val log = LoggerFactory.getLogger("Database")
    private var dataSource: HikariDataSource? = null
    private var initialized = false

    fun init(config: AppConfig) {
        if (initialized) return
        val url = config.dbUrl ?: run {
            log.warn("No database URL configured; running without a database")
            return
        }
        try {
            val hikari = HikariConfig().apply {
                jdbcUrl = url
                maximumPoolSize = config.dbPoolMaxSize
                connectionTimeout = 5_000
                initializationFailTimeout = -1
            }
            val ds = HikariDataSource(hikari)
            runFlyway(ds)
            Database.connect(ds)
            dataSource = ds
            initialized = true
            log.info("Database connected and migrated")
        } catch (e: Exception) {
            log.error("Database initialization failed: {}", e.message)
            initialized = false
        }
    }

    fun isConnected(): Boolean {
        val ds = dataSource ?: return false
        return try {
            ds.connection.use { it.isValid(2) }
        } catch (e: Exception) {
            false
        }
    }

    fun dataSource(): DataSource? = dataSource

    /** Runs a blocking Exposed transaction, failing fast with 503 when the DB is down. */
    fun <T> query(block: org.jetbrains.exposed.sql.Transaction.() -> T): T {
        if (!initialized) {
            // The service starts before Postgres may be accepting connections; retry
            // initialization on demand instead of staying permanently degraded.
            init(AppConfig)
        }
        if (!initialized) {
            throw ApiException(
                HttpStatusCode.ServiceUnavailable,
                "database_unavailable",
                "The database is unavailable; try again shortly"
            )
        }
        return transaction { block() }
    }

    private fun runFlyway(ds: DataSource) {
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
