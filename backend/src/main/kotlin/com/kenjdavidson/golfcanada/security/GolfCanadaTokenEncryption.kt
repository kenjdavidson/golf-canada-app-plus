package com.kenjdavidson.golfcanada.security

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val AES_ALGORITHM = "AES"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH_BITS = 128

/**
 * Encrypts and decrypts token strings using AES-256-GCM.
 *
 * A 256-bit key is derived from the configured [tokenEncryptionKey] string via SHA-256 so that
 * operators do not need to supply an exact 32-byte key.  Each encryption call uses a fresh random
 * 12-byte IV, which is prepended to the ciphertext before Base64-encoding.
 */
@Singleton
class GolfCanadaTokenEncryption(
    @Value("\${golf-canada-app.security.token-encryption-key}") private val tokenEncryptionKey: String,
) {
    private val secureRandom = SecureRandom()

    private val secretKey: SecretKeySpec by lazy {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(
            tokenEncryptionKey.toByteArray(Charsets.UTF_8),
        )
        SecretKeySpec(keyBytes, AES_ALGORITHM)
    }

    /**
     * Encrypts [plaintext] and returns a Base64-encoded string of `IV || ciphertext`.
     */
    fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a Base64-encoded `IV || ciphertext` string produced by [encrypt].
     */
    fun decrypt(encoded: String): String {
        val combined = Base64.getDecoder().decode(encoded)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
