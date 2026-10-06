package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utility for cryptographic password hashing and verification.
 * Uses SHA-256 with a cryptographically secure random 16-byte salt
 * and constant-time comparison to prevent side-channel timing attacks.
 */
object PasswordHasher {

    private val secureRandom = SecureRandom()

    /**
     * Generates a 16-byte cryptographically secure random salt encoded in Base64.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * Hashes the plaintext password with the provided salt using SHA-256.
     */
    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Combine password with salt
        val combined = "$password:$salt".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * Verifies whether a candidate plaintext password matches the stored salt and hash.
     * Uses MessageDigest.isEqual for constant-time comparison.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val candidateHash = hashPassword(password, salt)
        val candidateBytes = candidateHash.toByteArray(Charsets.UTF_8)
        val expectedBytes = expectedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidateBytes, expectedBytes)
    }
}
