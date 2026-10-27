package dev.neyra.companion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.neyra.companion.core.*
import dev.neyra.companion.data.ReminderCache
import dev.neyra.companion.ui.CompanionApp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class CaptureNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun spokenDraftAndTargetGuardSurviveNavigation() {
        val peers = listOf("first", "second").map { Peer(it, it, "127.0.0.1", 1, "a".repeat(64), "b".repeat(64)) }
        val store = object : PeerStore {
            override fun peers() = peers
            override fun save(peer: Peer) {}
            override fun remove(id: String) {}
        }
        val discovery = object : Discovery {
            override val candidates = MutableStateFlow<List<Candidate>>(emptyList())
            override val error = MutableStateFlow<String?>(null)
            override fun start() {}
            override fun stop() {}
        }
        val api = object : CompanionApi {
            override suspend fun snapshot(peer: Peer) = Snapshot(peer.id, peer.name, emptyList())
            override suspend fun send(peer: Peer, mode: CaptureMode, text: String, requestId: String) = "saved"
            override suspend fun reminderAction(peer: Peer, reminder: Reminder, action: String) = snapshot(peer)
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var model: CompanionViewModel
        compose.runOnUiThread { model = CompanionViewModel(discovery, store, api, ReminderCache.get(context)) }
        compose.setContent { CompanionApp(model, { receive -> receive("Synthetic spoken note") }, {}) }
        compose.waitUntil { model.state.value.peers.size == 2 }
        compose.onNodeWithText("Reminder").performClick()
        compose.onNodeWithText("Speak to Neyra").performClick()
        compose.onNodeWithText("Computers").performClick()
        compose.runOnIdle { model.select("second") }
        compose.onNodeWithText("Capture").performClick()
        compose.onNodeWithText("Synthetic spoken note").assertExists()
        compose.onNodeWithText("Reminder").assertIsSelected()
        compose.onNodeWithText("Send to second").assertIsNotEnabled()
        compose.onNodeWithText("Computer changed after recording.", substring = true).assertExists()
        compose.onNode(hasSetTextAction()).performTextInput(" reviewed")
        compose.onNodeWithText("Send to second").assertIsEnabled()
    }
}
