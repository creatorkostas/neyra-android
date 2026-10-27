package dev.neyra.companion.data

import dev.neyra.companion.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.*

class TlsCompanionApi : CompanionApi {
    private suspend fun request(peer: Peer, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        require(validFingerprint(peer.fingerprint)) { "Invalid desktop certificate fingerprint." }
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, auth: String) { throw CertificateException("Client certificates unsupported") }
            override fun checkServerTrusted(chain: Array<X509Certificate>, auth: String) {
                val actual = MessageDigest.getInstance("SHA-256").digest(chain.first().encoded)
                val expected = peer.fingerprint.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                if (!MessageDigest.isEqual(actual, expected)) throw CertificateException("Desktop identity changed. Pair again on the computer.")
                chain.first().checkValidity()
            }
        }
        val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
        val host = if (peer.host.contains(":")) "[${peer.host.replace("%", "%25")}]" else peer.host
        val connection = URL("https://$host:${peer.port}$path").openConnection() as HttpsURLConnection
        try {
            connection.sslSocketFactory = tls.socketFactory
            // Exact certificate pin is the identity, not an untrusted mDNS hostname.
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 6000; connection.readTimeout = 12000
            connection.setRequestProperty("Authorization", "Bearer ${peer.token}")
            if (body != null) {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 2_000_000) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            require(bytes.size <= 2_000_000) { "Desktop response too large." }
            val result = JSONObject(String(bytes, Charsets.UTF_8))
            check(status in 200..299) { result.optString("error", "Desktop request failed ($status)") }
            result
        } finally { connection.disconnect() }
    }
    private fun snapshot(json: JSONObject, expected: String): Snapshot {
        check(json.getString("machineId") == expected) { "Wrong computer identity. Pair again." }
        val records = json.getJSONArray("reminders")
        return Snapshot(expected, json.getString("name"), (0 until records.length()).map { i -> records.getJSONObject(i).let {
            Reminder(expected, it.getString("id"), it.getString("text"), (it.getDouble("due_at") * 1000).toLong(), it.getString("status"))
        } })
    }
    override suspend fun snapshot(peer: Peer) = snapshot(request(peer, "/v1/snapshot"), peer.id)
    override suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String): String =
        request(peer, "/v1/capture", JSONObject().put("mode", mode.name).put("text", text).put("requestId", requestId)).getString("message")
    override suspend fun reminderAction(peer: Peer, reminder: Reminder, action: String): Snapshot =
        snapshot(request(peer, "/v1/reminder", JSONObject().put("id", reminder.id).put("due_ms", reminder.dueAt).put("action", action).put("requestId", UUID.randomUUID().toString())), peer.id)
}
