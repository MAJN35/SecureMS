package com.example.crypto

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade Cryptographic Engine for SecureSMS.
 *
 * Implements:
 * - Elliptic Curve Cryptography: NIST P-256 (secp256r1) for universal Android compatibility
 * - Key Agreement: ECDH (Elliptic Curve Diffie-Hellman)
 * - Key Derivation: HKDF-SHA256 (RFC 5869)
 * - Authenticated Encryption: AES-256-GCM (12-byte random IV, 128-bit authentication tag)
 * - Context-bound Additional Authenticated Data (AAD) preventing replay/malleability
 * - Safety Number generation (60-digit numeric fingerprint in 12 groups of 5)
 */
object CryptoEngine {

    private const val EC_CURVE = "secp256r1"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val AES_KEY_SIZE_BYTES = 32

    private val secureRandom = SecureRandom()

    /**
     * Generate an EC P-256 KeyPair locally.
     */
    fun generateIdentityKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec(EC_CURVE)
        kpg.initialize(ecSpec, secureRandom)
        return kpg.generateKeyPair()
    }

    /**
     * Derive a shared symmetric key using ECDH and HKDF-SHA256.
     */
    fun deriveSharedKey(
        myPrivateKey: PrivateKey,
        theirPublicKey: PublicKey,
        salt: ByteArray = "SecureSMS-v1-Salt".toByteArray(StandardCharsets.UTF_8),
        info: ByteArray = "SecureSMS-AES256-GCM-Session".toByteArray(StandardCharsets.UTF_8)
    ): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(theirPublicKey, true)
        val rawSharedSecret = keyAgreement.generateSecret()

        return hkdfSha256(
            inputKeyMaterial = rawSharedSecret,
            salt = salt,
            info = info,
            outputLengthBytes = AES_KEY_SIZE_BYTES
        )
    }

    /**
     * Encrypt a plaintext message using AES-256-GCM with context-bound AAD.
     *
     * @param sharedKey 256-bit symmetric key derived via HKDF
     * @param plaintext Original UTF-8 message
     * @param aad Additional authenticated data (protocol version, sender/recipient IDs, sequence number)
     * @return EncryptedPayload containing the 12-byte IV and ciphertext+tag
     */
    fun encrypt(
        sharedKey: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray
    ): EncryptedPayload {
        require(sharedKey.size == AES_KEY_SIZE_BYTES) { "Shared key must be 32 bytes (256 bits)" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sharedKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aad)

        val ciphertextWithTag = cipher.doFinal(plaintext)
        return EncryptedPayload(iv = iv, ciphertext = ciphertextWithTag)
    }

    /**
     * Decrypt an AES-256-GCM payload with authenticated tag and AAD validation.
     *
     * Throws an exception if authentication fails or message has been tampered with.
     */
    fun decrypt(
        sharedKey: ByteArray,
        iv: ByteArray,
        ciphertextWithTag: ByteArray,
        aad: ByteArray
    ): ByteArray {
        require(sharedKey.size == AES_KEY_SIZE_BYTES) { "Shared key must be 32 bytes" }
        require(iv.size == GCM_IV_LENGTH_BYTES) { "IV must be 12 bytes" }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sharedKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aad)

        return cipher.doFinal(ciphertextWithTag)
    }

    /**
     * Compute a 60-digit Safety Number for out-of-band contact verification.
     * Combines both public keys symmetrically (lexicographically ordered) so both
     * parties calculate the exact same safety number.
     */
    fun computeSafetyNumber(myPubKey: PublicKey, theirPubKey: PublicKey): String {
        val myEncoded = myPubKey.encoded
        val theirEncoded = theirPubKey.encoded

        // Lexicographical ordering for symmetric verification
        val (first, second) = if (compareByteArrays(myEncoded, theirEncoded) <= 0) {
            Pair(myEncoded, theirEncoded)
        } else {
            Pair(theirEncoded, myEncoded)
        }

        val digest = MessageDigest.getInstance("SHA-512")
        digest.update("SecureSMS-SafetyNumber-V1".toByteArray(StandardCharsets.UTF_8))
        digest.update(first)
        digest.update(second)
        var hash = digest.digest()

        // 5200 iterations of SHA-512 for key stretching
        for (i in 0 until 5199) {
            digest.reset()
            digest.update(hash)
            hash = digest.digest()
        }

        // Format 60 digits into 12 groups of 5 digits
        val sb = StringBuilder()
        for (i in 0 until 12) {
            val offset = i * 4
            val value = ((hash[offset].toInt() and 0xFF) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)
            val positiveValue = Math.abs(value) % 100000
            sb.append(String.format("%05d", positiveValue))
            if (i < 11) sb.append(" ")
        }
        return sb.toString()
    }

    /**
     * Compute SHA-256 hex fingerprint for quick visual verification.
     */
    fun computeFingerprint(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey.encoded)
        return hash.joinToString(":") { String.format("%02X", it) }
    }

    /**
     * Compute an 8-byte key ID for compact envelope references.
     */
    fun computeKeyId(publicKey: PublicKey): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val full = digest.digest(publicKey.encoded)
        return full.copyOfRange(0, 8)
    }

    fun parsePublicKey(encoded: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(encoded)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    fun parsePublicKeyFromBase64(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        return parsePublicKey(bytes)
    }

    fun encodePublicKeyToBase64(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * HKDF-SHA256 (Extract-and-Expand) per RFC 5869.
     */
    fun hkdfSha256(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLengthBytes: Int
    ): ByteArray {
        // Step 1: Extract
        val mac = Mac.getInstance("HmacSHA256")
        val effectiveSalt = if (salt.isEmpty()) ByteArray(32) else salt
        mac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
        val prk = mac.doFinal(inputKeyMaterial)

        // Step 2: Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val okm = ByteBuffer.allocate(outputLengthBytes)
        var iteration = 1.toByte()

        while (okm.hasRemaining()) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(iteration)
            t = mac.doFinal()

            val toCopy = Math.min(t.size, okm.remaining())
            okm.put(t, 0, toCopy)
            iteration++
        }

        return okm.array()
    }

    private fun compareByteArrays(a: ByteArray, b: ByteArray): Int {
        val minLen = Math.min(a.size, b.size)
        for (i in 0 until minLen) {
            val byteA = a[i].toInt() and 0xFF
            val byteB = b[i].toInt() and 0xFF
            if (byteA != byteB) return byteA - byteB
        }
        return a.size - b.size
    }

    data class EncryptedPayload(
        val iv: ByteArray,
        val ciphertext: ByteArray
    ) {
        fun toCombined(): ByteArray {
            val bb = ByteBuffer.allocate(iv.size + ciphertext.size)
            bb.put(iv)
            bb.put(ciphertext)
            return bb.array()
        }

        companion object {
            fun fromCombined(combined: ByteArray): EncryptedPayload {
                require(combined.size > GCM_IV_LENGTH_BYTES) { "Payload too short" }
                val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
                val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
                return EncryptedPayload(iv, ciphertext)
            }
        }
    }
}
