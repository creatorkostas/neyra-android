package dev.neyra.companion.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.neyra.companion.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Requires scripts/companion_fixture.py on the host; all content is synthetic. */
@RunWith(AndroidJUnit4::class)
class CompanionIntegrationTest {
    @Test fun notificationActionCompletesTheMatchingReminder() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val peer = Peer("fixture", "Synthetic workstation", "10.0.2.2", args.getString("port")!!.toInt(), args.getString("pin")!!, args.getString("token")!!)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val api = TlsCompanionApi()
        api.send(peer, CaptureMode.REMINDER, "remind me in ten minutes to test", "notification-reminder")
        val snapshot = api.snapshot(peer)
        val record = snapshot.reminders.single()
        val store = EncryptedPeerStore.get(context); store.save(peer)
        val cache = ReminderCache.get(context); cache.update(snapshot)
        val intent = android.content.Intent(context, NotificationActionReceiver::class.java).setAction("synthetic-action:" + java.util.UUID.randomUUID())
            .putExtra("machine", peer.id).putExtra("id", record.id).putExtra("due", record.dueAt).putExtra("action", "done")
        NotificationActionReceiver().onReceive(context, intent)
        kotlinx.coroutines.withTimeout(20_000) {
            while (cache.records().none { it.id == record.id && it.status == "done" }) kotlinx.coroutines.delay(200)
        }
        assertEquals("done", cache.records().single().status)
        store.remove(peer.id); cache.forget(peer.id); CompanionDatabase.get(context).forget(peer.id)
        Unit
    }

    @Test fun qrEnrollmentJobsAndOfflineNoteCache() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val json = org.json.JSONObject().put("type", "neyra-pair-v1").put("id", "fixture").put("name", "Synthetic workstation")
            .put("host", "10.0.2.2").put("port", args.getString("port")!!.toInt()).put("fingerprint", args.getString("pin")).put("invite", args.getString("invite"))
        val candidate = parsePairingQr(json.toString())
        val api = TlsCompanionApi()
        val peer = api.enroll(candidate)
        assertNotEquals(candidate.token, peer.token)
        val chat = api.startJob(peer, "chat", "Synthetic question", "test-chat")
        assertEquals("Synthetic phone answer", awaitJob(api, peer, chat))
        val audio = android.util.Base64.encodeToString(ByteArray(3200), android.util.Base64.NO_WRAP)
        val transcription = api.startJob(peer, "transcribe", audio, "test-audio")
        assertEquals("Synthetic transcript", awaitJob(api, peer, transcription))
        api.forgetConversation(peer)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedPeerStore.get(context)
        store.save(peer)
        val db = CompanionDatabase.get(context)
        db.forget(peer.id)
        val snapshot = api.snapshot(peer)
        db.cache(snapshot)
        assertTrue(db.notes().any { it.text.contains("searchable") })
        val id = db.enqueue(peer.id, "Synthetic offline note")
        val attemptIds = mutableListOf<String>()
        var fail = true
        val retrying = object : CompanionApi by api {
            override suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String): String {
                attemptIds.add(requestId)
                if (fail) throw java.net.SocketTimeoutException("Synthetic offline")
                return api.send(peer, mode, text, requestId)
            }
        }
        OutboxSync.flush(context, retrying, listOf(peer))
        assertEquals(id, db.queue().single().id)
        assertEquals(peer.id, db.queue().single().machineId)
        db.close() // SQLite helper reopens the same durable queue.
        assertEquals(id, db.queue().single().id)
        fail = false
        OutboxSync.flush(context, retrying, listOf(peer))
        assertTrue(db.queue().isEmpty())
        assertEquals(listOf(id, id), attemptIds)
        store.remove(peer.id); db.forget(peer.id)
        Unit
    }

    @Test fun pinnedTlsCaptureReminderActionsAndEncryptedPersistence() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val peer = Peer("fixture", "Synthetic workstation", "10.0.2.2", args.getString("port")!!.toInt(), args.getString("pin")!!, args.getString("token")!!)
        val api = TlsCompanionApi()
        assertEquals("fixture", api.snapshot(peer).machineId)
        assertEquals("Note saved", api.send(peer, CaptureMode.NOTE, "Synthetic note", "fixture-note"))
        api.send(peer, CaptureMode.REMINDER, "remind me in 10 minutes to test", "fixture-reminder")
        val reminder = api.snapshot(peer).reminders.single()
        assertEquals("done", api.reminderAction(peer, reminder, "done").reminders.single().status)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedPeerStore.get(context)
        store.save(peer)
        assertEquals(peer, EncryptedPeerStore.get(context).peers().single())
        store.remove(peer.id)
        assertTrue(store.peers().isEmpty())
        val cache = ReminderCache.get(context)
        cache.update(Snapshot("fixture", "Synthetic", listOf(reminder.copy(status = "done"))))
        assertEquals("done", cache.records().single().status)
        cache.forget("fixture")
        assertTrue(cache.records().isEmpty())
        try { api.snapshot(peer.copy(fingerprint = "0".repeat(64))); fail("Wrong pin accepted") }
        catch (_: javax.net.ssl.SSLHandshakeException) { }
        Unit
    }
}
