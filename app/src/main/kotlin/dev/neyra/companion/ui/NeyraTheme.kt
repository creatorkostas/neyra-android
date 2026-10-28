package dev.neyra.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neyra.companion.CompanionState

private val Day = lightColorScheme(
    primary = Color(0xFF245EDB), onPrimary = Color.White,
    primaryContainer = Color(0xFFF5F6F8), onPrimaryContainer = Color(0xFF181B22),
    secondary = Color(0xFF566174), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFFF), onSecondaryContainer = Color(0xFF245EDB),
    background = Color(0xFFF5F6F8), surface = Color(0xFFF5F6F8), onSurface = Color(0xFF181B22),
    surfaceVariant = Color(0xFFEBEDF1), onSurfaceVariant = Color(0xFF5C6370),
    outline = Color(0xFF7C8390), outlineVariant = Color(0xFFDCE0E7),
    surfaceContainer = Color.White, surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFEBEDF1), surfaceContainerHighest = Color(0xFFE1E5EC))
private val Night = darkColorScheme(
    primary = Color(0xFF91B5FF), onPrimary = Color(0xFF102B60),
    primaryContainer = Color(0xFF101216), onPrimaryContainer = Color(0xFFF2F3F7),
    secondary = Color(0xFFB2BDD0), onSecondary = Color(0xFF182333),
    secondaryContainer = Color(0xFF203453), onSecondaryContainer = Color(0xFFADC8FF),
    background = Color(0xFF101216), surface = Color(0xFF101216), onSurface = Color(0xFFF2F3F7),
    surfaceVariant = Color(0xFF282C34), onSurfaceVariant = Color(0xFFADB3C0),
    outline = Color(0xFF838C9B), outlineVariant = Color(0xFF353A45),
    surfaceContainer = Color(0xFF1C1F26), surfaceContainerLow = Color(0xFF191C22),
    surfaceContainerHigh = Color(0xFF282C34), surfaceContainerHighest = Color(0xFF343943))

@Composable fun NeyraTheme(content: @Composable () -> Unit) {
    fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) =
        TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp)
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Night else Day,
        typography = Typography(
            headlineLarge = type(32, 38, FontWeight.Bold, -0.8f),
            titleLarge = type(22, 28, FontWeight.SemiBold, -0.4f),
            titleMedium = type(17, 23, FontWeight.SemiBold, -0.2f),
            titleSmall = type(15, 21, FontWeight.SemiBold),
            bodyLarge = type(16, 24), bodyMedium = type(14, 21), bodySmall = type(12, 18),
            labelLarge = type(15, 20, FontWeight.Medium), labelMedium = type(12, 16, FontWeight.Medium),
            labelSmall = type(11, 15, FontWeight.Medium, 0.1f)),
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)), content = content)
}

@Composable fun ConnectionPanel(state: CompanionState, onComputers: () -> Unit) {
    ContentGroup {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Outlined.Computer)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.selected?.name ?: "Choose a computer", style = MaterialTheme.typography.titleMedium)
                Text(if (state.selected == null) "Pair on your local network" else if (state.online) "Connected · local network" else "Offline · check Wi-Fi and Neyra",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onComputers, contentPadding = PaddingValues(0.dp)) {
            Text(if (state.selected == null) "Find computers" else "Change computer")
            Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp))
        }
    }
}
