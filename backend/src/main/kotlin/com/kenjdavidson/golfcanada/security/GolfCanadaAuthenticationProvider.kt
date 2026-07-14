package com.kenjdavidson.golfcanada.security

import com.kenjdavidson.golfcanada.golfcanada.api.AuthenticationApi
import com.kenjdavidson.golfcanada.golfcanada.client.ApiException
import com.kenjdavidson.golfcanada.security.exception.IncompleteGolfCanadaAuthenticationException
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationFailureReason
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.provider.HttpRequestReactiveAuthenticationProvider
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

private const val GRANT_TYPE_PASSWORD = "password"
private const val DEFAULT_SCOPE = "address email offline_access openid phone profile roles"

/**
 * Micronaut [HttpRequestReactiveAuthenticationProvider] that delegates credential validation to the
 * Golf Canada authentication API.
 *
 * On success, the Golf Canada access and refresh tokens are stored in [GolfCanadaTokenStorage]
 * (encrypted at rest) for later use by [GolfCanadaTokenManager].  The resulting Micronaut JWT
 * cookie contains the username and a subset of profile claims; the full Golf Canada token is
 * never exposed to the browser.
 *
 * Pass `rememberMe=true` as a request query parameter (e.g. `POST /api/login?rememberMe=true`) to
 * opt into proactive Golf Canada token refresh.  When `rememberMe` is false the refresh token is
 * not used and the session is cleaned up once the Golf Canada access token expires.
 */
@Singleton
class GolfCanadaAuthenticationProvider(
    private val authenticationApi: AuthenticationApi,
    private val tokenStorage: GolfCanadaTokenStorage,
) : HttpRequestReactiveAuthenticationProvider<Any> {

    private val log = LoggerFactory.getLogger(GolfCanadaAuthenticationProvider::class.java)

    override fun authenticate(
        httpRequest: HttpRequest<Any>,
        authenticationRequest: AuthenticationRequest<String, String>,
    ): Publisher<AuthenticationResponse> = Mono.fromCallable {
        val username = authenticationRequest.identity
        val password = authenticationRequest.secret
        val rememberMe = httpRequest.parameters["rememberMe"]?.toBoolean() ?: false

        log.debug("Authenticating user '{}' against Golf Canada (rememberMe={})", username, rememberMe)

        try {
            val authToken = authenticationApi.authenticate(
                GRANT_TYPE_PASSWORD,
                username,
                password,
                false,
                DEFAULT_SCOPE,
                null,
                null,
                null,
            )

            val authenticatedUser = GolfCanadaAuthenticatedUser.from(authToken, rememberMe)

            tokenStorage.saveSession(authenticatedUser)

            AuthenticationResponse.success(
                authenticatedUser.username,
                buildClaims(authenticatedUser),
            )
        } catch (e: ApiException) {
            log.debug("Golf Canada authentication failed for '{}': HTTP {}", username, e.code)
            when (e.code) {
                400, 401 -> AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH)
                else -> AuthenticationResponse.failure(AuthenticationFailureReason.UNKNOWN)
            }
        } catch (e: IncompleteGolfCanadaAuthenticationException) {
            log.warn("Incomplete Golf Canada response for '{}': {}", username, e.message)
            AuthenticationResponse.failure(AuthenticationFailureReason.UNKNOWN)
        } catch (e: Exception) {
            log.error("Unexpected error authenticating '{}' with Golf Canada", username, e)
            AuthenticationResponse.failure(AuthenticationFailureReason.UNKNOWN)
        }
    }

    private fun buildClaims(user: GolfCanadaAuthenticatedUser): Map<String, Any> = buildMap {
        put("individualId", user.individualId)
        put("fullName", user.fullName)
        put("rememberMe", user.rememberMe)
    }
}
