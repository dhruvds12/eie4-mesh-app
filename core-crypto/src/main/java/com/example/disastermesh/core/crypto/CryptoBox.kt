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

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx, "mesh_x25519_keys",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /* ---------- public key API ----------------------------------- */

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
        priv[0]  = (priv[0].toInt() and 0xF8).toByte()
        priv[31] = ((priv[31].toInt() and 0x7F) or 0x40).toByte()

        val pub = ByteArray(32)
        X25519.scalarMultBase(priv, 0, pub, 0)

        prefs(ctx).edit()
            .putString("priv_$uid", Base64.encodeToString(priv, Base64.NO_WRAP))
            .putString("pub_$uid",  Base64.encodeToString(pub , Base64.NO_WRAP))
            .apply()
        return pub
    }

    /* ---------- encrypt / decrypt -------------------------------- */

    fun encrypt(ctx: Context, plain: String, myUid: Int, peerPk: ByteArray): ByteArray {
        val key    = sharedSecret(ctx, myUid, peerPk)
        val nonce  = Random.Default.nextBytes(12)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(nonce))
        return nonce + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(ctx: Context, cipherBytes: ByteArray, myUid: Int, peerPk: ByteArray): String {
        val key    = sharedSecret(ctx, myUid, peerPk)
        val nonce  = cipherBytes.sliceArray(0 until 12)
        val ct     = cipherBytes.sliceArray(12 until cipherBytes.size)
        val cipher = Cipher.getInstance("ChaCha20-Poly1305")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(nonce))
        return cipher.doFinal(ct).toString(Charsets.UTF_8)
    }

    /* ---------- helper ------------------------------------------- */

    private fun sharedSecret(ctx: Context, uid: Int, peerPk: ByteArray): SecretKey {
        val sp   = prefs(ctx)
        val priv = Base64.decode(sp.getString("priv_$uid", null), Base64.NO_WRAP)

        val out  = ByteArray(32)
        X25519.scalarMult(priv, 0, peerPk, 0, out, 0)
        val hkdf = java.security.MessageDigest.getInstance("SHA-256").digest(out)
        return SecretKeySpec(hkdf, "ChaCha20")
    }
}
