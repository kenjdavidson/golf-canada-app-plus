package com.kenjdavidson.golfcanada

import io.micronaut.runtime.Micronaut
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>) {
    configureGolfCanadaTrustOrThrow()
    Micronaut.run(*args)
}

private const val GOLF_CANADA_CERTIFICATE_PATH = "ssl/golfcanada.pem"
private const val GOLF_CANADA_CERTIFICATE_ALIAS = "golf-canada"

private fun configureGolfCanadaTrustOrThrow() {
    try {
        val certificate = loadGolfCanadaCertificate()
        val trustStore = buildTrustStore(certificate, createDefaultTrustManager())
        val trustManager = createTrustManager(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
        SSLContext.setDefault(sslContext)
    } catch (error: Exception) {
        throw IllegalStateException(
            "Unable to configure SSL trust with $GOLF_CANADA_CERTIFICATE_PATH. " +
                "The application cannot start without the Golf Canada certificate.",
            error,
        )
    }
}

private fun loadGolfCanadaCertificate(): X509Certificate {
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificateStream = requireNotNull(
        Thread.currentThread().contextClassLoader.getResourceAsStream(GOLF_CANADA_CERTIFICATE_PATH),
    ) { "Certificate resource not found on classpath: " + GOLF_CANADA_CERTIFICATE_PATH }

    return certificateStream.use {
        certificateFactory.generateCertificate(it) as X509Certificate
    }
}

private fun buildTrustStore(certificate: X509Certificate, defaultTrustManager: X509TrustManager): KeyStore {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        defaultTrustManager.acceptedIssuers.forEachIndexed { index, issuer ->
            setCertificateEntry("default-root-$index", issuer)
        }
        setCertificateEntry(GOLF_CANADA_CERTIFICATE_ALIAS, certificate)
    }
    return keyStore
}

private fun createDefaultTrustManager(): X509TrustManager = createTrustManager(null)

private fun createTrustManager(keyStore: KeyStore?): X509TrustManager {
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }
    return trustManagerFactory.trustManagers
        .filterIsInstance<X509TrustManager>()
        .firstOrNull()
        ?: throw IllegalStateException("Unable to load X509TrustManager")
}
