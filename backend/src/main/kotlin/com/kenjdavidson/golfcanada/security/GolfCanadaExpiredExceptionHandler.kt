package com.kenjdavidson.golfcanada.security

import com.kenjdavidson.golfcanada.security.exception.GolfCanadaExpiredException
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.security.token.cookie.AccessTokenCookieConfiguration
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Handles [GolfCanadaExpiredException] by:
 * 1. Expiring the JWT access-token cookie so the browser discards it.
 * 2. Redirecting the client to `/login` so the user can re-authenticate.
 *
 * This covers the scenario described in constraint #3 of the issue: when the Golf Canada refresh
 * token expires or is revoked, the local SQLite session is cleared by [GolfCanadaTokenManager]
 * and this handler ensures the browser cookie is also cleared gracefully.
 */
@Singleton
@Produces
@Requires(classes = [GolfCanadaExpiredException::class, ExceptionHandler::class])
class GolfCanadaExpiredExceptionHandler(
    private val accessTokenCookieConfiguration: AccessTokenCookieConfiguration,
) : ExceptionHandler<GolfCanadaExpiredException, HttpResponse<*>> {

    private val log = LoggerFactory.getLogger(GolfCanadaExpiredExceptionHandler::class.java)

    override fun handle(
        request: HttpRequest<*>,
        exception: GolfCanadaExpiredException,
    ): MutableHttpResponse<*> {
        log.info("Golf Canada session expired — clearing cookie and redirecting to /login: {}", exception.message)

        val expiredCookie = Cookie.of(accessTokenCookieConfiguration.cookieName, "")
            .maxAge(0)
            .path("/")
            .httpOnly(true)

        return HttpResponse.redirect<Any>(URI.create("/login"))
            .cookie(expiredCookie)
    }
}
