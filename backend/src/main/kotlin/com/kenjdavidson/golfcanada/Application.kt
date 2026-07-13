package com.kenjdavidson.golfcanada

import io.micronaut.runtime.Micronaut
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>) {
    configureGolfCanadaTrust()
    Micronaut.run(*args)
}

private const val GOLF_CANADA_CERTIFICATE_PATH = "ssl/golfcanada.pem"

private fun configureGolfCanadaTrust() {
    val certificate = loadGolfCanadaCertificate()
    val trustManagers = listOf(
        createTrustManagerForCertificate(certificate),
        createDefaultTrustManager(),
    )

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf<TrustManager>(CompositeX509TrustManager(trustManagers)), SecureRandom())
    SSLContext.setDefault(sslContext)
}

private fun loadGolfCanadaCertificate(): X509Certificate {
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificateStream = Thread.currentThread().contextClassLoader.getResourceAsStream(GOLF_CANADA_CERTIFICATE_PATH)
        ?: throw IllegalStateException("Missing certificate resource: $GOLF_CANADA_CERTIFICATE_PATH")

    return certificateStream.use {
        certificateFactory.generateCertificate(it) as X509Certificate
    }
}

private fun createDefaultTrustManager(): X509TrustManager {
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)

    return trustManagerFactory.trustManagers
        .filterIsInstance<X509TrustManager>()
        .firstOrNull()
        ?: throw IllegalStateException("Unable to load default X509TrustManager")
}

private fun createTrustManagerForCertificate(certificate: X509Certificate): X509TrustManager {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("golf-canada", certificate)
    }

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }

    return trustManagerFactory.trustManagers
        .filterIsInstance<X509TrustManager>()
        .firstOrNull()
        ?: throw IllegalStateException("Unable to load X509TrustManager for Golf Canada certificate")
}

private class CompositeX509TrustManager(
    private val delegates: List<X509TrustManager>,
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        var lastError: CertificateException? = null
        delegates.forEach { manager ->
            try {
                manager.checkClientTrusted(chain, authType)
                return
            } catch (error: CertificateException) {
                lastError = error
            }
        }
        throw lastError ?: CertificateException("Client certificate is not trusted")
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        var lastError: CertificateException? = null
        delegates.forEach { manager ->
            try {
                manager.checkServerTrusted(chain, authType)
                return
            } catch (error: CertificateException) {
                lastError = error
            }
        }
        throw lastError ?: CertificateException("Server certificate is not trusted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegates
        .flatMap { it.acceptedIssuers.asList() }
        .distinctBy { it.subjectX500Principal.name + it.serialNumber.toString() }
        .toTypedArray()
}
