package com.momento.backend.config

/**
 * Environment-driven configuration. Secrets come exclusively from environment
 * variables (or a local .env during development) — never from source.
 */
object AppConfig {

    private val dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
        .ignoreIfMissing()
        .load()

    val environment: String = env("APP_ENV", "development")
    val serverPort: Int = env("SERVER_PORT", "8080").toInt()

    /** Development-only demo dataset; never enabled in production. */
    val seedDemoData: Boolean =
        env("SEED_DEMO_DATA", if (environment == "development") "true" else "false").toBoolean()

    // Security
    val jwtSecret: String = env("JWT_SECRET", "dev-only-insecure-secret-change-me")
    val jwtIssuer: String = env("JWT_ISSUER", "momento")
    val jwtAccessTtlMinutes: Long = env("JWT_ACCESS_TTL_MINUTES", "30").toLong()
    val jwtRefreshTtlDays: Long = env("JWT_REFRESH_TTL_DAYS", "30").toLong()

    val argon2Parallelism: Int = env("ARGON2_PARALLELISM", "1").toInt()
    val argon2MemoryKb: Int = env("ARGON2_MEMORY_KB", "65536").toInt()
    val argon2Iterations: Int = env("ARGON2_ITERATIONS", "3").toInt()

    // Database
    val dbUrl: String? = envOrNull("POSTGRES_URL")
        ?: buildJdbcUrl(
            host = env("POSTGRES_HOST", "localhost"),
            port = env("POSTGRES_PORT", "5432"),
            database = env("POSTGRES_DB", "momento"),
            user = env("POSTGRES_USER", "momento"),
            password = env("POSTGRES_PASSWORD", "momento_dev_password")
        )
    val dbPoolMaxSize: Int = env("DB_POOL_MAX_SIZE", "10").toInt()

    // Redis
    val redisHost: String = env("REDIS_HOST", "localhost")
    val redisPort: Int = env("REDIS_PORT", "6379").toInt()

    // Object storage
    val s3Endpoint: String = env("S3_ENDPOINT", "http://localhost:9000")
    val s3AccessKey: String = env("S3_ACCESS_KEY", "momento")
    val s3SecretKey: String = env("S3_SECRET_KEY", "momento_dev_password")
    val s3Bucket: String = env("S3_BUCKET", "momento-media")
    val s3Region: String = env("S3_REGION", "us-east-1")
    val s3PublicBaseUrl: String = env("S3_PUBLIC_BASE_URL", "http://localhost:9000")
    val s3UseSsl: Boolean = env("S3_USE_SSL", "false").toBoolean()

    // Nexconn integration (Chat + Call)
    val nexconnAppId: String? = envOrNull("NEXCONN_APP_ID")
    val nexconnAppSecret: String? = envOrNull("NEXCONN_APP_SECRET")
    val nexconnApiBaseUrl: String? = envOrNull("NEXCONN_API_BASE_URL")
    val nexconnWsBaseUrl: String? = envOrNull("NEXCONN_WS_BASE_URL")

    /** Data-center code the SDK connects to: BJ, SG, NA, SA or OM. */
    val nexconnAreaCode: String = env("NEXCONN_AREA_CODE", "SG").uppercase()
    val nexconnConfigured: Boolean
        get() = !nexconnAppId.isNullOrBlank() &&
            !nexconnAppSecret.isNullOrBlank() &&
            !nexconnApiBaseUrl.isNullOrBlank()

    private fun env(key: String, default: String): String = envOrNull(key) ?: default

    private fun envOrNull(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: dotenv.get(key)?.takeIf { it.isNotBlank() }

    private fun buildJdbcUrl(host: String, port: String, database: String, user: String, password: String) =
        "jdbc:postgresql://$host:$port/$database?user=$user&password=$password"
}
