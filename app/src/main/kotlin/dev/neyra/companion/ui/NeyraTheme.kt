package dev.neyra.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neyra.companion.CompanionState
import dev.neyra.companion.R

@OptIn(ExperimentalTextApi::class)
private val Manrope = FontFamily(
    Font(R.font.manrope, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(450))),
    Font(R.font.manrope, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(650))))
private val Day = lightColorScheme(
    primary = Color(0xFF225542), onPrimary = Color(0xFFF5F8ED),
    primaryContainer = Color(0xFF153D31), onPrimaryContainer = Color(0xFFEAF2DA),
    secondary = Color(0xFF526348), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEC98), onSecondaryContainer = Color(0xFF243313),
    tertiary = Color(0xFF845137), background = Color(0xFFF4F5EC),
    surface = Color(0xFFF4F5EC), onSurface = Color(0xFF192E25),
    surfaceVariant = Color(0xFFE4E8DB), onSurfaceVariant = Color(0xFF465649),
    outline = Color(0xFF737F6D), outlineVariant = Color(0xFFC5CEBB),
    surfaceContainer = Color(0xFFE9EDDF), surfaceContainerLow = Color(0xFFEEF1E5),
    surfaceContainerHigh = Color(0xFFE1E7D6), surfaceContainerHighest = Color(0xFFDCE3D1))
private val Night = darkColorScheme(
    primary = Color(0xFFDCEC98), onPrimary = Color(0xFF243313),
    primaryContainer = Color(0xFF193F32), onPrimaryContainer = Color(0xFFEAF2DA),
    secondary = Color(0xFFBCCBA7), onSecondary = Color(0xFF26361D),
    secondaryContainer = Color(0xFF3B4F2C), onSecondaryContainer = Color(0xFFE0ECAF),
    background = Color(0xFF101F19), surface = Color(0xFF101F19),
    onSurface = Color(0xFFEEF2E3), surfaceVariant = Color(0xFF2E4034),
    onSurfaceVariant = Color(0xFFC1CCB8), outline = Color(0xFF8A9B83),
    outlineVariant = Color(0xFF435442), surfaceContainer = Color(0xFF1C3025),
    surfaceContainerLow = Color(0xFF16271E), surfaceContainerHigh = Color(0xFF263B2D),
    surfaceContainerHighest = Color(0xFF304535))

@Composable fun NeyraTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Night else Day,
        typography = base.copy(
            headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
            titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
            titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
            bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
            bodySmall = base.bodySmall.copy(fontFamily = Manrope),
            labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold)),
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)), content = content)
}

@Composable fun ConnectionPanel(state: CompanionState, onComputers: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(24.dp))
                val line = MaterialTheme.colorScheme.onPrimaryContainer
                Canvas(Modifier.weight(1f).height(24.dp).padding(horizontal = 12.dp)) {
                    val y = size.height / 2
                    if (state.online) drawLine(line, Offset(0f,y), Offset(size.width,y), 2.dp.toPx())
                    else { val step = 12.dp.toPx(); var x = 0f; while(x < size.width) { drawLine(line.copy(alpha = 0.6f),Offset(x,y),Offset(minOf(x+step/2,size.width),y), 1.dp.toPx()); x += step } }
                    drawCircle(line, 3.dp.toPx(), Offset(size.width,y))
                }
                Icon(Icons.Outlined.Computer, null, Modifier.size(24.dp))
            }
            Text(state.selected?.name ?: "Choose your computer", style = MaterialTheme.typography.titleLarge)
            Text(if (state.selected == null) "Pair once on your local network to get started." else if (state.online) "Connected on your local network" else "Not connected · check Wi-Fi and desktop Neyra", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onComputers, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer), contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(if (state.selected == null) "Find computers" else "Change computer")
                Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}
