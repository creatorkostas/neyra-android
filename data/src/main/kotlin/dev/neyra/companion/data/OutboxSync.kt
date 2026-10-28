package dev.neyra.companion.data

import android.content.Context
import android.util.Base64
import dev.neyra.companion.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

object OutboxSync {
    private val lock = Mutex()
    suspend fun flush(context: Context, api: CompanionApi, peers: List<Peer>) = lock.withLock {
        withContext(Dispatchers.IO) {
            val db = CompanionDatabase.get(context)
            val unavailable = mutableSetOf<String>()
            val store = EncryptedPeerStore.get(context)
            var audioStarted = false
            for (original in db.queue().take(10)) {
                if (original.machineId in unavailable) continue
                val peer = peers.find { it.id == original.machineId } ?: continue
                if (store.peers().none { it.id == peer.id }) continue
                try {
                    var entry = original
                    if (entry.kind == "AUDIO") {
                        if (audioStarted) continue
                        audioStarted = true
                        val file = File(entry.audioPath ?: error("Recording missing"))
                        require(file.length() in 3200..1_920_000) { "Recording missing or too large." }
                        val id = api.startJob(peer, "transcribe", Base64.encodeToString(file.readBytes(), Base64.NO_WRAP), entry.id + ":audio:" + UUID.randomUUID().toString())
                        val result = awaitJob(api, peer, id)
                        require(result.isNotBlank()) { "No speech recognized. Record again." }
                        db.transcribed(entry.id, result)
                        entry = entry.copy(kind = "NOTE", text = result)
                    }
                    if (store.peers().none { it.id == peer.id } || db.queue().none { it.id == entry.id }) continue
                    api.send(peer, CaptureMode.NOTE, entry.text, entry.id)
                    db.remove(entry.id)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { db.error(original.id, e.message ?: "Waiting for this computer"); unavailable.add(peer.id); }
            }
        }
    }
}

suspend fun awaitJob(api: CompanionApi, peer: Peer, id: String, partial: (String) -> Unit = {}): String {
    try {
        return withTimeout(300_000) {
            while (true) {
                val job = api.job(peer, id)
                partial(job.text)
                when (job.status) {
                    "done" -> return@withTimeout job.text
                    "failed", "cancelled" -> error(job.error.ifBlank { "Request cancelled" })
                }
                delay(500)
            }
            @Suppress("UNREACHABLE_CODE") ""
        }
    } catch (e: Exception) {
        withContext(NonCancellable) { runCatching { api.job(peer, id, true) } }
        throw e
    }
}
