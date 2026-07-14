package com.kenjdavidson.golfcanada.security.exception

/**
 * Thrown when the Golf Canada authentication response is missing required fields
 * (e.g. no access token or no user in the response).
 */
class IncompleteGolfCanadaAuthenticationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
