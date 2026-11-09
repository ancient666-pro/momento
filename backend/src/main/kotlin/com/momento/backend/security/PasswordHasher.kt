package com.momento.backend.security

import com.momento.backend.config.AppConfig
import de.mkammerer.argon2.Argon2Factory

class PasswordHasher {

    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun hash(password: String): String = argon2.hash(
        AppConfig.argon2Iterations,
        AppConfig.argon2MemoryKb,
        AppConfig.argon2Parallelism,
        password.toCharArray()
    )

    fun verify(hash: String, password: String): Boolean = try {
        argon2.verify(hash, password.toCharArray())
    } catch (e: Exception) {
        false
    }
}
