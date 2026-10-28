package dev.neyra.companion

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.neyra.companion.core.*
import dev.neyra.companion.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

data class CompanionState(
    val peers: List<Peer> = emptyList(), val selectedId: String? = null,
    val reminders: List<Reminder> = emptyList(), val notes: List<Note> = emptyList(), val outbox: List<QueuedCapture> = emptyList(),
    val initialized: Boolean = false,
    val busy: Boolean = false, val message: String? = null, val lastSync: Long = 0, val online: Boolean = false,
    val recording: Boolean = false, val audioPath: String? = null, val audioTarget: String? = null,
    val answer: String = "", val answerTarget: String? = null, val answerVersion: Long = 0L,
    val reviewText: String = "", val reviewTarget: String? = null, val reviewVersion: Long = 0L,
    val incomingText: String = "", val importVersion: Long = 0L, val syncingOutbox: Boolean = false,
) { val selected: Peer? get() = peers.find { it.id == selectedId } }

class CompanionViewModel(
    val discovery: Discovery, private val store: PeerStore, private val api: CompanionApi,
    private val cache: ReminderCache, private val context: Context? = null,
    private val database: CompanionDatabase? = null, private val recorder: PhoneRecorder? = null,
) : ViewModel() {
    val state = MutableStateFlow(CompanionState())
    val candidates = discovery.candidates
    val discoveryError = discovery.error
    private var polling: Job? = null
    private var operation: Job? = null
    private var recordingLimit: Job? = null
    private var syncing = false
    private var flushing = false
    init { viewModelScope.launch {
        try {
            val peers = withContext(Dispatchers.IO) { store.peers() }
            val prefs = context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)
            val path = prefs?.getString("audio", null)?.takeIf { File(it).exists() }
            state.update { it.copy(peers = peers, selectedId = prefs?.getString("selected", null)?.takeIf { id -> peers.any { it.id == id } } ?: peers.firstOrNull()?.id, audioPath = path, audioTarget = prefs?.getString("target", null), reviewText = prefs?.getString("reviewText", "").orEmpty(), reviewTarget = prefs?.getString("reviewTarget", null), reviewVersion = prefs?.getLong("reviewVersion", 0L) ?: 0L) }
            loadLocal()
            state.update { it.copy(initialized = true) }
        } catch (_: Exception) { message("Could not read saved pairing. Clear app storage and pair again."); state.update { it.copy(initialized = true) } }
    } }
    fun message(value: String?) { state.update { it.copy(message = value) } }
    fun importText(text: String) { state.update { it.copy(incomingText = text.take(8000), importVersion = System.currentTimeMillis()) } }
    private suspend fun loadLocal() {
        val local = withContext(Dispatchers.IO) { Triple(cache.records(), database?.notes().orEmpty(), database?.queue().orEmpty()) }
        state.update { it.copy(reminders = local.first, notes = local.second, outbox = local.third) }
    }
    fun start() {
        discovery.start()
        if (polling?.isActive == true) return
        polling = viewModelScope.launch { while (isActive) { refresh(false); delay(5000) } }
    }
    fun stop() { discovery.stop(); polling?.cancel(); polling = null; if (state.value.recording) stopRecording() }
    fun select(id: String) { context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.putString("selected", id)?.apply(); state.update { it.copy(selectedId = id, online = false, lastSync = cache.lastSync(id), answer = "", answerTarget = null) }; viewModelScope.launch { refresh(true) } }
    fun pair(candidate: Candidate, pin: String, token: String) {
        if (!validFingerprint(pin) || !token.matches(Regex("[a-fA-F0-9]{64}"))) return message("Copy the full fingerprint and phone token from desktop Companion settings.")
        savePair(Peer(candidate.id, candidate.name, candidate.host, candidate.port, pin.lowercase(), token), false)
    }
    fun pairQr(value: String) {
        try { savePair(parsePairingQr(value), true) }
        catch (_: Exception) { message("Scan the QR shown in Neyra’s Android companion window.") }
    }
    private fun savePair(candidate: Peer, enroll: Boolean) { perform {
        val address = reachable(candidate)
        val peer = if (enroll) api.enroll(address) else address
        val snapshot = api.snapshot(peer)
        withContext(Dispatchers.IO) { store.save(peer); cache.update(snapshot); database?.cache(snapshot) }
        state.update { it.copy(peers = it.peers.filterNot { p -> p.id == peer.id } + peer, selectedId = peer.id, online = true, lastSync = System.currentTimeMillis()) }
        loadLocal(); "Paired with ${snapshot.name}"
    } }
    fun forget(peer: Peer) { perform {
        withContext(Dispatchers.IO) { synchronized(store) { store.remove(peer.id); cache.forget(peer.id); database?.forget(peer.id) } }
        state.update { val peers = it.peers.filterNot { p -> p.id == peer.id }; it.copy(peers = peers, selectedId = peers.firstOrNull()?.id, online = false, lastSync = 0, answer = "") }
        loadLocal(); "Removed locally. Revoke its token on the computer too."
    } }
    private fun reachable(peer: Peer): Peer = candidates.value.find { it.id == peer.id }?.let { peer.copy(host = it.host, port = it.port) } ?: peer
    suspend fun refresh(report: Boolean) {
        if (syncing) return
        syncing = true
        try {
            for (saved in state.value.peers) {
                val peer = reachable(saved)
                try {
                    val snapshot = api.snapshot(peer)
                    withContext(Dispatchers.IO) { synchronized(store) {
                        if (store.peers().any { it.id == peer.id }) { cache.update(snapshot); database?.cache(snapshot); if (peer != saved) store.save(peer) }
                    } }
                    state.update { current -> current.copy(peers = current.peers.map { if (it.id == peer.id) peer else it },
                        online = if (current.selectedId == peer.id) true else current.online,
                        lastSync = if (current.selectedId == peer.id) System.currentTimeMillis() else current.lastSync) }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    state.update { it.copy(online = if (it.selectedId == peer.id) false else it.online) }
                    if (report) message(e.message ?: "Computer unreachable. Check Wi-Fi and desktop Companion settings.")
                }
            }
            loadLocal()
            flushQueue()
        } finally { syncing = false }
    }
    fun refresh() { viewModelScope.launch { refresh(true) } }
    fun flushQueue() {
        if (flushing || context == null || database == null) return
        flushing = true
        state.update { it.copy(syncingOutbox = true) }
        viewModelScope.launch {
            try { OutboxSync.flush(context, api, state.value.peers.map(::reachable)); loadLocal() }
            finally { flushing = false; state.update { it.copy(syncingOutbox = false) } }
        }
    }
    fun deleteQueued(id: String) { if (!flushing) viewModelScope.launch { withContext(Dispatchers.IO) { database?.remove(id) }; loadLocal() } }
    fun send(mode: CaptureMode, text: String, onSuccess: () -> Unit = {}, repeatSeconds: Int = 0) {
        val peer = state.value.selected?.let(::reachable) ?: return message("Pair and select a computer first.")
        if (text.isBlank() || text.length > 8000) return message("Enter between 1 and 8,000 characters.")
        val requestId = UUID.randomUUID().toString()
        if (mode == CaptureMode.CHAT) {
            perform {
                val id = api.startJob(peer, "chat", text.trim(), requestId)
                state.update { it.copy(answer = "", answerTarget = peer.id) }
                val answer = awaitJob(api, peer, id) { partial -> state.update { it.copy(answer = partial, answerTarget = peer.id) } }
                state.update { it.copy(answer = answer, answerTarget = peer.id, answerVersion = System.currentTimeMillis()) }
                onSuccess(); clearReview(); "Answer ready on your phone"
            }
        } else perform {
            val result = if (mode == CaptureMode.NOTE && database != null) {
                withContext(Dispatchers.IO) { database.enqueue(peer.id, text.trim()) }
                loadLocal(); flushQueue(); "Saved on phone. Queued for ${peer.name}."
            } else if (mode == CaptureMode.REMINDER) api.recurring(peer, text.trim(), repeatSeconds, requestId)
            else api.send(peer, mode, text.trim(), requestId)
            onSuccess(); clearReview(); result
        }
    }
    fun action(reminder: Reminder, action: String) {
        val peer = state.value.peers.find { it.id == reminder.machineId }?.let(::reachable) ?: return
        perform {
            val snapshot = api.reminderAction(peer, reminder, action)
            withContext(Dispatchers.IO) { cache.update(snapshot); database?.cache(snapshot) }
            loadLocal(); when (action) { "done" -> "Reminder completed"; "stop_repeat" -> "Repeating stopped"; else -> "Snoozed for 10 minutes" }
        }
    }
    fun toggleRecording() {
        if (state.value.recording) return stopRecording()
        if (recorder == null) return message("Recording unavailable")
        val peer = state.value.selected ?: return message("Select a computer first.")
        if (state.value.audioPath != null) return message("Transcribe, queue, or discard the current recording first.")
        try {
            recorder.start()
            context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.putString("audio", recorder.currentFile?.absolutePath)?.putString("target", peer.id)?.apply()
            state.update { it.copy(recording = true, audioTarget = peer.id) }
            recordingLimit = viewModelScope.launch { delay(60_000); if (state.value.recording) stopRecording() }
        } catch (e: Exception) { message(e.message ?: "Could not open microphone") }
    }
    private fun stopRecording() {
        if (!state.value.recording) return
        recordingLimit?.cancel()
        state.update { it.copy(recording = false) }
        viewModelScope.launch {
            try {
                val file = recorder?.finish() ?: return@launch
                state.update { it.copy(audioPath = file.absolutePath) }
                context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.putString("audio", file.absolutePath)?.putString("target", state.value.audioTarget)?.apply()
            } catch (e: Exception) { message(e.message ?: "Recording failed") }
        }
    }
    fun discardAudio() {
        val path = state.value.audioPath
        state.update { it.copy(audioPath = null, audioTarget = null) }
        context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.remove("audio")?.remove("target")?.apply()
        viewModelScope.launch(Dispatchers.IO) { path?.let { File(it).delete() } }
    }
    fun queueAudio() {
        val path = state.value.audioPath ?: return
        val target = state.value.audioTarget ?: return
        perform {
            withContext(Dispatchers.IO) { database?.enqueue(target, audio = File(path)) ?: error("Outbox unavailable") }
            context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.remove("audio")?.remove("target")?.apply()
            state.update { it.copy(audioPath = null, audioTarget = null) }
            loadLocal(); flushQueue(); "Recording queued. It will transcribe and save as a note on its original computer."
        }
    }
    fun transcribe() {
        val path = state.value.audioPath ?: return
        val peer = state.value.peers.find { it.id == state.value.audioTarget }?.let(::reachable) ?: return
        perform {
            val audio = withContext(Dispatchers.IO) { Base64.encodeToString(File(path).readBytes(), Base64.NO_WRAP) }
            val id = api.startJob(peer, "transcribe", audio, UUID.randomUUID().toString())
            val text = awaitJob(api, peer, id)
            require(text.isNotBlank()) { "No speech recognized. Your recording is kept; try another desktop engine or record again." }
            val version = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                val saved = context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.putString("reviewText", text)?.putString("reviewTarget", peer.id)?.putLong("reviewVersion", version)?.remove("audio")?.remove("target")?.commit() ?: true
                check(saved) { "Could not save transcript locally. Your recording is still available." }
                File(path).delete()
            }
            state.update { it.copy(audioPath = null, audioTarget = null, reviewText = text, reviewTarget = peer.id, reviewVersion = version) }
            "Transcribed on ${peer.name}. Review before sending."
        }
    }
    private fun clearReview() {
        context?.getSharedPreferences("capture-draft", Context.MODE_PRIVATE)?.edit()?.remove("reviewText")?.remove("reviewTarget")?.remove("reviewVersion")?.apply()
        state.update { it.copy(reviewText = "", reviewTarget = null, reviewVersion = 0L) }
    }
    fun cancelOperation() { operation?.cancel(); message("Stopped") }
    fun clearConversation() {
        val peer = state.value.selected?.let(::reachable) ?: return
        perform { api.forgetConversation(peer); state.update { it.copy(answer = "", answerTarget = null) }; "Conversation cleared" }
    }
    private fun perform(block: suspend () -> String) {
        if (state.value.busy) return
        state.update { it.copy(busy = true) }
        operation = viewModelScope.launch {
            try { message(block()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message(e.message ?: "Could not connect. Check Wi-Fi and try again.") }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() { discovery.stop(); if (recorder != null) CoroutineScope(Dispatchers.IO).launch { runCatching { recorder.finish() } } }
}
