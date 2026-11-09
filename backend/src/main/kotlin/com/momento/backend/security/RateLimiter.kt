package com.momento.backend.security

import com.momento.backend.config.AppConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * Fixed-window rate limiter. Uses Redis when reachable (shared across instances)
 * and falls back to a process-local window so a Redis outage cannot disable
 * abuse protection on authentication endpoints.
 */
object RateLimiter {

    private val log = LoggerFactory.getLogger("RateLimiter")
    private val local = ConcurrentHashMap<String, Window>()
    private val pool: JedisPool? = try {
        JedisPool(JedisPoolConfig().apply { maxTotal = 8 }, AppConfig.redisHost, AppConfig.redisPort)
    } catch (e: Exception) {
        log.warn("Redis pool unavailable for rate limiting: {}", e.message)
        null
    }

    data class Window(val startSecond: Long, val count: AtomicLong)

    /**
     * @return true when the request is allowed, false when the limit is exceeded.
     */
    fun allow(key: String, limit: Int, windowSeconds: Long = 60): Boolean {
        val redisKey = "rl:$key"
        if (pool != null) {
            try {
                pool.resource.use { jedis: Jedis ->
                    val count = jedis.incr(redisKey)
                    if (count == 1L) jedis.expire(redisKey, windowSeconds)
                    return count <= limit
                }
            } catch (e: Exception) {
                log.debug("Redis rate limit unavailable, using local window: {}", e.message)
            }
        }
        val now = System.currentTimeMillis() / 1000
        val window = local.compute(key) { _, existing ->
            if (existing == null || now - existing.startSecond >= windowSeconds) {
                Window(now, AtomicLong(0))
            } else {
                existing
            }
        }!!
        return window.count.incrementAndGet() <= limit
    }
}
