package dev.neyra.companion.data

import dev.neyra.companion.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.io.IOException
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.*

class CompanionHttpException(val status: Int, message: String) : java.io.IOException(message)

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
        val client = OkHttpClient.Builder().sslSocketFactory(tls.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }.followRedirects(false).followSslRedirects(false)
            .connectTimeout(6, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
        val builder = Request.Builder().url("https://$host:${peer.port}$path").header("Authorization", "Bearer ${peer.token}")
        if (body != null) builder.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        val call = client.newCall(builder.build())
        suspendCancellableCoroutine<JSONObject> { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown(); if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            val input = response.body?.byteStream() ?: error("Empty desktop response")
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (output.size() <= 2_000_000) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                            require(output.size() <= 2_000_000) { "Desktop response too large." }
                            val result = JSONObject(output.toString("UTF-8"))
                            if (!response.isSuccessful) throw CompanionHttpException(response.code, result.optString("error", "Desktop request failed (${response.code})"))
                            if (continuation.isActive) continuation.resume(result)
                        }
                    } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                    finally { client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
                }
            })
        }
    }
    private fun snapshot(json: JSONObject, expected: String): Snapshot {
        check(json.getString("machineId") == expected) { "Wrong computer identity. Pair again." }
        val records = json.getJSONArray("reminders")
        return Snapshot(expected, json.getString("name"), (0 until records.length()).map { i -> records.getJSONObject(i).let {
            Reminder(expected, it.getString("id"), it.getString("text"), (it.getDouble("due_at") * 1000).toLong(), it.getString("status"), it.optInt("repeat_seconds"))
        } }, json.optJSONArray("notes")?.let { notes -> (0 until notes.length()).map { i -> notes.getJSONObject(i).let {
            Note(expected, it.getString("id"), it.getString("text"), it.getString("time"), it.optString("title"))
        } } } ?: emptyList())
    }
    override suspend fun snapshot(peer: Peer) = snapshot(request(peer, "/v1/snapshot"), peer.id)
    override suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String): String =
        request(peer, "/v1/capture", JSONObject().put("mode", mode.name).put("text", text).put("requestId", requestId)).getString("message")
    override suspend fun enroll(peer: Peer): Peer {
        val result = request(peer, "/v1/enroll", JSONObject())
        check(result.getString("machineId") == peer.id) { "QR belongs to a different computer." }
        return peer.copy(token = result.getString("token"))
    }
    override suspend fun startJob(peer: Peer, kind: String, text: String, requestId: String): String =
        request(peer, if (kind == "chat") "/v1/chat" else "/v1/transcribe", JSONObject().put(if (kind == "chat") "text" else "pcm", text).put("requestId", requestId)).getString("jobId")
    override suspend fun job(peer: Peer, id: String, cancel: Boolean): RemoteJob {
        val result = request(peer, "/v1/job", JSONObject().put("jobId", id).put("action", if (cancel) "cancel" else "status"))
        return RemoteJob(id, result.getString("status"), result.getString("text"), result.optString("error"))
    }
    override suspend fun forgetConversation(peer: Peer) { request(peer, "/v1/forget", JSONObject()) }
    override suspend fun recurring(peer: Peer, text: String, repeatSeconds: Int, requestId: String): String =
        request(peer, "/v1/capture", JSONObject().put("mode", "REMINDER").put("text", text).put("repeatSeconds", repeatSeconds).put("requestId", requestId)).getString("message")
    override suspend fun reminderAction(peer: Peer, reminder: Reminder, action: String): Snapshot = actionRequest(peer, reminder, action, UUID.randomUUID().toString())
    suspend fun actionRequest(peer: Peer, reminder: Reminder, action: String, requestId: String): Snapshot =
        snapshot(request(peer, "/v1/reminder", JSONObject().put("id", reminder.id).put("due_ms", reminder.dueAt).put("action", action).put("requestId", requestId)), peer.id)
}
