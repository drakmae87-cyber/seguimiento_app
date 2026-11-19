package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.EyeColorPreset
import com.example.model.HudColorTheme

@Immutable
data class NekoAccentPalette(
    val primaryGlow: Color,
    val secondaryGlow: Color,
    val highlightPop: Color,
    val stageBackdropTop: Color,
    val stageBackdropBottom: Color
)

val LocalNekoPalette = staticCompositionLocalOf {
    NekoAccentPalette(
        primaryGlow = HudCyan,
        secondaryGlow = BioEmerald,
        highlightPop = StellarAmber,
        stageBackdropTop = Color(0xFF071A2E),
        stageBackdropBottom = StageVoid
    )
}

fun HudColorTheme.toAccentPalette(): NekoAccentPalette = when (this) {
    HudColorTheme.INTERSTELLAR -> NekoAccentPalette(
        primaryGlow = HudCyan,
        secondaryGlow = BioEmerald,
        highlightPop = StellarAmber,
        stageBackdropTop = Color(0xFF071B30),
        stageBackdropBottom = Color(0xFF02050D)
    )
    HudColorTheme.PLASMA_NEON -> NekoAccentPalette(
        primaryGlow = NeonMagenta,
        secondaryGlow = HudCyan,
        highlightPop = PlasmaViolet,
        stageBackdropTop = Color(0xFF1B0A2E),
        stageBackdropBottom = Color(0xFF05030E)
    )
    HudColorTheme.COBALT_STREAM -> NekoAccentPalette(
        primaryGlow = SpaceCobalt,
        secondaryGlow = Color(0xFF5CE1FF),
        highlightPop = BioEmerald,
        stageBackdropTop = Color(0xFF05183D),
        stageBackdropBottom = Color(0xFF020714)
    )
    HudColorTheme.SOLAR_HORIZON -> NekoAccentPalette(
        primaryGlow = StellarAmber,
        secondaryGlow = HorizonCoral,
        highlightPop = Color(0xFFE6F4FF),
        stageBackdropTop = Color(0xFF211321),
        stageBackdropBottom = Color(0xFF06050B)
    )
}

fun EyeColorPreset.toColor(): Color = when (this) {
    EyeColorPreset.EMERALD -> BioEmerald
    EyeColorPreset.CYAN -> HudCyan
    EyeColorPreset.MAGENTA -> NeonMagenta
    EyeColorPreset.AMBER -> StellarAmber
}

fun parseHexColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val parsed = cleaned.toLongOrNull(16) ?: return null
    val argb = if (cleaned.length == 6) (0xFF000000L or parsed) else parsed
    return Color(argb.toInt())
}

fun colorToHexString(color: Color): String {
    val rgb = color.toArgb() and 0x00FFFFFF
    return String.format("#%06X", rgb)
}

fun resolveEyeColor(preset: EyeColorPreset, customHex: String?): Color {
    return parseHexColorOrNull(customHex) ?: preset.toColor()
}

@Composable
fun NekoBeatTheme(
    colorTheme: HudColorTheme = HudColorTheme.INTERSTELLAR,
    customPrimaryHex: String? = null,
    customSecondaryHex: String? = null,
    content: @Composable () -> Unit
) {
    val basePalette = colorTheme.toAccentPalette()
    val palette = basePalette.copy(
        primaryGlow = parseHexColorOrNull(customPrimaryHex) ?: basePalette.primaryGlow,
        secondaryGlow = parseHexColorOrNull(customSecondaryHex) ?: basePalette.secondaryGlow
    )
    val colorScheme = darkColorScheme(
        primary = palette.primaryGlow,
        onPrimary = Color(0xFF030812),
        primaryContainer = palette.primaryGlow.copy(alpha = 0.22f),
        onPrimaryContainer = InkCream,
        secondary = palette.secondaryGlow,
        onSecondary = Color(0xFF031410),
        secondaryContainer = palette.secondaryGlow.copy(alpha = 0.20f),
        onSecondaryContainer = InkCream,
        tertiary = palette.highlightPop,
        onTertiary = Color(0xFF050810),
        background = palette.stageBackdropBottom,
        onBackground = InkCream,
        surface = StageSurface,
        onSurface = InkCream,
        surfaceVariant = StageCard,
        onSurfaceVariant = InkMuted,
        outline = StageBorder
    )

    CompositionLocalProvider(LocalNekoPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NekoBeatTypography,
            content = content
        )
    }
}
