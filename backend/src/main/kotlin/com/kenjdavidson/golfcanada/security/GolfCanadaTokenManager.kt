package com.kenjdavidson.golfcanada.security

import com.kenjdavidson.golfcanada.golfcanada.api.AuthenticationApi
import com.kenjdavidson.golfcanada.security.exception.GolfCanadaExpiredException
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Instant

private val TOKEN_REFRESH_BUFFER = java.time.Duration.ofSeconds(300)

/**
 * Manages Golf Canada access tokens for authenticated users.
 *
 * Tokens are stored encrypted in SQLite via [GolfCanadaTokenStorage].  When an access token is
 * close to expiry (within a 5-minute buffer) and the user has opted into "remember me", this
 * manager transparently refreshes it using the stored refresh token.
 *
 * A [Mutex] ensures that only one coroutine executes the refresh flow at a time — preventing the
 * classic OAuth race condition where two concurrent requests both try to use an already-invalidated
 * refresh token.  Waiting coroutines re-read the newly-saved token once the lock is released.
 */
@Singleton
class GolfCanadaTokenManager(
    private val tokenStorage: GolfCanadaTokenStorage,
    private val authenticationApi: AuthenticationApi,
) {
    private val log = LoggerFactory.getLogger(GolfCanadaTokenManager::class.java)
    private val refreshMutex = Mutex()

    /**
     * Returns a valid Golf Canada access token for [username].
     *
     * If the stored token is still valid (beyond the 5-minute buffer) it is returned immediately.
     * If it has expired and the user opted into "remember me", the token is refreshed.
     * If no session exists, or the refresh token itself has expired, a [GolfCanadaExpiredException]
     * is thrown — callers should clear the session cookie and redirect the user to login.
     *
     * This method is safe to call concurrently from multiple threads.
     */
    fun getValidAccessToken(username: String): String = runBlocking {
        refreshMutex.withLock {
            val session = tokenStorage.getSession(username)
                ?: throw GolfCanadaExpiredException("No session found for user: $username")

            val tokenIsValid = session.expiresAt.isAfter(Instant.now().plus(TOKEN_REFRESH_BUFFER))

            if (tokenIsValid) {
                return@withLock session.accessToken
            }

            if (!session.rememberMe) {
                log.info("Access token expired for user {} and rememberMe=false — clearing session", username)
                tokenStorage.clearSession(username)
                throw GolfCanadaExpiredException(
                    "Golf Canada access token expired for user $username and rememberMe is disabled.",
                )
            }

            val refreshToken = session.refreshToken
                ?: run {
                    log.warn("No refresh token available for user {} — clearing session", username)
                    tokenStorage.clearSession(username)
                    throw GolfCanadaExpiredException(
                        "No refresh token stored for user $username.",
                    )
                }

            log.info("Refreshing Golf Canada access token for user {}", username)
            return@withLock refreshTokens(username, refreshToken)
        }
    }

    private fun refreshTokens(username: String, refreshToken: String): String {
        return try {
            val newTokens = authenticationApi.authenticate(
                "refresh_token",
                null,
                null,
                null,
                null,
                null,
                null,
                refreshToken,
            )

            val newAccessToken = newTokens.accessToken
                ?: run {
                    tokenStorage.clearSession(username)
                    throw GolfCanadaExpiredException(
                        "Token refresh response for user $username is missing access_token.",
                    )
                }

            val expiresIn = newTokens.expiresIn?.toLong() ?: 3600L

            tokenStorage.updateSession(
                username = username,
                newAccessToken = newAccessToken,
                newRefreshToken = newTokens.refreshToken,
                expiresInSeconds = expiresIn,
            )

            log.info("Successfully refreshed Golf Canada access token for user {}", username)
            newAccessToken
        } catch (e: GolfCanadaExpiredException) {
            throw e
        } catch (e: Exception) {
            log.warn("Failed to refresh Golf Canada access token for user {}: {}", username, e.message)
            tokenStorage.clearSession(username)
            throw GolfCanadaExpiredException(
                "Golf Canada refresh token for user $username has been revoked or has expired.",
                e,
            )
        }
    }
}
