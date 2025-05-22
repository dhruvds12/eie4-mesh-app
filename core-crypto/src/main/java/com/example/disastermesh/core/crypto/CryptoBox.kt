package com.example.disastermesh.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.rfc7748.X25519
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.ChaCha20ParameterSpec   // only to clamp
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Pure-software X25519 + ChaCha20-Poly1305 using Bouncy Castle.
 */
object CryptoBox {

    /* ---------- prefs helpers ------------------------------------ */
    // Create a shared preferences instance whose keys and values are encrypted on disc
    // Generates a master AES 256 GCM key stored in the android keystore --> use AES256 SIV to encrypt the preferences keys themselves
    // Uses aes-gcm to encrypt the preference values
    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx, "mesh_x25519_keys",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /* ---------- public key API ----------------------------------- */
    // Generates and cache an X25519
    /** returns 32-byte public key – generates pair if missing */
    fun ensureKeyPair(uid: Int, ctx: Context): ByteArray {
        prefs(ctx).getString("pub_$uid", null)?.let {
            return Base64.decode(it, Base64.NO_WRAP)
        }

        /* generate software key-pair */
        if (Security.getProvider("BC") == null)
            Security.addProvider(BouncyCastleProvider())

        val priv = ByteArray(32).also { Random.Default.nextBytes(it) }
        /* clamp per RFC 7748 */
        // clear the low 3 bits of byte 0 to clear the high bit and set the second highest bit of byte 31
        // ensures teh scalar is in the subgroup and prevents small-subgroup attacks
        priv[0] = (priv[0].toInt() and 0xF8).toByte()
        priv[31] = ((priv[31].toInt() and 0x7F) or 0x40).toByte()

        val pub = ByteArray(32)
        X25519.scalarMultBase(priv, 0, pub, 0)

        prefs(ctx).edit()
            .putString("priv_$uid", Base64.encodeToString(priv, Base64.NO_WRAP))
            .putString("pub_$uid", Base64.encodeToString(pub, Base64.NO_WRAP))
            .apply()
        return pub
    }

    /* ---------- encrypt / decrypt -------------------------------- */
    // Derive the symmetric key using shared secret
    // generate a nonce
    // AEAD cipher providing both confidentiality (ChaCha20) and integrity (Poly1305).
    fun encrypt(ctx: Context, plain: String, myUid: Int, peerPk: ByteArray): ByteArray {
        val key = sharedSecret(ctx, myUid, peerPk)
        val nonce = Random.Default.nextBytes(12)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(nonce))
        return nonce + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)) // append 16 byte tag Poly1305 tag
    }

    // Same key derivation via sharedSecret.
    //
    //Extract nonce (first 12 bytes) and ciphertext+tag (remaining).
    //
    //Initialize cipher in DECRYPT mode with the same nonce.
    //
    //doFinal(ct) simultaneously verifies the Poly1305 tag and decrypts.
    //
    //If authentication fails, a BadPaddingException (or similar) is thrown.
    //
    //Convert the result back to a UTF-8 string.
    fun decrypt(ctx: Context, cipherBytes: ByteArray, myUid: Int, peerPk: ByteArray): String {
        val key = sharedSecret(ctx, myUid, peerPk)
        val nonce = cipherBytes.sliceArray(0 until 12)
        val ct = cipherBytes.sliceArray(12 until cipherBytes.size)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(nonce))
        return cipher.doFinal(ct).toString(Charsets.UTF_8)
    }

    /* ---------- helper ------------------------------------------- */
    // Used to generate the shared secret key for a given user
    // load MY private key from memory
    private fun sharedSecret(ctx: Context, uid: Int, peerPk: ByteArray): SecretKey {
        val sp = prefs(ctx)
        val priv = Base64.decode(sp.getString("priv_$uid", null), Base64.NO_WRAP)

        val out = ByteArray(32)
        // X25519 DH: compute out = priv × peerPk, the 32-byte raw Diffie-Hellman shared secret.
        X25519.scalarMult(priv, 0, peerPk, 0, out, 0)
        // Simple HKDF: here we use a single SHA-256 hash of the raw shared secret
        //to derive a 32-byte symmetric key suitable for ChaCha20.
        val hkdf = java.security.MessageDigest.getInstance("SHA-256").digest(out)
        return SecretKeySpec(hkdf, "ChaCha20")
    }
}
