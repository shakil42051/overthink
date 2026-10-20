package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences

val Forest = Color(0xFF234E43)
val Lime = Color(0xFFDCEBB6)
val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }

fun Modifier.semanticsLabel(label: String) = semantics { contentDescription = label }

val AppPreferences.gutter
    get() = if (density == "Compact") 16.dp else 24.dp
val AppPreferences.corner
    get() =
        when (corners) {
            "Crisp" -> 6.dp
            "Soft" -> 14.dp
            else -> 24.dp
        }
val AppPreferences.motionMillis
    get() =
        if (reduceMotion) 0
        else
            when (motionSpeed) {
                "Quick" -> 140
                "Relaxed" -> 420
                else -> 240
            }

fun accentColors(name: String): Pair<Color, Color> =
    when (name) {
        "Ocean" -> Color(0xFF155B78) to Color(0xFFD0EBF8)
        "Indigo" -> Color(0xFF49469A) to Color(0xFFE4E0FF)
        "Rose" -> Color(0xFF8C3C58) to Color(0xFFFFDCE6)
        "Amber" -> Color(0xFF765300) to Color(0xFFFFE5AA)
        else -> Forest to Lime
    }

private val Light =
    lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        primaryContainer = Lime,
        onPrimaryContainer = Color(0xFF18362E),
        secondary = Color(0xFF605E53),
        secondaryContainer = Color(0xFFE0E8D9),
        onSecondaryContainer = Forest,
        background = Color(0xFFF5F4F0),
        onBackground = Color(0xFF252D28),
        surface = Color(0xFFFFFEFA),
        surfaceVariant = Color(0xFFEBEAE3),
        onSurface = Color(0xFF252D28),
        onSurfaceVariant = Color(0xFF5D665F),
        outline = Color(0xFF848B80),
        outlineVariant = Color(0xFFDADDD2),
    )
private val Dark =
    darkColorScheme(
        primary = Lime,
        onPrimary = Color(0xFF17382E),
        primaryContainer = Forest,
        onPrimaryContainer = Lime,
        secondaryContainer = Forest,
        onSecondaryContainer = Lime,
        background = Color(0xFF151D19),
        onBackground = Color(0xFFEDF0E7),
        surface = Color(0xFF1C2721),
        surfaceVariant = Color(0xFF2A362E),
        onSurface = Color(0xFFEDF0E7),
        onSurfaceVariant = Color(0xFFBAC5B9),
        outlineVariant = Color(0xFF3E4C40),
    )

@Composable
fun OverthinkTheme(
    dark: Boolean,
    preferences: AppPreferences = AppPreferences(),
    content: @Composable () -> Unit,
) {
    val (accent, tint) = accentColors(preferences.accent)
    val scheme =
        if (dark)
            Dark.copy(
                primary = tint,
                onPrimary = accent,
                primaryContainer = accent,
                onPrimaryContainer = tint,
                secondaryContainer = accent,
                onSecondaryContainer = tint,
            )
        else
            Light.copy(
                primary = accent,
                primaryContainer = tint,
                onPrimaryContainer = accent,
                secondaryContainer = tint,
                onSecondaryContainer = accent,
            )
    val heading =
        if (preferences.typography == "Editorial") FontFamily.Serif else FontFamily.SansSerif
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppPreferences provides preferences,
        LocalDensity provides Density(density.density, density.fontScale * preferences.fontScale),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes =
                Shapes(
                    small = RoundedCornerShape(preferences.corner / 2),
                    medium = RoundedCornerShape(preferences.corner),
                    large = RoundedCornerShape(preferences.corner),
                ),
            typography =
                Typography(
                    displayMedium =
                        TextStyle(
                            fontFamily = heading,
                            fontSize = 36.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1).sp,
                        ),
                    headlineLarge =
                        TextStyle(
                            fontFamily = heading,
                            fontSize = 30.sp,
                            lineHeight = 37.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    headlineMedium =
                        TextStyle(
                            fontFamily = heading,
                            fontSize = 24.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    titleLarge =
                        TextStyle(
                            fontSize = 21.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    titleMedium =
                        TextStyle(
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
                    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
                    labelLarge =
                        TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    labelSmall =
                        TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                ),
        ) {
            Surface(
                Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    }
}

@Composable
fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.8.sp,
    )
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        action?.invoke()
    }
}

@Composable
fun PaperSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalAppPreferences.current.corner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(
                if (LocalAppPreferences.current.density == "Compact") 14.dp else 20.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(
                    if (LocalAppPreferences.current.density == "Compact") 8.dp else 12.dp
                ),
            content = content,
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, action: (@Composable () -> Unit)? = null) {
    PaperSurface {
        Eyebrow("Room to think")
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun Tag(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ForwardIcon() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
}
