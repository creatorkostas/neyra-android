package dev.neyra.companion.core

import kotlinx.coroutines.flow.StateFlow

data class Candidate(val id: String, val name: String, val host: String, val port: Int)
data class Peer(val id: String, val name: String, val host: String, val port: Int, val fingerprint: String, val token: String)
data class Reminder(val machineId: String, val id: String, val text: String, val dueAt: Long, val status: String) {
    val deliveryKey: String get() = "$machineId:$id:$dueAt"
}
data class Snapshot(val machineId: String, val name: String, val reminders: List<Reminder>)
enum class CaptureMode(val label: String) { NOTE("Note"), REMINDER("Reminder"), ASSISTANT("Assistant") }
interface Discovery {
    val candidates: StateFlow<List<Candidate>>
    val error: StateFlow<String?>
    fun start()
    fun stop()
}
interface CompanionApi {
    suspend fun snapshot(peer: Peer): Snapshot
    suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String): String
    suspend fun reminderAction(peer: Peer, reminder: Reminder, action: String): Snapshot
}
interface PeerStore {
    fun peers(): List<Peer>
    fun save(peer: Peer)
    fun remove(id: String)
}
fun validFingerprint(value: String): Boolean = value.matches(Regex("[a-fA-F0-9]{64}"))
fun dueReminders(records: List<Reminder>, now: Long, delivered: Set<String>): List<Reminder> =
    records.filter { it.status in setOf("pending", "fired") && it.dueAt <= now && it.deliveryKey !in delivered }
