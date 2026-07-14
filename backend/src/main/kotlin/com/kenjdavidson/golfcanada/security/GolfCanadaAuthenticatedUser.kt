package com.kenjdavidson.golfcanada.security

import com.kenjdavidson.golfcanada.golfcanada.model.AuthToken
import com.kenjdavidson.golfcanada.security.exception.IncompleteGolfCanadaAuthenticationException
import java.time.Instant

/**
 * Represents an authenticated Golf Canada user, holding the Golf Canada access and refresh
 * tokens alongside basic profile information.  Instances are created from a successful
 * [AuthToken] response and stored in [GolfCanadaTokenStorage].
 */
data class GolfCanadaAuthenticatedUser(
    val username: String,
    val individualId: Long?,
    val fullName: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant,
    val rememberMe: Boolean,
) {
    companion object {
        /**
         * Constructs a [GolfCanadaAuthenticatedUser] from a raw [AuthToken] returned by the Golf
         * Canada authentication API.
         *
         * @throws IncompleteGolfCanadaAuthenticationException if any required field is absent.
         */
        fun from(authToken: AuthToken, rememberMe: Boolean): GolfCanadaAuthenticatedUser {
            val user = authToken.user
                ?: throw IncompleteGolfCanadaAuthenticationException("AuthToken is missing the user field")

            val username = user.username
                ?: throw IncompleteGolfCanadaAuthenticationException("AuthToken user is missing the username field")

            val accessToken = authToken.accessToken
                ?: throw IncompleteGolfCanadaAuthenticationException("AuthToken is missing the access_token field")

            val expiresIn = authToken.expiresIn
                ?: throw IncompleteGolfCanadaAuthenticationException("AuthToken is missing the expires_in field")

            return GolfCanadaAuthenticatedUser(
                username = username,
                individualId = user.id,
                fullName = user.fullName,
                accessToken = accessToken,
                refreshToken = authToken.refreshToken,
                expiresAt = Instant.now().plusSeconds(expiresIn.toLong()),
                rememberMe = rememberMe,
            )
        }
    }
}
