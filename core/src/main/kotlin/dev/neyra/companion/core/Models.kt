package dev.neyra.companion.core

import kotlinx.coroutines.flow.StateFlow

data class Candidate(val id: String, val name: String, val host: String, val port: Int)
data class Peer(val id: String, val name: String, val host: String, val port: Int, val fingerprint: String, val token: String)
data class Reminder(val machineId: String, val id: String, val text: String, val dueAt: Long, val status: String, val repeatSeconds: Int = 0) {
    val deliveryKey: String get() = "$machineId:$id:$dueAt"
}
data class Note(val machineId: String, val id: String, val text: String, val time: String, val title: String = "")
data class QueuedCapture(val id: String, val machineId: String, val kind: String, val text: String, val audioPath: String?, val createdAt: Long, val error: String = "")
data class RemoteJob(val id: String, val status: String, val text: String, val error: String = "")
data class Snapshot(val machineId: String, val name: String, val reminders: List<Reminder>, val notes: List<Note> = emptyList())
enum class CaptureMode(val label: String) { NOTE("Note"), REMINDER("Reminder"), ASSISTANT("Commands"), CHAT("Conversation") }
interface Discovery {
    val candidates: StateFlow<List<Candidate>>
    val error: StateFlow<String?>
    fun start()
    fun stop()
}
interface CompanionApi {
    suspend fun snapshot(peer: Peer): Snapshot
    suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String): String
    suspend fun enroll(peer: Peer): Peer = error("Update the companion API")
    suspend fun startJob(peer: Peer, kind: String, text: String, requestId: String): String = error("Update the companion API")
    suspend fun job(peer: Peer, id: String, cancel: Boolean = false): RemoteJob = error("Update the companion API")
    suspend fun forgetConversation(peer: Peer) { }
    suspend fun recurring(peer: Peer, text: String, repeatSeconds: Int, requestId: String): String = send(peer, CaptureMode.REMINDER, text, requestId)
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
