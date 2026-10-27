package dev.neyra.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.neyra.companion.core.*
import dev.neyra.companion.data.ReminderCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class CompanionState(
    val peers: List<Peer> = emptyList(), val selectedId: String? = null,
    val reminders: List<Reminder> = emptyList(), val busy: Boolean = false,
    val message: String? = null, val lastSync: Long = 0, val online: Boolean = false,
) { val selected: Peer? get() = peers.find { it.id == selectedId } }

class CompanionViewModel(
    val discovery: Discovery, private val store: PeerStore,
    private val api: CompanionApi, private val cache: ReminderCache,
) : ViewModel() {
    val state = MutableStateFlow(CompanionState())
    val candidates = discovery.candidates
    val discoveryError = discovery.error
    private var polling: Job? = null
    private var syncing = false
    init { viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { store.peers() to cache.records() } }
        .onSuccess { (peers, records) -> state.value = state.value.copy(peers = peers, selectedId = peers.firstOrNull()?.id, reminders = records) }
        .onFailure { message("Could not read saved pairing. Clear app storage and pair again.") }
    } }
    fun message(value: String?) { state.update { it.copy(message = value) } }
    fun start() {
        discovery.start()
        if (polling?.isActive == true) return
        polling = viewModelScope.launch { while (isActive) { refresh(false); delay(5000) } }
    }
    fun stop() { discovery.stop(); polling?.cancel(); polling = null }
    fun select(id: String) { state.update { it.copy(selectedId = id, online = false, lastSync = cache.lastSync(id)) }; viewModelScope.launch { refresh(true) } }
    fun pair(candidate: Candidate, pin: String, token: String) {
        if (!validFingerprint(pin) || !token.matches(Regex("[a-fA-F0-9]{64}"))) { message("Copy the full fingerprint and phone token from desktop Companion settings."); return }
        perform {
            val peer = Peer(candidate.id, candidate.name, candidate.host, candidate.port, pin.lowercase(), token)
            val snapshot = api.snapshot(peer)
            withContext(Dispatchers.IO) { store.save(peer); cache.update(snapshot) }
            state.update { it.copy(peers = it.peers.filterNot { p -> p.id == peer.id } + peer, selectedId = peer.id, online = true, reminders = cache.records(), lastSync = System.currentTimeMillis()) }
            "Paired with ${snapshot.name}"
        }
    }
    fun forget(peer: Peer) { perform {
        withContext(Dispatchers.IO) { synchronized(store) { store.remove(peer.id); cache.forget(peer.id) } }
        state.update { val peers = it.peers.filterNot { p -> p.id == peer.id }; it.copy(peers = peers, selectedId = peers.firstOrNull()?.id, reminders = cache.records(), online = false, lastSync = 0) }
        "Removed from this phone. Revoke its token on the computer too."
    } }
    private fun reachable(peer: Peer): Peer = candidates.value.find { it.id == peer.id }?.let { peer.copy(host = it.host, port = it.port) } ?: peer
    suspend fun refresh(report: Boolean) {
        if (syncing) return
        syncing = true
        try {
            val peers = state.value.peers
            for (saved in peers) {
                val peer = reachable(saved)
                runCatching {
                    val snapshot = api.snapshot(peer)
                    withContext(Dispatchers.IO) { synchronized(store) { if (store.peers().any { it.id == peer.id }) { cache.update(snapshot); if (peer != saved) store.save(peer) } } }
                    state.update { current -> current.copy(
                        peers = current.peers.map { if (it.id == peer.id) peer else it },
                        reminders = cache.records(),
                        online = if (current.selectedId == peer.id) true else current.online,
                        lastSync = if (current.selectedId == peer.id) System.currentTimeMillis() else current.lastSync,
                    ) }
                }.onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    state.update { it.copy(online = if (it.selectedId == peer.id) false else it.online) }
                    if (report) message(failure.message ?: "Computer unreachable. Check Wi-Fi and desktop Companion settings.")
                }
            }
        } finally { syncing = false }
    }
    fun refresh() { viewModelScope.launch { refresh(true) } }
    fun send(mode: CaptureMode, text: String, onSuccess: () -> Unit = {}) {
        val peer = state.value.selected?.let(::reachable) ?: return message("Pair and select a computer first.")
        if (text.isBlank() || text.length > 8000) return message("Enter between 1 and 8,000 characters.")
        val requestId = UUID.randomUUID().toString()
        perform { val result = api.send(peer, mode, text.trim(), requestId); onSuccess(); refresh(false); result }
    }
    fun action(reminder: Reminder, action: String) {
        val peer = state.value.peers.find { it.id == reminder.machineId }?.let(::reachable) ?: return
        perform {
            val snapshot = api.reminderAction(peer, reminder, action)
            withContext(Dispatchers.IO) { cache.update(snapshot) }
            state.update { it.copy(reminders = cache.records()) }
            if (action == "done") "Reminder completed" else "Snoozed for 10 minutes"
        }
    }
    private fun perform(block: suspend () -> String) {
        if (state.value.busy) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { message(block()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message(e.message ?: "Could not connect. Check Wi-Fi and try again.") }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() { discovery.stop() }
}
