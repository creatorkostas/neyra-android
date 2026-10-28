package dev.neyra.companion.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.neyra.companion.CompanionState

@Composable fun ComputerHeader(state: CompanionState, select: (String) -> Unit, computers: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("neyra", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.widthIn(max = 250.dp).weight(1f, fill = false)) {
            Surface(shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Row(Modifier.clickable(enabled = !state.recording && !state.busy, role = Role.Button,
                    onClickLabel = "Select computer", onClick = { expanded = !expanded })
                    .heightIn(min = 48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Computer, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.selected?.name ?: "Select a computer", style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.peers.forEach { peer -> DropdownMenuItem(text = { Text(peer.name) },
                    onClick = { select(peer.id); expanded = false }) }
                DropdownMenuItem(text = { Text("Manage computers") }, onClick = { expanded = false; computers() })
            }
        }
    }
}

@Composable fun SpeakHome(state: CompanionState, speak: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f), label = "Speak press")
    val enabled = state.selected != null && !state.busy
    Box(Modifier.fillMaxWidth().padding(top = 112.dp, bottom = 64.dp), contentAlignment = Alignment.Center) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(48.dp),
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = if (enabled && !pressed) 2.dp else 0.dp,
            modifier = Modifier.sizeIn(minWidth = 184.dp, minHeight = 184.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            Column(Modifier.clickable(enabled = enabled, role = Role.Button, interactionSource = interaction,
                indication = null, onClick = speak).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Icon(if (state.recording) Icons.Outlined.Stop else Icons.Outlined.Mic, null, Modifier.size(52.dp))
                Spacer(Modifier.height(14.dp))
                Text(if (state.recording) "Stop recording" else "Speak to Neyra", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
