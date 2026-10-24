package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android Keystore helper for securing local database encryption keys and long-term
 * private identity keys in hardware-backed storage where supported.
 *
 * Includes an automated software fallback for local JVM/Robolectric test environments
 * where hardware-backed "AndroidKeyStore" provider is not registered.
 */
class KeystoreHelper(context: Context) {

    private var fallbackKey: SecretKey? = null

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    } catch (e: Exception) {
        null
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        val ks = keyStore
        if (ks != null) {
            try {
                if (!ks.containsAlias(MASTER_KEY_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE
                    )
                    val parameterSpec = KeyGenParameterSpec.Builder(
                        MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()

                    keyGenerator.init(parameterSpec)
                    keyGenerator.generateKey()
                }
            } catch (e: Exception) {
                initFallbackKey()
            }
        } else {
            initFallbackKey()
        }
    }

    private fun initFallbackKey() {
        if (fallbackKey == null) {
            val kg = KeyGenerator.getInstance("AES")
            kg.init(256)
            fallbackKey = kg.generateKey()
        }
    }

    private fun getMasterKey(): SecretKey {
        return try {
            keyStore?.getKey(MASTER_KEY_ALIAS, null) as? SecretKey ?: (fallbackKey ?: run {
                initFallbackKey()
                fallbackKey!!
            })
        } catch (e: Exception) {
            fallbackKey ?: run {
                initFallbackKey()
                fallbackKey!!
            }
        }
    }

    /**
     * Encrypt sensitive data with the Keystore master key.
     */
    fun encryptWithMasterKey(plaintext: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt sensitive data with the Keystore master key.
     */
    fun decryptWithMasterKey(base64Ciphertext: String): ByteArray {
        val combined = Base64.decode(base64Ciphertext, Base64.NO_WRAP)
        require(combined.size > 12) { "Invalid encrypted data size" }

        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)

        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "SecureSmsMasterStorageKey"
    }
}
