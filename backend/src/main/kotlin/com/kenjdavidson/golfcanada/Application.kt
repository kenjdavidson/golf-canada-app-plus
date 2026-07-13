package com.kenjdavidson.golfcanada

import com.kenjdavidson.golfcanada.security.SSLUtilities.configureGolfCanadaTrustOrThrow
import io.micronaut.runtime.Micronaut

fun main(args: Array<String>) {
    configureGolfCanadaTrustOrThrow()
    Micronaut.run(*args)
}
