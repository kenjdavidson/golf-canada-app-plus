package com.kenjdavidson.golfcanada.security.exception

/**
 * Thrown when the Golf Canada session has expired and cannot be refreshed
 * (e.g. the refresh token has been revoked or has expired on Golf Canada's side).
 * Handlers should clear the local session and the browser cookie, then redirect
 * the user to the login screen.
 */
class GolfCanadaExpiredException(
    message: String = "Golf Canada session expired. Please log in again.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
