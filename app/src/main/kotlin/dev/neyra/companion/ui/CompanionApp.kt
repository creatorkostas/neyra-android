package dev.neyra.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neyra.companion.*
import dev.neyra.companion.core.*
import java.text.DateFormat
import java.util.Date

private enum class Destination(val label: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Outlined.Mic), COMPOSE("Compose", Icons.Outlined.EditNote), DEVICES("PCs", Icons.Outlined.Computer), INBOX("Inbox", Icons.Outlined.Notifications), NOTES("Notes", Icons.Outlined.Notes)
}

private class CaptureDraft(modeValue: String = "NOTE", textValue: String = "", targetValue: String? = null,
    repeatValue: Int = 0, imported: Long = 0L, reviewed: Long = 0L) {
    val mode = mutableStateOf(CaptureMode.valueOf(modeValue))
    val text = mutableStateOf(textValue)
    val target = mutableStateOf(targetValue)
    val repeat = mutableIntStateOf(repeatValue)
    var importedVersion by mutableLongStateOf(imported)
    var reviewedVersion by mutableLongStateOf(reviewed)
    companion object {
        val saver = listSaver<CaptureDraft, String>(save = { listOf(it.mode.value.name, it.text.value, it.target.value.orEmpty(), it.repeat.intValue.toString(), it.importedVersion.toString(), it.reviewedVersion.toString()) },
            restore = { CaptureDraft(it[0], it[1], it[2].ifEmpty { null }, it[3].toInt(), it[4].toLong(), it[5].toLong()) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CompanionApp(model: CompanionViewModel, onSpeak: ((String) -> Unit) -> Unit, onNotifications: () -> Unit, onScan: () -> Unit = {}, onRead: (String) -> Unit = {}, onStopSpeech: () -> Unit = {}) {
    NeyraTheme {
        val state by model.state.collectAsStateWithLifecycle()
        val candidates by model.candidates.collectAsStateWithLifecycle()
        val discoveryError by model.discoveryError.collectAsStateWithLifecycle()
        var destination by rememberSaveable { mutableStateOf(Destination.CAPTURE) }
        val captureDraft = rememberSaveable(saver = CaptureDraft.saver) { CaptureDraft() }
        val screenState = rememberSaveableStateHolder()
        var forgetting by remember { mutableStateOf<Peer?>(null) }
        var pairing by remember { mutableStateOf<Candidate?>(null) }
        val snackbar = remember { SnackbarHostState() }
        var lastRead by rememberSaveable { mutableLongStateOf(0L) }
        LaunchedEffect(state.answerVersion) {
            if (state.answerVersion != 0L && state.answerVersion != lastRead && state.answerTarget == state.selectedId) {
                lastRead = state.answerVersion; onRead(state.answer)
            }
        }
        var navigatedImport by rememberSaveable { mutableLongStateOf(0L) }
        LaunchedEffect(state.importVersion) {
            if (state.importVersion != 0L && state.importVersion != navigatedImport) {
                destination = Destination.COMPOSE; navigatedImport = state.importVersion
            }
        }
        var wasRecording by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state.recording) {
            if (wasRecording && !state.recording) destination = Destination.COMPOSE
            wasRecording = state.recording
        }
        LaunchedEffect(state.reviewVersion) {
            if (state.reviewVersion != 0L && state.reviewVersion != captureDraft.reviewedVersion) destination = Destination.COMPOSE
        }
        LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); model.message(null) } }
        BoxWithConstraints {
            val wide = maxWidth >= 600.dp
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer)) {
                if (wide) NavigationRail(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Spacer(Modifier.height(20.dp))
                    Destination.entries.forEach { item -> NavigationRailItem(selected = destination == item,
                        onClick = { destination = item }, icon = { Icon(item.icon, null) }, label = { Text(item.label) }, colors = NavigationRailItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.onPrimary, selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer, indicatorColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer)) }
                }
                Scaffold(modifier = Modifier.weight(1f),
                    topBar = { if (destination == Destination.CAPTURE) Surface(color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.statusBarsPadding()) { ComputerHeader(state, model::select, { destination = Destination.DEVICES }) } } else TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer), title = { Text("neyra", style = MaterialTheme.typography.titleMedium) }, actions = {
                        if (destination != Destination.CAPTURE) IconButton(onClick = model::refresh, enabled = !state.busy) { Icon(Icons.Outlined.Refresh, "Sync computers") }
                    }) },
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = { if (!wide) Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(28.dp), shadowElevation = 2.dp) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                                Destination.entries.forEach { item ->
                                    val selected = destination == item
                                    Column(Modifier.weight(1f).selectable(selected = selected, role = Role.Tab,
                                        onClick = { destination = item }).padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(item.icon, null, Modifier.size(22.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(item.label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal))
                                        Box(Modifier.size(width = 16.dp, height = 3.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp)))
                                    }
                                }
                            }
                        }
                    } },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    screenState.SaveableStateProvider(destination.name) {
                    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (state.busy && destination != Destination.CAPTURE) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            TextButton(onClick = { model.cancelOperation(); onStopSpeech() }) { Text("Stop request") }
                        }
                        when (destination) {
                            Destination.CAPTURE -> SpeakHome(state) {
                                onStopSpeech()
                                if (!state.recording) captureDraft.target.value = state.selectedId
                                val stopping = state.recording
                                onSpeak { result -> captureDraft.text.value = result; destination = Destination.COMPOSE }
                                if (stopping) destination = Destination.COMPOSE
                            }
                            Destination.COMPOSE -> Capture(state, model, captureDraft, onSpeak, { destination = Destination.DEVICES }, onRead, onStopSpeech)
                            Destination.DEVICES -> Devices(state, candidates, discoveryError, model::select, { pairing = it }, { peer -> if (state.outbox.any { it.machineId == peer.id }) forgetting = peer else model.forget(peer) },
                                { model.discovery.stop(); model.discovery.start() }, onScan)
                            Destination.INBOX -> Inbox(state, model::action, onNotifications)
                            Destination.NOTES -> NotesScreen(state, model::flushQueue, model::deleteQueued)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    }
                    }
                }
            }
        }
        forgetting?.let { peer ->
            AlertDialog(onDismissRequest = { forgetting = null }, title = { Text("Forget ${peer.name}?") },
                text = { Text("This deletes ${state.outbox.count { it.machineId == peer.id }} unsent notes or recordings and the cached notes from this phone.") },
                confirmButton = { TextButton(onClick = { model.forget(peer); forgetting = null }, enabled = !state.busy && !state.syncingOutbox) { Text("Delete queued notes and forget") } },
                dismissButton = { TextButton(onClick = { forgetting = null }) { Text("Cancel") } })
        }
        pairing?.let { candidate -> PairDialog(candidate, state.busy, state.message, { pairing = null }) { pin, token -> model.pair(candidate, pin, token) } }
        LaunchedEffect(state.peers) { if (state.peers.any { it.id == pairing?.id }) pairing = null }
    }
}

@Composable private fun Capture(state: CompanionState, model: CompanionViewModel, draft: CaptureDraft, onSpeak: ((String) -> Unit) -> Unit,
    onComputers: () -> Unit, onRead: (String) -> Unit, onStopSpeech: () -> Unit) {
    var mode by draft.mode
    var text by draft.text
    var speechTarget by draft.target
    var repeat by draft.repeat
    val draftView = remember { BringIntoViewRequester() }
    LaunchedEffect(state.importVersion) {
        if (state.importVersion != 0L && state.importVersion != draft.importedVersion) {
            text = state.incomingText; mode = CaptureMode.NOTE; speechTarget = null
            draft.importedVersion = state.importVersion
            withFrameNanos { }; draftView.bringIntoView()
        }
    }
    LaunchedEffect(state.reviewVersion) {
        if (state.reviewVersion != 0L && state.reviewVersion != draft.reviewedVersion) {
            text = state.reviewText; speechTarget = state.reviewTarget
            draft.reviewedVersion = state.reviewVersion
            withFrameNanos { }; draftView.bringIntoView()
        }
    }
    Text("Compose", style = MaterialTheme.typography.headlineLarge)
    ConnectionPanel(state, onComputers)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { CaptureMode.entries.forEach { item ->
        Choice(selected = item == mode, onClick = { mode = item }, label = { Text(item.label) }, enabled = !state.busy)
    } }
    if (mode == CaptureMode.CHAT) {
        Text("Uses the assistant provider configured on your computer. Follow-ups share only this phone’s recent conversation; desktop screen, clipboard and notes are not attached.", style = MaterialTheme.typography.bodySmall)
        if (state.answer.isNotEmpty() && state.answerTarget == state.selectedId) {
            Text("Neyra", style = MaterialTheme.typography.titleMedium)
            Text(state.answer, style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onRead(state.answer) }) { Text("Read aloud") }
                TextButton(onClick = onStopSpeech) { Text("Stop voice") }
                TextButton(onClick = model::clearConversation, enabled = !state.busy) { Text("Clear conversation") }
            }
        }
    }
    ActionButton(onClick = {
        onStopSpeech()
        if (!state.recording) speechTarget = state.selectedId
        onSpeak { result -> text = result }
    }, enabled = state.selected != null && !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(if (state.recording) Icons.Outlined.Stop else Icons.Outlined.Mic, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(if (state.recording) "Stop recording" else "Speak to Neyra", style = MaterialTheme.typography.titleMedium)
    }
    Text("Records up to 60 seconds on this phone. Your selected computer transcribes with its Neyra speech engine. Audio stays private until you choose Transcribe or Queue.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.audioPath != null) {
        Text("Recording ready · ${state.peers.find { it.id == state.audioTarget }?.name ?: "original computer"}", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(secondary = true, onClick = { model.transcribe() }, enabled = !state.busy) { Text("Transcribe on computer") }
            TextButton(onClick = model::queueAudio, enabled = !state.busy) { Text("Queue as note") }
            TextButton(onClick = model::discardAudio, enabled = !state.busy) { Text("Discard recording") }
        }
        Text("Queued recordings transcribe and save as notes on their original computer after reconnection.", style = MaterialTheme.typography.bodySmall)
    }
    if (mode == CaptureMode.REMINDER) {
        Text("Repeat", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0 to "Once", 86400 to "Daily", 604800 to "Weekly").forEach { (seconds, label) ->
            Choice(selected = repeat == seconds, onClick = { repeat = seconds }, label = { Text(label) }, enabled = !state.busy)
        } }
    }
    OutlinedTextField(value = text, onValueChange = { if (it.length <= 8000) { text = it; speechTarget = null } },
        label = { Text(if (mode == CaptureMode.REMINDER) "Remind me in 10 minutes to…" else "Speak or type your request") },
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(draftView), minLines = 3, maxLines = 10, enabled = !state.busy, shape = MaterialTheme.shapes.large, colors = editorColors())
    if (speechTarget != null && speechTarget != state.selectedId) Text("Computer changed after recording. Select the original computer or edit the text before sending.", color = MaterialTheme.colorScheme.error)
    ActionButton(onClick = { val sent = text; model.send(mode, sent, { if (text == sent) { text = ""; speechTarget = null } }, repeat) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = text.isNotBlank() && state.selected != null && !state.busy && (speechTarget == null || speechTarget == state.selectedId)) {
        Text(if (mode == CaptureMode.NOTE) "Save note on ${state.selected?.name ?: "computer"}" else "Send to ${state.selected?.name ?: "computer"}")
    }
    if (mode == CaptureMode.NOTE) Text("Notes are saved in the phone outbox first and sent automatically to this computer when reachable.", style = MaterialTheme.typography.bodySmall)
    if (mode == CaptureMode.ASSISTANT) Text("Local commands: open calculator, volume up, pause music, set a timer, daily briefing. Replies play on the computer. Screen and clipboard requests stay on the desktop.", style = MaterialTheme.typography.bodyMedium)
}

@Composable private fun Devices(state: CompanionState, candidates: List<Candidate>, error: String?, onSelect: (String) -> Unit,
    onPair: (Candidate) -> Unit, onForget: (Peer) -> Unit, scan: () -> Unit, scanQr: () -> Unit) {
    Text("Your computers", style = MaterialTheme.typography.headlineLarge)
    Text("A private connection. On your network.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ContentGroup {
    ActionButton(onClick = scanQr, enabled = !state.busy) { Icon(Icons.Outlined.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Scan pairing QR") }
    Text("Open Android companion on your computer and scan its five-minute QR to pair this phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (state.peers.isNotEmpty()) {
        Text("Paired", style = MaterialTheme.typography.titleMedium)
        ContentGroup {
        state.peers.forEach { peer ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).selectable(selected = state.selectedId == peer.id,
                    role = Role.RadioButton, onClick = { onSelect(peer.id) }).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(Icons.Outlined.Computer)
                    Column(Modifier.weight(1f)) {
                        Text(peer.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (state.selectedId == peer.id) "Selected for capture" else "Tap to select",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.selectedId == peer.id) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onForget(peer) }, enabled = !state.busy) { Icon(Icons.Outlined.LinkOff, "Forget ${peer.name}") }
            }
        }
        }
    }
    Text("Nearby", style = MaterialTheme.typography.titleMedium)
    val unpaired = candidates.filterNot { candidate -> state.peers.any { it.id == candidate.id } }
    if (unpaired.isEmpty()) {
        EmptyContent(Icons.Outlined.Wifi, "Looking for Neyra…", "On your computer, open Neyra → Android companion → Enable companion. Keep both devices on the same Wi-Fi. Guest networks may block discovery.")
    }
    unpaired.forEach { candidate -> ContentGroup {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Outlined.Computer)
            Column(Modifier.weight(1f)) {
                Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                Text("Available to pair", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onPair(candidate) }) { Text("Pair") }
        }
    } }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    ActionButton(secondary = true, onClick = scan, enabled = !state.busy) { Text("Scan again") }
    Text("Pairing verifies your computer’s certificate. Discovery alone never grants access. To revoke a phone’s access, use Companion settings on that computer.", style = MaterialTheme.typography.bodySmall)
}

@Composable private fun Inbox(state: CompanionState, onAction: (Reminder, String) -> Unit, onNotifications: () -> Unit) {
    Text("Reminders", style = MaterialTheme.typography.headlineLarge)
    Text("From your computers, wherever you are at home.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ContentGroup {
    ActionButton(secondary = true, onClick = onNotifications) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Notification settings") }
    Text("Syncs while open; background refresh is periodic. Android may delay alerts. Synced reminders remain available offline; new changes need your local network.", style = MaterialTheme.typography.bodySmall)
    Text(if (state.lastSync == 0L) "Not synced yet" else "Selected computer synced ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.lastSync))}", style = MaterialTheme.typography.labelLarge)
    }
    if (state.reminders.isEmpty()) {
        EmptyContent(Icons.Outlined.NotificationsNone, "You’re all caught up", "Reminders appear here after pairing and syncing. Try saying “Remind me in 10 minutes to take a break.”")
    }
    state.reminders.sortedBy { it.dueAt }.forEach { reminder ->
        ContentGroup {
            Text(reminder.text, style = MaterialTheme.typography.titleMedium)
            Text("${state.peers.find { it.id == reminder.machineId }?.name ?: "Computer"} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(reminder.dueAt))} · ${reminder.status}${if (reminder.repeatSeconds == 86400) " · daily" else if (reminder.repeatSeconds == 604800) " · weekly" else ""}", style = MaterialTheme.typography.bodySmall)
            if (reminder.repeatSeconds > 0) TextButton(onClick = { onAction(reminder, "stop_repeat") }, enabled = !state.busy) { Text("Stop repeating") }
            if (reminder.status != "done") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAction(reminder, "done") }, enabled = !state.busy) { Text("Mark done") }
                TextButton(onClick = { onAction(reminder, "snooze") }, enabled = !state.busy) { Text("Snooze 10 min") }
            }
            HorizontalDivider()
        }
    }
}

@Composable private fun PairDialog(candidate: Candidate, busy: Boolean, error: String?, dismiss: () -> Unit, pair: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }; var token by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("Pair ${candidate.name}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Copy these values from Android companion on this computer. Verify the fingerprint there; do not trust values sent by another device.")
            OutlinedTextField(pin, { pin = it.filterNot(Char::isWhitespace) }, label = { Text("Certificate fingerprint") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), modifier = Modifier.fillMaxWidth(), colors = editorColors())
            OutlinedTextField(token, { token = it.filterNot(Char::isWhitespace) }, label = { Text("Phone token") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), colors = editorColors())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(onClick = { pair(pin, token) }, enabled = !busy && validFingerprint(pin) && token.length == 64) { Text("Verify and pair") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}
