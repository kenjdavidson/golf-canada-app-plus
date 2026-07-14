package com.kenjdavidson.golfcanada.security

import jakarta.inject.Singleton
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of [GolfCanadaTokenStorage].
 *
 * Access and refresh tokens are encrypted using [GolfCanadaTokenEncryption] (AES-256-GCM) before
 * being placed into the map, and decrypted on retrieval.  [ConcurrentHashMap] provides thread-safe
 * access without explicit synchronisation.
 */
@Singleton
class InMemoryTokenStorage(
    private val encryption: GolfCanadaTokenEncryption,
) : GolfCanadaTokenStorage {

    private data class EncryptedSession(
        val encryptedAccessToken: String,
        val encryptedRefreshToken: String?,
        val expiresAt: Instant,
        val rememberMe: Boolean,
    )

    private val sessions = ConcurrentHashMap<String, EncryptedSession>()

    override fun saveSession(user: GolfCanadaAuthenticatedUser) {
        sessions[user.username] = EncryptedSession(
            encryptedAccessToken = encryption.encrypt(user.accessToken),
            encryptedRefreshToken = user.refreshToken?.let { encryption.encrypt(it) },
            expiresAt = user.expiresAt,
            rememberMe = user.rememberMe,
        )
    }

    override fun getSession(username: String): GolfCanadaUserSession? {
        val entry = sessions[username] ?: return null
        return GolfCanadaUserSession(
            username = username,
            accessToken = encryption.decrypt(entry.encryptedAccessToken),
            refreshToken = entry.encryptedRefreshToken?.let { encryption.decrypt(it) },
            expiresAt = entry.expiresAt,
            rememberMe = entry.rememberMe,
        )
    }

    override fun updateSession(
        username: String,
        newAccessToken: String,
        newRefreshToken: String?,
        expiresInSeconds: Long,
    ) {
        sessions.computeIfPresent(username) { _, existing ->
            existing.copy(
                encryptedAccessToken = encryption.encrypt(newAccessToken),
                encryptedRefreshToken = newRefreshToken?.let { encryption.encrypt(it) },
                expiresAt = Instant.now().plusSeconds(expiresInSeconds),
            )
        }
    }

    override fun clearSession(username: String) {
        sessions.remove(username)
    }
}
