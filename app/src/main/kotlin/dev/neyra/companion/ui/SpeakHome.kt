package dev.neyra.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.neyra.companion.CompanionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SpeakHome(state: CompanionState, select: (String) -> Unit, computers: () -> Unit, speak: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!state.recording && !state.busy) expanded = it }) {
        OutlinedTextField(value = state.selected?.name ?: "Select a computer", onValueChange = {}, readOnly = true,
            label = { Text("Computer") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            enabled = !state.recording && !state.busy)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.peers.forEach { peer -> DropdownMenuItem(text = { Text(peer.name) },
                onClick = { select(peer.id); expanded = false }) }
            DropdownMenuItem(text = { Text("Manage computers") }, onClick = { expanded = false; computers() })
        }
    }
    Button(onClick = speak, enabled = state.selected != null && !state.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp)) {
        Icon(if (state.recording) Icons.Outlined.Stop else Icons.Outlined.Mic, null, Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(if (state.recording) "Stop recording" else "Speak to Neyra", style = MaterialTheme.typography.titleMedium)
    }
}
