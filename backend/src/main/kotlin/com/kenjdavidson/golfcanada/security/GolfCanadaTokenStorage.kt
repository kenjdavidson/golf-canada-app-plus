package com.kenjdavidson.golfcanada.security

import java.time.Instant

/**
 * Represents a persisted user session.
 */
data class GolfCanadaUserSession(
    val username: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
    val rememberMe: Boolean,
)

/**
 * Storage contract for Golf Canada user sessions.  Implementations are responsible for
 * thread-safety and for encrypting access and refresh tokens before storing them.
 */
interface GolfCanadaTokenStorage {

    /**
     * Persists a new session, or replaces an existing one, for the given user.
     */
    fun saveSession(user: GolfCanadaAuthenticatedUser)

    /**
     * Returns the stored session for [username], or `null` if no session exists.
     */
    fun getSession(username: String): GolfCanadaUserSession?

    /**
     * Updates the access and refresh tokens for an existing session after a successful Golf Canada
     * token refresh.
     */
    fun updateSession(
        username: String,
        newAccessToken: String,
        newRefreshToken: String?,
        expiresInSeconds: Long,
    )

    /**
     * Removes the session for [username].  Called when a refresh token is invalidated so the user
     * must log in again.
     */
    fun clearSession(username: String)
}
