package com.sirktv.app.storage

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import androidx.annotation.RequiresApi
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

internal data class LegacyEncryptedPayload(
    val iv: String,
    val wrappedKey: String,
    val ciphertext: String
)

/**
 * Fallback encryption for API 21-22 devices (older Fire OS / Android TV boxes),
 * where androidx.security's EncryptedSharedPreferences is unavailable (it
 * requires API 23+ for AndroidKeyStore-generated symmetric keys). A random
 * AES-256-GCM key encrypts the credential payload; that AES key is itself
 * wrapped by an RSA keypair held in AndroidKeyStore (supported since API 18),
 * so the raw AES key never touches disk unencrypted.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal object LegacyCredentialCipher {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "sirktv_legacy_wrap_key"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    fun encrypt(context: Context, plainText: String): LegacyEncryptedPayload {
        val aesKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val wrappedKey = wrapAesKey(context, aesKey)
        return LegacyEncryptedPayload(
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            wrappedKey = Base64.encodeToString(wrappedKey, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    fun decrypt(context: Context, payload: LegacyEncryptedPayload): String {
        val aesKey = unwrapAesKey(context, Base64.decode(payload.wrappedKey, Base64.NO_WRAP))
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(payload.iv, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), spec)
        val plainBytes = cipher.doFinal(Base64.decode(payload.ciphertext, Base64.NO_WRAP))
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun wrapAesKey(context: Context, aesKey: ByteArray): ByteArray {
        val keyPair = getOrCreateRsaKeyPair(context)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
        return cipher.doFinal(aesKey)
    }

    private fun unwrapAesKey(context: Context, wrappedKey: ByteArray): ByteArray {
        val keyPair = getOrCreateRsaKeyPair(context)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        return cipher.doFinal(wrappedKey)
    }

    @Suppress("DEPRECATION")
    private fun getOrCreateRsaKeyPair(context: Context): KeyPair {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) {
            return KeyPair(existing.certificate.publicKey, existing.privateKey)
        }

        val generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER)
        val start = Calendar.getInstance()
        val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 25) }
        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(KEY_ALIAS)
            .setSubject(X500Principal("CN=$KEY_ALIAS"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
        generator.initialize(spec)
        return generator.generateKeyPair()
    }
}
