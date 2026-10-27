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
