package dev.neyra.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
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

private val Light = lightColorScheme(primary = Color(0xFF394A75), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE2F9), onPrimaryContainer = Color(0xFF162447),
    background = Color(0xFFFAF9F6), secondaryContainer = Color(0xFFDCE2F0), onSecondaryContainer = Color(0xFF253454), surface = Color(0xFFFAF9F6), onSurface = Color(0xFF1B2335), secondary = Color(0xFF565F71))
private val Dark = darkColorScheme(primary = Color(0xFFBAC7EF), onPrimary = Color(0xFF223259),
    primaryContainer = Color(0xFF394A75), onPrimaryContainer = Color(0xFFDCE2F9),
    background = Color(0xFF151922), secondaryContainer = Color(0xFF35425E), onSecondaryContainer = Color(0xFFDCE2F0), surface = Color(0xFF151922), onSurface = Color(0xFFE9EDF5), secondary = Color(0xFFBDC8DC))
private enum class Destination(val label: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Outlined.Mic), DEVICES("Computers", Icons.Outlined.Computer), INBOX("Inbox", Icons.Outlined.Notifications)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CompanionApp(model: CompanionViewModel, onSpeak: ((String) -> Unit) -> Unit, onNotifications: () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light) {
        val state by model.state.collectAsStateWithLifecycle()
        val candidates by model.candidates.collectAsStateWithLifecycle()
        val discoveryError by model.discoveryError.collectAsStateWithLifecycle()
        var destination by rememberSaveable { mutableStateOf(Destination.CAPTURE) }
        val screenState = rememberSaveableStateHolder()
        var pairing by remember { mutableStateOf<Candidate?>(null) }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); model.message(null) } }
        BoxWithConstraints {
            val wide = maxWidth >= 600.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) NavigationRail(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
                    Spacer(Modifier.height(20.dp))
                    Destination.entries.forEach { item -> NavigationRailItem(selected = destination == item,
                        onClick = { destination = item }, icon = { Icon(item.icon, null) }, label = { Text(item.label) }) }
                }
                Scaffold(modifier = Modifier.weight(1f),
                    topBar = { TopAppBar(title = { Text("Neyra") }, actions = {
                        IconButton(onClick = model::refresh, enabled = !state.busy) { Icon(Icons.Outlined.Refresh, "Sync computers") }
                    }) },
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = { if (!wide) NavigationBar { Destination.entries.forEach { item ->
                        NavigationBarItem(selected = destination == item, onClick = { destination = item },
                            icon = { Icon(item.icon, null) }, label = { Text(item.label) })
                    } } },
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        screenState.SaveableStateProvider(destination.name) {
                        when (destination) {
                            Destination.CAPTURE -> Capture(state, onSpeak, { mode, text, done -> model.send(mode, text, done) }, { destination = Destination.DEVICES })
                            Destination.DEVICES -> Devices(state, candidates, discoveryError, model::select, { pairing = it }, model::forget,
                                { model.discovery.stop(); model.discovery.start() })
                            Destination.INBOX -> Inbox(state, model::action, onNotifications)
                        }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    }
                }
            }
        }
        pairing?.let { candidate -> PairDialog(candidate, state.busy, state.message, { pairing = null }) { pin, token -> model.pair(candidate, pin, token) } }
        LaunchedEffect(state.peers) { if (state.peers.any { it.id == pairing?.id }) pairing = null }
    }
}

@Composable private fun Capture(state: CompanionState, onSpeak: ((String) -> Unit) -> Unit,
    onSend: (CaptureMode, String, () -> Unit) -> Unit, onComputers: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(CaptureMode.NOTE) }
    var text by rememberSaveable { mutableStateOf("") }
    var speechTarget by rememberSaveable { mutableStateOf<String?>(null) }
    Text("Your voice,\nyour computer.", style = MaterialTheme.typography.headlineLarge)
    Text("Capture a thought or ask Neyra to help.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.selected?.name ?: "Choose your computer", style = MaterialTheme.typography.titleLarge)
            Text(if (state.selected == null) "Pair once on your local network to get started."
                else if (state.online) "Connected on your local network" else "Not connected · check Wi-Fi and desktop Neyra",
                style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onComputers) { Text(if (state.selected == null) "Find computers" else "Change computer") }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CaptureMode.entries.forEach { item ->
        FilterChip(selected = item == mode, onClick = { mode = item }, label = { Text(item.label) }, enabled = !state.busy)
    } }
    FilledTonalButton(onClick = {
        speechTarget = state.selectedId
        onSpeak { result -> text = result }
    }, enabled = state.selected != null && !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(12.dp)); Text("Speak to Neyra", style = MaterialTheme.typography.titleMedium)
    }
    Text("Uses your phone’s speech service. Offline recognition is requested, but your speech provider may use the internet. Review the words before sending.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(value = text, onValueChange = { if (it.length <= 8000) { text = it; speechTarget = null } },
        label = { Text(if (mode == CaptureMode.REMINDER) "Remind me in 10 minutes to…" else "Speak or type your request") },
        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 10, enabled = !state.busy)
    if (speechTarget != null && speechTarget != state.selectedId) Text("Computer changed after recording. Select the original computer or edit the text before sending.", color = MaterialTheme.colorScheme.error)
    Button(onClick = { val sent = text; onSend(mode, sent) { if (text == sent) { text = ""; speechTarget = null } } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = text.isNotBlank() && state.selected != null && !state.busy && (speechTarget == null || speechTarget == state.selectedId)) {
        Text(if (mode == CaptureMode.NOTE) "Save note on ${state.selected?.name ?: "computer"}" else "Send to ${state.selected?.name ?: "computer"}")
    }
    if (mode == CaptureMode.ASSISTANT) Text("Local commands: open calculator, volume up, pause music, set a timer, daily briefing. Replies play on the computer. Screen and clipboard requests stay on the desktop.", style = MaterialTheme.typography.bodyMedium)
}

@Composable private fun Devices(state: CompanionState, candidates: List<Candidate>, error: String?, onSelect: (String) -> Unit,
    onPair: (Candidate) -> Unit, onForget: (Peer) -> Unit, scan: () -> Unit) {
    Text("Your computers", style = MaterialTheme.typography.headlineLarge)
    Text("Neyra looks for computers on the same network while this app is open.", style = MaterialTheme.typography.bodyLarge)
    if (state.peers.isNotEmpty()) {
        Text("Paired", style = MaterialTheme.typography.titleMedium)
        state.peers.forEach { peer ->
            ListItem(headlineContent = { Text(peer.name) }, supportingContent = { Text(if (state.selectedId == peer.id) "Selected for capture" else "Ready to select") },
                leadingContent = { RadioButton(selected = state.selectedId == peer.id, onClick = { onSelect(peer.id) }) },
                trailingContent = { IconButton(onClick = { onForget(peer) }, enabled = !state.busy) { Icon(Icons.Outlined.LinkOff, "Forget ${peer.name}") } })
        }
        HorizontalDivider()
    }
    Text("Nearby", style = MaterialTheme.typography.titleMedium)
    val unpaired = candidates.filterNot { candidate -> state.peers.any { it.id == candidate.id } }
    if (unpaired.isEmpty()) {
        Text("Looking for Neyra…", style = MaterialTheme.typography.titleLarge)
        Text("On your computer, open Neyra → Android companion → Enable companion. Keep both devices on the same Wi-Fi. Guest networks may block discovery.")
    }
    unpaired.forEach { candidate -> ListItem(headlineContent = { Text(candidate.name) }, supportingContent = { Text("Available to pair") },
        leadingContent = { Icon(Icons.Outlined.Computer, null) }, trailingContent = { TextButton(onClick = { onPair(candidate) }) { Text("Pair") } }) }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    OutlinedButton(onClick = scan, enabled = !state.busy) { Text("Scan again") }
    Text("Pairing verifies your computer’s certificate. Discovery alone never grants access. To revoke a phone’s access, use Companion settings on that computer.", style = MaterialTheme.typography.bodySmall)
}

@Composable private fun Inbox(state: CompanionState, onAction: (Reminder, String) -> Unit, onNotifications: () -> Unit) {
    Text("With you,\naway from your desk.", style = MaterialTheme.typography.headlineLarge)
    Text("Reminders from your paired computers.", style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = onNotifications) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(8.dp)); Text("Notification settings") }
    Text("Syncs while open; background refresh is periodic. Android may delay alerts. Synced reminders remain available offline; new changes need your local network.", style = MaterialTheme.typography.bodySmall)
    Text(if (state.lastSync == 0L) "Not synced yet" else "Selected computer synced ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.lastSync))}", style = MaterialTheme.typography.labelLarge)
    if (state.reminders.isEmpty()) {
        Text("A little room to breathe.", style = MaterialTheme.typography.titleLarge)
        Text("Your reminders will appear here after pairing and syncing. Try saying “Remind me in 10 minutes to take a break.”")
    }
    state.reminders.sortedBy { it.dueAt }.forEach { reminder ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(reminder.text, style = MaterialTheme.typography.titleMedium)
            Text("${state.peers.find { it.id == reminder.machineId }?.name ?: "Computer"} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(reminder.dueAt))} · ${reminder.status}", style = MaterialTheme.typography.bodySmall)
            if (reminder.status != "done") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            OutlinedTextField(pin, { pin = it.filterNot(Char::isWhitespace) }, label = { Text("Certificate fingerprint") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(token, { token = it.filterNot(Char::isWhitespace) }, label = { Text("Phone token") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        } }, confirmButton = { TextButton(onClick = { pair(pin, token) }, enabled = !busy && validFingerprint(pin) && token.length == 64) { Text("Verify and pair") } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("Cancel") } })
}
