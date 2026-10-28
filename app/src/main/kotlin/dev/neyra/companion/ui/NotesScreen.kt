package dev.neyra.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.neyra.companion.CompanionState

@Composable fun NotesScreen(state: CompanionState, retry: () -> Unit, remove: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<String?>(null) }
    var showAllQueued by rememberSaveable { mutableStateOf(false) }
    Text("Worth keeping.", style = MaterialTheme.typography.headlineLarge)
    Text("Search recent cached notes from your paired computers, even offline. Long notes are cached as excerpts.", style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(query, { query = it }, label = { Text("Search notes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    val queuedMatches = state.outbox.filter { query.isBlank() || it.text.contains(query, true) || (it.kind == "AUDIO" && "voice note".contains(query, true)) }
    if (queuedMatches.isNotEmpty()) {
        Text("Outbox · ${state.outbox.size}", style = MaterialTheme.typography.titleLarge)
        Text("Queued notes keep their original destination. Recordings transcribe and save automatically after reconnecting.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = retry, enabled = !state.syncingOutbox) { Text(if (state.syncingOutbox) "Sending queued notes…" else "Retry queued notes") }
        (if (showAllQueued) queuedMatches else queuedMatches.take(3)).forEach { item ->
            Text(if (item.kind == "AUDIO") "Voice note" else item.text.take(240), style = MaterialTheme.typography.bodyLarge)
            Text("To ${state.peers.find { it.id == item.machineId }?.name ?: "original computer"} · ${item.error.ifBlank { "Waiting to send" }}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { deleting = item.id }, enabled = !state.syncingOutbox) { Text("Delete queued note") }
            HorizontalDivider()
        }
        if (queuedMatches.size > 3) TextButton(onClick = { showAllQueued = !showAllQueued }) { Text(if (showAllQueued) "Show fewer queued notes" else "Show all ${queuedMatches.size} queued notes") }
    }
    deleting?.takeIf { id -> state.outbox.any { it.id == id } }?.let { id ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this queued note?") },
            text = { Text("The unsent note or recording will be removed from this phone.") },
            confirmButton = { TextButton(onClick = { remove(id); deleting = null }, enabled = !state.syncingOutbox) { Text("Delete note") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep note") } })
    }
    val matches = state.notes.filter { query.isBlank() || it.text.contains(query, true) || it.title.contains(query, true) }
    if (matches.isEmpty()) Text(if (state.notes.isEmpty()) "Sync a computer to bring its recent notes here." else "No matching notes.")
    matches.forEach { note ->
        var expanded by rememberSaveable(note.machineId, note.id) { mutableStateOf(false) }
        Text(note.title.ifBlank { note.text.lineSequence().firstOrNull().orEmpty().take(80) }, style = MaterialTheme.typography.titleMedium)
        Text("${state.peers.find { it.id == note.machineId }?.name ?: "Computer"} · ${note.time}", style = MaterialTheme.typography.bodySmall)
        Text(if (expanded) note.text else note.text.take(320), style = MaterialTheme.typography.bodyLarge)
        if (note.text.length > 320) TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Show less" else "Read note") }
        HorizontalDivider()
    }
}
