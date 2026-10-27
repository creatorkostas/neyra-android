package dev.neyra.companion.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dev.neyra.companion.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Tokens and certificate pins are encrypted with a non-exportable Android Keystore key. */
class EncryptedPeerStore private constructor(context: Context) : PeerStore {
    companion object {
        @Volatile private var instance: EncryptedPeerStore? = null
        fun get(context: Context): EncryptedPeerStore = instance ?: synchronized(this) {
            instance ?: EncryptedPeerStore(context.applicationContext).also { instance = it }
        }
    }
    private val prefs = context.getSharedPreferences("paired-machines", Context.MODE_PRIVATE)
    private val alias = "neyra.companion.peers.v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized override fun peers(): List<Peer> {
        val saved = prefs.getString("encrypted", null) ?: return emptyList()
        val parts = saved.split(":")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        val array = JSONArray(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
        return (0 until array.length()).map { i -> array.getJSONObject(i).let { Peer(it.getString("id"), it.getString("name"), it.getString("host"), it.getInt("port"), it.getString("fingerprint"), it.getString("token")) } }
    }
    private fun write(peers: List<Peer>) {
        val array = JSONArray()
        peers.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("host", it.host).put("port", it.port).put("fingerprint", it.fingerprint).put("token", it.token)) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("encrypted", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) { "Could not save pairing." }
    }
    @Synchronized override fun save(peer: Peer) = write(peers().filterNot { it.id == peer.id } + peer)
    @Synchronized override fun remove(id: String) = write(peers().filterNot { it.id == id })
}
