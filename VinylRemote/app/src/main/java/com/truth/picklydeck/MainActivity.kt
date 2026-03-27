package com.truth.picklydeck

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private enum class DeckThemeOption {
    AUTO,
    SILVER,
    BLACK
}

private enum class DeckVisualOption {
    TURNTABLE,
    CAMPFIRE,
    CD_PLAYER
}

private enum class VisualizerOption {
    OFF,
    WAVE,
    SPECTRUM
}

private data class DeckPalette(
    val pageGradient: List<Color>,
    val cardColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val bodyColor: Color,
    val helperText: Color,
    val deckGradient: List<Color>,
    val deckBorder: Color,
    val deckGlow: Color,
    val screwColor: Color,
    val screwCut: Color,
    val platterGradient: List<Color>,
    val strobeA: Color,
    val strobeB: Color,
    val grooveColor: Color,
    val glossColor: Color,
    val labelRing: Color,
    val labelBg: Color,
    val labelFallbackStart: Color,
    val labelFallbackEnd: Color,
    val spindle: Color,
    val controlTint: Color,
    val controlButtonBg: Color,
    val controlButtonBorder: Color,
    val seekText: Color,
    val armBase: Color,
    val armShadow: Color,
    val armHighlight: Color,
    val pivotOuter: Color,
    val pivotInner: Color,
    val cartridgeBody: Color,
    val cartridgeShadow: Color,
    val cartridgeHighlight: Color,
    val stylus: Color,
    val stylusGlow: Color
)

private fun paletteFor(option: DeckThemeOption): DeckPalette {
    return when (option) {
        DeckThemeOption.AUTO -> minimalWhitePalette()
        DeckThemeOption.SILVER -> minimalWhitePalette()
        DeckThemeOption.BLACK -> DeckPalette(
            pageGradient = listOf(Color(0xFF111315), Color(0xFF1B1F25), Color(0xFF0B0D11)),
            cardColor = Color(0x6612151A),
            titleColor = Color(0xFFE5F2FF),
            subtitleColor = Color(0xFFAFC4D6),
            bodyColor = Color(0xFFDDE8F2),
            helperText = Color(0xFFA3B4C3),
            deckGradient = listOf(Color(0xFF0F1216), Color(0xFF1A1F26), Color(0xFF0A0C10)),
            deckBorder = Color(0x80516479),
            deckGlow = Color(0x2B2A4A66),
            screwColor = Color(0xFF5C6978),
            screwCut = Color(0x66222C38),
            platterGradient = listOf(Color(0xFF454A54), Color(0xFF1B1E23), Color(0xFF08090C)),
            strobeA = Color(0xFFAAB7C9),
            strobeB = Color(0xFF687386),
            grooveColor = Color(0xFF222730),
            glossColor = Color(0x2FE8F2FF),
            labelRing = Color(0xB394B6D9),
            labelBg = Color(0xFF1C2530),
            labelFallbackStart = Color(0xFF4A78A6),
            labelFallbackEnd = Color(0xFF253B57),
            spindle = Color(0xFFD8E4F0),
            controlTint = Color(0xFFD8E8F8),
            controlButtonBg = Color(0xFF16202B),
            controlButtonBorder = Color(0xFF4F708E),
            seekText = Color(0xFFD5E7FA),
            armBase = Color(0xFFD5DBE5),
            armShadow = Color(0x99434856),
            armHighlight = Color(0xFFF2F9FF),
            pivotOuter = Color(0xFF6E8196),
            pivotInner = Color(0xFF1C232D),
            cartridgeBody = Color(0xFF7E8E9F),
            cartridgeShadow = Color(0x8A10151C),
            cartridgeHighlight = Color(0xFFC7D5E4),
            stylus = Color(0xFF161A1F),
            stylusGlow = Color(0x334CC8FF)
        )
    }
}

private fun minimalWhitePalette(): DeckPalette {
    return DeckPalette(
        pageGradient = listOf(Color(0xFFF1F2F4), Color(0xFFE7E9ED), Color(0xFFEDEEF1)),
        cardColor = Color(0xF7FFFFFF),
        titleColor = Color(0xFF1A1C1F),
        subtitleColor = Color(0xFF5D6670),
        bodyColor = Color(0xFF1A1C1F),
        helperText = Color(0xFF64707C),
        deckGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF4F6F8), Color(0xFFECEEF2)),
        deckBorder = Color(0xFFD2D7DD),
        deckGlow = Color(0x20A0A7B3),
        screwColor = Color(0xFFC4CBD3),
        screwCut = Color(0x6676808C),
        platterGradient = listOf(Color(0xFF2E3136), Color(0xFF17191C), Color(0xFF0A0B0C)),
        strobeA = Color(0xFFBEC5CE),
        strobeB = Color(0xFF808A95),
        grooveColor = Color(0xFF212327),
        glossColor = Color(0x3DF7FAFF),
        labelRing = Color(0x99D7DDE4),
        labelBg = Color(0xFF2B2F34),
        labelFallbackStart = Color(0xFF3E454D),
        labelFallbackEnd = Color(0xFF1F2328),
        spindle = Color(0xFFE4E8ED),
        controlTint = Color(0xFF2B3138),
        controlButtonBg = Color(0xFFF5F7F9),
        controlButtonBorder = Color(0xFFC6CED8),
        seekText = Color(0xFF3F4952),
        armBase = Color(0xFFD8DEE5),
        armShadow = Color(0x80535D68),
        armHighlight = Color(0xFFF7FBFF),
        pivotOuter = Color(0xFFC7CED7),
        pivotInner = Color(0xFF6A737E),
        cartridgeBody = Color(0xFFC7CED5),
        cartridgeShadow = Color(0x70363C43),
        cartridgeHighlight = Color(0xFFF3F8FF),
        stylus = Color(0xFF2A2E33),
        stylusGlow = Color(0x332B3138)
    )
}

private fun mixColor(a: Color, b: Color, t: Float): Color {
    val p = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * p,
        green = a.green + (b.green - a.green) * p,
        blue = a.blue + (b.blue - a.blue) * p,
        alpha = a.alpha + (b.alpha - a.alpha) * p
    )
}

private fun extractAlbumAccentColor(albumArt: Bitmap?): Color? {
    if (albumArt == null || albumArt.width <= 0 || albumArt.height <= 0) return null
    return try {
        val sample = Bitmap.createScaledBitmap(albumArt, 72, 72, true)
        val bins = IntArray(6 * 6 * 6)
        val hsv = FloatArray(3)
        var bestScore = -1f
        var bestIdx = -1
        var avgR = 0f
        var avgG = 0f
        var avgB = 0f
        var avgW = 0f

        fun binIndex(rBin: Int, gBin: Int, bBin: Int): Int = rBin * 36 + gBin * 6 + bBin

        for (y in 0 until sample.height step 2) {
            for (x in 0 until sample.width step 2) {
                val argb = sample.getPixel(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha < 32) continue

                val r = ((argb ushr 16) and 0xFF) / 255f
                val g = ((argb ushr 8) and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f
                android.graphics.Color.RGBToHSV(
                    ((r * 255f).toInt().coerceIn(0, 255)),
                    ((g * 255f).toInt().coerceIn(0, 255)),
                    ((b * 255f).toInt().coerceIn(0, 255)),
                    hsv
                )

                if (hsv[2] < 0.09f) continue
                val sat = hsv[1]
                val value = hsv[2]
                val weight = (0.42f + sat * 1.35f + value * 0.5f).coerceAtLeast(0.1f)

                avgR += r * weight
                avgG += g * weight
                avgB += b * weight
                avgW += weight

                val rBin = (r * 5f).toInt().coerceIn(0, 5)
                val gBin = (g * 5f).toInt().coerceIn(0, 5)
                val bBin = (b * 5f).toInt().coerceIn(0, 5)
                val idx = binIndex(rBin, gBin, bBin)
                bins[idx] += (weight * 100f).toInt().coerceAtLeast(1)
                val score = bins[idx].toFloat() * (0.84f + sat * 0.5f)
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = idx
                }
            }
        }

        val raw = if (bestIdx >= 0) {
            val rBin = bestIdx / 36
            val gBin = (bestIdx % 36) / 6
            val bBin = bestIdx % 6
            Color(
                red = ((rBin + 0.5f) / 6f).coerceIn(0f, 1f),
                green = ((gBin + 0.5f) / 6f).coerceIn(0f, 1f),
                blue = ((bBin + 0.5f) / 6f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else if (avgW > 0.0001f) {
            Color(
                red = (avgR / avgW).coerceIn(0f, 1f),
                green = (avgG / avgW).coerceIn(0f, 1f),
                blue = (avgB / avgW).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            null
        } ?: return null

        val hsvOut = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (raw.red * 255f).toInt().coerceIn(0, 255),
            (raw.green * 255f).toInt().coerceIn(0, 255),
            (raw.blue * 255f).toInt().coerceIn(0, 255),
            hsvOut
        )
        hsvOut[1] = (hsvOut[1] * 1.18f).coerceIn(0.24f, 0.88f)
        hsvOut[2] = hsvOut[2].coerceIn(0.5f, 0.95f)
        Color(android.graphics.Color.HSVToColor(hsvOut))
    } catch (_: Throwable) {
        null
    }
}

private fun autoPaletteForAlbumArt(albumArt: Bitmap?): DeckPalette {
    val base = paletteFor(DeckThemeOption.SILVER)
    val accent = extractAlbumAccentColor(albumArt) ?: Color(0xFF6FA8FF)
    val accentSoft = mixColor(accent, Color.White, 0.62f)
    val accentMid = mixColor(accent, Color.White, 0.38f)
    val accentDeep = mixColor(accent, Color.Black, 0.42f)

    return base.copy(
        pageGradient = listOf(
            mixColor(Color(0xFFF6FAFF), accentSoft, 0.5f),
            mixColor(Color(0xFFEAF0F8), accentSoft, 0.44f),
            mixColor(Color(0xFFDDE7F3), accentMid, 0.4f)
        ),
        cardColor = mixColor(Color.White, accentSoft, 0.22f).copy(alpha = 0.84f),
        titleColor = mixColor(Color(0xFF1B2531), accentDeep, 0.2f),
        subtitleColor = mixColor(Color(0xFF4C5F73), accentDeep, 0.18f),
        bodyColor = mixColor(Color(0xFF1D2835), accentDeep, 0.16f),
        helperText = mixColor(Color(0xFF5F7184), accentDeep, 0.15f),
        deckGradient = listOf(
            mixColor(Color(0xFFFFFFFF), accentSoft, 0.18f),
            mixColor(Color(0xFFF2F6FB), accentSoft, 0.24f),
            mixColor(Color(0xFFE5ECF4), accentMid, 0.28f)
        ),
        deckBorder = mixColor(Color(0xFFB2BFCE), accentMid, 0.32f),
        deckGlow = accent.copy(alpha = 0.14f),
        screwColor = mixColor(Color(0xFFB3BECB), accentMid, 0.22f),
        screwCut = Color(0x99414855),
        platterGradient = listOf(
            mixColor(Color(0xFF4A525E), accentSoft, 0.14f),
            mixColor(Color(0xFF1A1F26), accentDeep, 0.2f),
            Color(0xFF07090C)
        ),
        strobeA = mixColor(Color(0xFFBEC8D4), accentSoft, 0.3f),
        strobeB = mixColor(Color(0xFF7C8794), accentDeep, 0.24f),
        grooveColor = mixColor(Color(0xFF21262D), accentDeep, 0.22f),
        glossColor = accentSoft.copy(alpha = 0.2f),
        labelRing = accentMid.copy(alpha = 0.56f),
        labelBg = mixColor(Color(0xFF202934), accentDeep, 0.26f),
        labelFallbackStart = mixColor(accent, Color.White, 0.16f),
        labelFallbackEnd = mixColor(accentDeep, Color.Black, 0.2f),
        spindle = mixColor(Color(0xFFE2E9F1), accentSoft, 0.14f),
        controlTint = mixColor(Color(0xFF2D3744), accentDeep, 0.2f),
        controlButtonBg = mixColor(Color.White, accentSoft, 0.18f),
        controlButtonBorder = mixColor(Color(0xFFB0BECE), accentMid, 0.3f),
        seekText = mixColor(Color(0xFF455566), accentDeep, 0.12f),
        armBase = mixColor(Color(0xFFDDE4EB), accentSoft, 0.1f),
        armShadow = mixColor(Color(0xFF4E5866), accentDeep, 0.34f).copy(alpha = 0.56f),
        armHighlight = mixColor(Color(0xFFFBFDFF), accentSoft, 0.18f),
        pivotOuter = mixColor(Color(0xFFB7C3D1), accentMid, 0.22f),
        pivotInner = mixColor(Color(0xFF636F7C), accentDeep, 0.22f),
        cartridgeBody = mixColor(Color(0xFFC4CDD7), accentMid, 0.2f),
        cartridgeShadow = Color.Black.copy(alpha = 0.34f),
        cartridgeHighlight = mixColor(Color(0xFFF2F7FE), accentSoft, 0.2f),
        stylus = mixColor(Color(0xFF242A32), accentDeep, 0.2f),
        stylusGlow = accent.copy(alpha = 0.18f)
    )
}

private fun scaledDp(base: Dp, scale: Float, minDp: Dp): Dp {
    return max(base.value * scale, minDp.value).dp
}

private const val NEEDLE_UI_PLAY_START = 0.30f
private const val NEEDLE_UI_PLAY_END = 0.98f
// Angles tuned so start is at outer groove (right-lower edge), then moves inward.
private const val NEEDLE_UI_REST_ANGLE = -102f
private const val NEEDLE_UI_PLAY_START_ANGLE = -90f
private const val NEEDLE_UI_PLAY_END_ANGLE = -62f
private const val KEY_LIGHT_X = 0.22f
private const val KEY_LIGHT_Y = 0.16f
private const val PRIMARY_REFLECTION_START = 214f
private const val PRIMARY_REFLECTION_SWEEP = 56f
private const val SECONDARY_REFLECTION_START = 20f
private const val SECONDARY_REFLECTION_SWEEP = 30f
private const val UI_PREFS_NAME = "picklydeck_ui_prefs"
private const val UI_PREF_THEME_KEY = "selected_theme"
private const val UI_PREF_VISUAL_KEY = "selected_visual"
private const val UI_PREF_VISUALIZER_KEY = "selected_visualizer"

private data class MoonPhaseState(
    val illumination: Float,
    val waxing: Boolean,
    val phaseRatio: Float
)

private const val LUNAR_SYNODIC_MONTH_DAYS = 29.530588853
private const val REFERENCE_NEW_MOON_UTC_MS = 947182440000L // 2000-01-06T18:14:00Z

private fun currentMoonPhaseState(nowUtcMs: Long = System.currentTimeMillis()): MoonPhaseState {
    val dayMs = 86_400_000.0
    val daysFromReference = (nowUtcMs - REFERENCE_NEW_MOON_UTC_MS) / dayMs
    val phaseDays = ((daysFromReference % LUNAR_SYNODIC_MONTH_DAYS) + LUNAR_SYNODIC_MONTH_DAYS) %
        LUNAR_SYNODIC_MONTH_DAYS
    val phaseRatio = (phaseDays / LUNAR_SYNODIC_MONTH_DAYS).toFloat().coerceIn(0f, 1f)
    val illumination = ((1.0 - cos(2.0 * PI * phaseRatio.toDouble())) / 2.0)
        .toFloat()
        .coerceIn(0f, 1f)
    return MoonPhaseState(
        illumination = illumination,
        waxing = phaseRatio < 0.5f,
        phaseRatio = phaseRatio
    )
}

private fun progressToNeedleAngle(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p <= NEEDLE_UI_PLAY_START) {
        val t = (p / NEEDLE_UI_PLAY_START).coerceIn(0f, 1f)
        NEEDLE_UI_REST_ANGLE + (NEEDLE_UI_PLAY_START_ANGLE - NEEDLE_UI_REST_ANGLE) * t
    } else {
        val t = ((p - NEEDLE_UI_PLAY_START) / (NEEDLE_UI_PLAY_END - NEEDLE_UI_PLAY_START))
            .coerceIn(0f, 1f)
        NEEDLE_UI_PLAY_START_ANGLE + (NEEDLE_UI_PLAY_END_ANGLE - NEEDLE_UI_PLAY_START_ANGLE) * t
    }
}

private fun angleToNeedleProgress(angle: Float): Float {
    val a = angle.coerceIn(NEEDLE_UI_REST_ANGLE, NEEDLE_UI_PLAY_END_ANGLE)
    return if (a <= NEEDLE_UI_PLAY_START_ANGLE) {
        val t = ((a - NEEDLE_UI_REST_ANGLE) / (NEEDLE_UI_PLAY_START_ANGLE - NEEDLE_UI_REST_ANGLE))
            .coerceIn(0f, 1f)
        NEEDLE_UI_PLAY_START * t
    } else {
        val t = ((a - NEEDLE_UI_PLAY_START_ANGLE) / (NEEDLE_UI_PLAY_END_ANGLE - NEEDLE_UI_PLAY_START_ANGLE))
            .coerceIn(0f, 1f)
        NEEDLE_UI_PLAY_START + (NEEDLE_UI_PLAY_END - NEEDLE_UI_PLAY_START) * t
    }
}

private fun keyLight(size: Size): Offset {
    return Offset(size.width * KEY_LIGHT_X, size.height * KEY_LIGHT_Y)
}

private fun reflectionArcTopLeft(center: Offset, radius: Float): Offset {
    return Offset(center.x - radius, center.y - radius)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDualReflections(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    strokeWidth: Float
) {
    val arcTopLeft = reflectionArcTopLeft(center, radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    drawArc(
        color = primaryColor,
        startAngle = PRIMARY_REFLECTION_START,
        sweepAngle = PRIMARY_REFLECTION_SWEEP,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    drawArc(
        color = secondaryColor,
        startAngle = SECONDARY_REFLECTION_START,
        sweepAngle = SECONDARY_REFLECTION_SWEEP,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
    )
}

private enum class LayoutPresetType {
    FLIP_COVER,
    FLIP_OPEN,
    PHONE_COMPACT,
    PHONE_STANDARD,
    LANDSCAPE,
    TABLET
}

private data class LayoutPreset(
    val type: LayoutPresetType,
    val uiScale: Float,
    val wideLayout: Boolean,
    val hideControlPanelForShortScreen: Boolean,
    val outerPaddingBase: Dp,
    val sectionGapBase: Dp,
    val deckWidthShort: Float,
    val deckWidthUltra: Float,
    val deckWidthCompact: Float,
    val deckWidthRegular: Float,
    val deckHeightUltraFactor: Float,
    val deckHeightCompactFactor: Float,
    val deckHeightRegularFactor: Float
)

private fun resolveLayoutPreset(maxWidth: Dp, maxHeight: Dp): LayoutPreset {
    val w = maxWidth.value
    val h = maxHeight.value
    val ratio = h / w.coerceAtLeast(1f)
    val isLandscape = w > h

    return when {
        maxWidth >= 900.dp || (maxWidth >= 760.dp && maxHeight >= 520.dp) -> LayoutPreset(
            type = LayoutPresetType.TABLET,
            uiScale = 1f,
            wideLayout = true,
            hideControlPanelForShortScreen = false,
            outerPaddingBase = 18.dp,
            sectionGapBase = 14.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.94f,
            deckWidthCompact = 0.97f,
            deckWidthRegular = 1f,
            deckHeightUltraFactor = 0.58f,
            deckHeightCompactFactor = 0.66f,
            deckHeightRegularFactor = 0.72f
        )
        isLandscape && maxWidth >= 620.dp -> LayoutPreset(
            type = LayoutPresetType.LANDSCAPE,
            uiScale = 0.95f,
            wideLayout = true,
            hideControlPanelForShortScreen = false,
            outerPaddingBase = 14.dp,
            sectionGapBase = 10.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.92f,
            deckWidthCompact = 0.95f,
            deckWidthRegular = 0.98f,
            deckHeightUltraFactor = 0.56f,
            deckHeightCompactFactor = 0.63f,
            deckHeightRegularFactor = 0.7f
        )
        ratio >= 2.08f && maxWidth < 350.dp -> LayoutPreset(
            type = LayoutPresetType.FLIP_COVER,
            uiScale = 0.82f,
            wideLayout = false,
            hideControlPanelForShortScreen = true,
            outerPaddingBase = 8.dp,
            sectionGapBase = 8.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.92f,
            deckWidthCompact = 0.96f,
            deckWidthRegular = 0.98f,
            deckHeightUltraFactor = 0.58f,
            deckHeightCompactFactor = 0.64f,
            deckHeightRegularFactor = 0.7f
        )
        ratio >= 2.0f && maxWidth < 460.dp -> LayoutPreset(
            type = LayoutPresetType.FLIP_OPEN,
            uiScale = 0.9f,
            wideLayout = false,
            hideControlPanelForShortScreen = false,
            outerPaddingBase = 10.dp,
            sectionGapBase = 9.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.93f,
            deckWidthCompact = 0.96f,
            deckWidthRegular = 0.98f,
            deckHeightUltraFactor = 0.58f,
            deckHeightCompactFactor = 0.64f,
            deckHeightRegularFactor = 0.72f
        )
        maxWidth < 360.dp || maxHeight < 700.dp -> LayoutPreset(
            type = LayoutPresetType.PHONE_COMPACT,
            uiScale = 0.9f,
            wideLayout = false,
            hideControlPanelForShortScreen = false,
            outerPaddingBase = 12.dp,
            sectionGapBase = 10.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.9f,
            deckWidthCompact = 0.94f,
            deckWidthRegular = 0.96f,
            deckHeightUltraFactor = 0.54f,
            deckHeightCompactFactor = 0.6f,
            deckHeightRegularFactor = 0.67f
        )
        else -> LayoutPreset(
            type = LayoutPresetType.PHONE_STANDARD,
            uiScale = 1f,
            wideLayout = false,
            hideControlPanelForShortScreen = false,
            outerPaddingBase = 16.dp,
            sectionGapBase = 12.dp,
            deckWidthShort = 1f,
            deckWidthUltra = 0.92f,
            deckWidthCompact = 0.95f,
            deckWidthRegular = 0.98f,
            deckHeightUltraFactor = 0.56f,
            deckHeightCompactFactor = 0.62f,
            deckHeightRegularFactor = 0.69f
        )
    }
}

@Composable
private fun BackgroundMaterialTexture(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val diagStep = size.minDimension * 0.08f
        val diagCount = (size.width / diagStep).toInt() + 16
        for (i in -8..diagCount) {
            val x = i * diagStep - size.height * 0.35f
            drawLine(
                color = Color.White.copy(alpha = 0.02f),
                start = Offset(x, 0f),
                end = Offset(x + size.height * 0.9f, size.height),
                strokeWidth = 1f
            )
        }

        val diagStepB = size.minDimension * 0.11f
        val diagCountB = (size.width / diagStepB).toInt() + 16
        for (i in -8..diagCountB) {
            val x = i * diagStepB - size.height * 0.2f
            drawLine(
                color = Color.Black.copy(alpha = 0.014f),
                start = Offset(x, 0f),
                end = Offset(x + size.height * 0.75f, size.height),
                strokeWidth = 1f
            )
        }

        for (i in 0 until 260) {
            val px = ((i * 37) % 997) / 997f * size.width
            val py = ((i * 73 + 29) % 991) / 991f * size.height
            val r = ((i * 17) % 5 + 1) * 0.23f
            drawCircle(
                color = if (i % 3 == 0) {
                    Color.White.copy(alpha = 0.038f)
                } else {
                    Color.Black.copy(alpha = 0.02f)
                },
                radius = r,
                center = Offset(px, py)
            )
        }
    }
}

private fun performNeedleDropVibration(context: Context, strong: Boolean) {
    try {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(
                if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK
            )
        } else {
            VibrationEffect.createOneShot(
                if (strong) 24L else 12L,
                if (strong) 190 else 120
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            val vibrator = manager?.defaultVibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(effect)
            }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect)
            }
        }
    } catch (_: Throwable) {
        // Ignore device-specific vibration errors.
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PicklyDeckScreen()
                }
            }
        }
    }
}

@Composable
private fun PicklyDeckScreen(vm: PicklyDeckViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scratchEngine = remember { ScratchSoundEngine(context) }
    val uiPrefs = remember(context) {
        context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(context.hasPostNotificationsPermission())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
    var selectedTheme by rememberSaveable {
        mutableStateOf(parseThemeOption(uiPrefs.getString(UI_PREF_THEME_KEY, DeckThemeOption.AUTO.name)))
    }
    var selectedVisual by rememberSaveable {
        mutableStateOf(parseVisualOption(uiPrefs.getString(UI_PREF_VISUAL_KEY, DeckVisualOption.TURNTABLE.name)))
    }
    var selectedVisualizer by rememberSaveable {
        mutableStateOf(
            parseVisualizerOption(
                uiPrefs.getString(UI_PREF_VISUALIZER_KEY, VisualizerOption.OFF.name)
            )
        )
    }
    val palette = remember(selectedTheme, state.albumArt) {
        if (selectedTheme == DeckThemeOption.AUTO) {
            autoPaletteForAlbumArt(state.albumArt)
        } else {
            paletteFor(selectedTheme)
        }
    }

    DisposableEffect(Unit) {
        onDispose { scratchEngine.release() }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = context.hasPostNotificationsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(selectedTheme) {
        uiPrefs.edit().putString(UI_PREF_THEME_KEY, selectedTheme.name).apply()
    }
    LaunchedEffect(selectedVisual) {
        uiPrefs.edit().putString(UI_PREF_VISUAL_KEY, selectedVisual.name).apply()
    }
    LaunchedEffect(selectedVisualizer) {
        uiPrefs.edit().putString(UI_PREF_VISUALIZER_KEY, selectedVisualizer.name).apply()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.linearGradient(
                    colors = palette.pageGradient,
                    start = Offset.Zero,
                    end = Offset(900f, 1700f)
                )
            )
    ) {
        if (selectedTheme == DeckThemeOption.AUTO && state.albumArt != null) {
            Image(
                bitmap = state.albumArt!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.47f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color(0x99EEF3FA),
                                Color(0xCCE4EAF4)
                            )
                        )
                    )
            )
        }

        val availableMaxHeight = maxHeight
        val preset = resolveLayoutPreset(maxWidth = maxWidth, maxHeight = maxHeight)
        val isFlipCoverPreset = preset.type == LayoutPresetType.FLIP_COVER
        val isLandscape = maxWidth > maxHeight
        val compactWidth = maxWidth < 360.dp
        val compactHeight = maxHeight < 700.dp
        val ultraCompact = maxWidth < 320.dp || maxHeight < 560.dp
        val wideLayout = preset.wideLayout || (isLandscape && maxWidth >= 620.dp)
        val hideControlPanelForShortScreen = preset.hideControlPanelForShortScreen || (compactHeight && !wideLayout)
        val uiScale = if (isLandscape && compactHeight) {
            min(preset.uiScale, 0.9f)
        } else {
            preset.uiScale
        }
        val outerPadding = scaledDp(preset.outerPaddingBase, uiScale, 8.dp)
        val sectionGap = scaledDp(preset.sectionGapBase, uiScale, 8.dp)
        val portraitDeckMaxHeight = if (hideControlPanelForShortScreen) {
            availableMaxHeight - outerPadding * 2f
        } else {
            when {
                ultraCompact -> availableMaxHeight * preset.deckHeightUltraFactor
                compactHeight -> availableMaxHeight * preset.deckHeightCompactFactor
                else -> availableMaxHeight * preset.deckHeightRegularFactor
            }
        }
        val portraitDeckWidthFraction = when {
            hideControlPanelForShortScreen -> preset.deckWidthShort
            ultraCompact -> preset.deckWidthUltra
            compactWidth || compactHeight -> preset.deckWidthCompact
            else -> preset.deckWidthRegular
        }
        val contentWidth = maxWidth - outerPadding * 2f
        val portraitDeckSide = minOf(
            contentWidth * portraitDeckWidthFraction,
            portraitDeckMaxHeight
        )

        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
                horizontalArrangement = Arrangement.spacedBy(sectionGap)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.43f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(sectionGap)
                ) {
                    NowPlayingPanel(
                        state = state,
                        palette = palette,
                        uiScale = uiScale
                    )
                    ControlPanel(
                        state = state,
                        palette = palette,
                        uiScale = uiScale,
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it },
                        selectedVisual = selectedVisual,
                        onVisualSelected = { selectedVisual = it },
                        selectedVisualizer = selectedVisualizer,
                        onVisualizerSelected = { selectedVisualizer = it },
                        hasNotificationPermission = hasNotificationPermission,
                        onOpenSettings = vm::openNotificationAccessSettings,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenNotificationPermissionSettings = context::openAppNotificationSettings,
                        onPrev = vm::skipPrevious,
                        onPlayPause = vm::togglePlayPause,
                        onNext = vm::skipNext,
                        onSeekPreview = vm::previewSeekFraction,
                        onSeekCommit = vm::seekToFraction
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    TurntableDeck(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(maxHeight = availableMaxHeight - outerPadding * 2f),
                        isPlaying = state.isPlaying,
                        playbackSpeed = state.playbackSpeed,
                        needleProgress = state.needleProgress,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        albumArt = state.albumArt,
                        palette = palette,
                        themeOption = selectedTheme,
                        visualOption = selectedVisual,
                        visualizerOption = selectedVisualizer,
                        trackTitle = state.title,
                        trackArtist = state.artist,
                        uiScale = uiScale,
                        onNeedleProgress = vm::setNeedleProgress,
                        onNeedleRelease = vm::snapNeedleAndControlPlayback,
                        onNeedleScratch = scratchEngine::playScrub,
                        onPrev = vm::skipPrevious,
                        onPlayPause = vm::togglePlayPause,
                        onNext = vm::skipNext
                    )
                }
            }
        } else if (hideControlPanelForShortScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
            ) {
                TurntableDeck(
                    modifier = Modifier
                        .size(portraitDeckSide)
                        .align(Alignment.Center),
                    isPlaying = state.isPlaying,
                    playbackSpeed = state.playbackSpeed,
                    needleProgress = state.needleProgress,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    albumArt = state.albumArt,
                    palette = palette,
                    themeOption = selectedTheme,
                    visualOption = selectedVisual,
                    visualizerOption = selectedVisualizer,
                    trackTitle = state.title,
                    trackArtist = state.artist,
                    uiScale = uiScale,
                    onNeedleProgress = vm::setNeedleProgress,
                    onNeedleRelease = vm::snapNeedleAndControlPlayback,
                    onNeedleScratch = scratchEngine::playScrub,
                    onPrev = vm::skipPrevious,
                    onPlayPause = vm::togglePlayPause,
                    onNext = vm::skipNext
                )
                if (isFlipCoverPreset) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(scaledDp(16.dp, uiScale, 12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = palette.cardColor.copy(alpha = 0.92f)
                        )
                    ) {
                        PlaybackControls(
                            isPlaying = state.isPlaying,
                            enabled = state.hasNotificationAccess,
                            palette = palette,
                            uiScale = uiScale * 0.9f,
                            onPrev = vm::skipPrevious,
                            onPlayPause = vm::togglePlayPause,
                            onNext = vm::skipNext
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(outerPadding),
                verticalArrangement = Arrangement.spacedBy(sectionGap)
            ) {
                TurntableDeck(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(portraitDeckSide),
                    isPlaying = state.isPlaying,
                    playbackSpeed = state.playbackSpeed,
                    needleProgress = state.needleProgress,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    albumArt = state.albumArt,
                    palette = palette,
                    themeOption = selectedTheme,
                    visualOption = selectedVisual,
                    visualizerOption = selectedVisualizer,
                    trackTitle = state.title,
                    trackArtist = state.artist,
                    uiScale = uiScale,
                    onNeedleProgress = vm::setNeedleProgress,
                    onNeedleRelease = vm::snapNeedleAndControlPlayback,
                    onNeedleScratch = scratchEngine::playScrub,
                    onPrev = vm::skipPrevious,
                    onPlayPause = vm::togglePlayPause,
                    onNext = vm::skipNext
                )

                NowPlayingPanel(
                    state = state,
                    palette = palette,
                    uiScale = uiScale
                )

                ControlPanel(
                    state = state,
                    palette = palette,
                    uiScale = uiScale,
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it },
                    selectedVisual = selectedVisual,
                    onVisualSelected = { selectedVisual = it },
                    selectedVisualizer = selectedVisualizer,
                    onVisualizerSelected = { selectedVisualizer = it },
                    hasNotificationPermission = hasNotificationPermission,
                    onOpenSettings = vm::openNotificationAccessSettings,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenNotificationPermissionSettings = context::openAppNotificationSettings,
                    onPrev = vm::skipPrevious,
                    onPlayPause = vm::togglePlayPause,
                    onNext = vm::skipNext,
                    onSeekPreview = vm::previewSeekFraction,
                    onSeekCommit = vm::seekToFraction
                )
            }
        }
    }
}

@Composable
private fun NowPlayingPanel(
    state: PicklyDeckUiState,
    palette: DeckPalette,
    uiScale: Float
) {
    val appName = state.connectedPackage?.let(::toReadableAppName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scaledDp(18.dp, uiScale, 12.dp)),
        colors = CardDefaults.cardColors(containerColor = palette.cardColor)
    ) {
        Column(
            modifier = Modifier
                .padding(scaledDp(14.dp, uiScale, 10.dp)),
            verticalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.titleColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (state.artist.isNotBlank() || appName != null) {
                Text(
                    text = listOfNotNull(
                        state.artist.takeIf { it.isNotBlank() },
                        appName
                    ).joinToString("  |  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ControlPanel(
    state: PicklyDeckUiState,
    palette: DeckPalette,
    uiScale: Float,
    selectedTheme: DeckThemeOption,
    onThemeSelected: (DeckThemeOption) -> Unit,
    selectedVisual: DeckVisualOption,
    onVisualSelected: (DeckVisualOption) -> Unit,
    selectedVisualizer: VisualizerOption,
    onVisualizerSelected: (VisualizerOption) -> Unit,
    hasNotificationPermission: Boolean,
    onOpenSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationPermissionSettings: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: (Float) -> Unit
) {
    var showDeckSettings by rememberSaveable { mutableStateOf(false) }
    val needsNotificationPermission =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission

    if (!state.hasNotificationAccess || needsNotificationPermission) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(scaledDp(16.dp, uiScale, 12.dp)),
            colors = CardDefaults.cardColors(containerColor = palette.cardColor)
        ) {
            Column(
                modifier = Modifier.padding(scaledDp(12.dp, uiScale, 8.dp)),
                verticalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
            ) {
                Text(
                    text = "Finish access setup",
                    color = palette.bodyColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "PicklyDeck reads the active media session title, artist, album art, playback state, and transport controls from your device so it can render the turntable UI, widget, and lock screen controls. This data stays on-device. PicklyDeck does not upload playback history, fetch lyrics, or require an account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.helperText
                )
                if (!state.hasNotificationAccess) {
                    Text(
                        text = "Step 1. Enable notification access so Android can expose the active player session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.bodyColor
                    )
                    Button(onClick = onOpenSettings) {
                        Text("Open Notification Access")
                    }
                }
                if (needsNotificationPermission) {
                    Text(
                        text = "Step 2. Allow notifications on Android 13+ so PicklyDeck can show its ongoing controls notification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.bodyColor
                    )
                    Button(onClick = onRequestNotificationPermission) {
                        Text("Allow Notifications")
                    }
                    TextButton(onClick = onOpenNotificationPermissionSettings) {
                        Text("Open App Notification Settings", color = palette.controlTint)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(8.dp, uiScale, 6.dp)))
    }

    PlaybackControls(
        isPlaying = state.isPlaying,
        enabled = state.hasNotificationAccess,
        palette = palette,
        uiScale = uiScale,
        onPrev = onPrev,
        onPlayPause = onPlayPause,
        onNext = onNext
    )

    Spacer(modifier = Modifier.height(scaledDp(8.dp, uiScale, 6.dp)))
    if (state.canSeek) {
        SeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            palette = palette,
            uiScale = uiScale,
            onSeekPreview = onSeekPreview,
            onSeekCommit = onSeekCommit
        )
    } else {
        Text(
            text = "This media session does not expose seek controls.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.helperText
        )
    }

    Spacer(modifier = Modifier.height(scaledDp(10.dp, uiScale, 6.dp)))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Deck Settings",
            style = MaterialTheme.typography.bodySmall,
            color = palette.subtitleColor
        )
        IconButton(onClick = { showDeckSettings = true }) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Open deck settings",
                tint = palette.controlTint
            )
        }
    }

    if (showDeckSettings) {
        DeckSettingsDialog(
            palette = palette,
            uiScale = uiScale,
            selectedTheme = selectedTheme,
            onThemeSelected = onThemeSelected,
            selectedVisual = selectedVisual,
            onVisualSelected = onVisualSelected,
            selectedVisualizer = selectedVisualizer,
            onVisualizerSelected = onVisualizerSelected,
            onDismiss = { showDeckSettings = false }
        )
    }
}

@Composable
private fun DeckSettingsDialog(
    palette: DeckPalette,
    uiScale: Float,
    selectedTheme: DeckThemeOption,
    onThemeSelected: (DeckThemeOption) -> Unit,
    selectedVisual: DeckVisualOption,
    onVisualSelected: (DeckVisualOption) -> Unit,
    selectedVisualizer: VisualizerOption,
    onVisualizerSelected: (VisualizerOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Deck Settings",
                color = palette.titleColor,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.subtitleColor
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
                ) {
                    ThemeChip(
                        text = "AUTO",
                        selected = selectedTheme == DeckThemeOption.AUTO,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onThemeSelected(DeckThemeOption.AUTO) }
                    )
                    ThemeChip(
                        text = "SILVER",
                        selected = selectedTheme == DeckThemeOption.SILVER,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onThemeSelected(DeckThemeOption.SILVER) }
                    )
                    ThemeChip(
                        text = "BLACK",
                        selected = selectedTheme == DeckThemeOption.BLACK,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onThemeSelected(DeckThemeOption.BLACK) }
                    )
                }

                Text(
                    text = "Visual Style",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.subtitleColor
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
                ) {
                    ThemeChip(
                        text = "TURNTABLE",
                        selected = selectedVisual == DeckVisualOption.TURNTABLE,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualSelected(DeckVisualOption.TURNTABLE) }
                    )
                    ThemeChip(
                        text = "CAMPFIRE",
                        selected = selectedVisual == DeckVisualOption.CAMPFIRE,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualSelected(DeckVisualOption.CAMPFIRE) }
                    )
                    ThemeChip(
                        text = "CD PLAYER",
                        selected = selectedVisual == DeckVisualOption.CD_PLAYER,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualSelected(DeckVisualOption.CD_PLAYER) }
                    )
                }

                Text(
                    text = "Visualizer Pack",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.subtitleColor
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
                ) {
                    ThemeChip(
                        text = "OFF",
                        selected = selectedVisualizer == VisualizerOption.OFF,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualizerSelected(VisualizerOption.OFF) }
                    )
                    ThemeChip(
                        text = "WAVE",
                        selected = selectedVisualizer == VisualizerOption.WAVE,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualizerSelected(VisualizerOption.WAVE) }
                    )
                    ThemeChip(
                        text = "SPECTRUM",
                        selected = selectedVisualizer == VisualizerOption.SPECTRUM,
                        palette = palette,
                        uiScale = uiScale,
                        onClick = { onVisualizerSelected(VisualizerOption.SPECTRUM) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.controlTint)
            }
        },
        containerColor = palette.cardColor
    )
}

@Composable
private fun HeaderCard(
    appName: String,
    title: String,
    artist: String,
    selectedTheme: DeckThemeOption,
    onThemeSelected: (DeckThemeOption) -> Unit,
    palette: DeckPalette,
    uiScale: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scaledDp(20.dp, uiScale, 14.dp)),
        colors = CardDefaults.cardColors(containerColor = palette.cardColor)
    ) {
        Column(modifier = Modifier.padding(scaledDp(14.dp, uiScale, 10.dp))) {
            Text(
                text = "PicklyDeck",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.titleColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.bodySmall,
                color = palette.subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(scaledDp(6.dp, uiScale, 4.dp)))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.bodyColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(scaledDp(10.dp, uiScale, 6.dp)))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8.dp, uiScale, 6.dp))
            ) {
                ThemeChip(
                    text = "SILVER",
                    selected = selectedTheme == DeckThemeOption.SILVER,
                    palette = palette,
                    uiScale = uiScale,
                    onClick = { onThemeSelected(DeckThemeOption.SILVER) }
                )
                ThemeChip(
                    text = "BLACK",
                    selected = selectedTheme == DeckThemeOption.BLACK,
                    palette = palette,
                    uiScale = uiScale,
                    onClick = { onThemeSelected(DeckThemeOption.BLACK) }
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    text: String,
    selected: Boolean,
    palette: DeckPalette,
    uiScale: Float,
    onClick: () -> Unit
) {
    val bg = if (selected) palette.controlButtonBg else Color.Transparent
    val border = if (selected) palette.controlButtonBorder else palette.subtitleColor.copy(alpha = 0.45f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(scaledDp(12.dp, uiScale, 8.dp)))
            .border(1.dp, border, RoundedCornerShape(scaledDp(12.dp, uiScale, 8.dp)))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(
                horizontal = scaledDp(12.dp, uiScale, 8.dp),
                vertical = scaledDp(7.dp, uiScale, 4.dp)
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.controlTint else palette.subtitleColor
        )
    }
}

@Composable
private fun TurntableDeck(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    playbackSpeed: Float,
    needleProgress: Float,
    positionMs: Long,
    durationMs: Long,
    albumArt: Bitmap?,
    palette: DeckPalette,
    themeOption: DeckThemeOption,
    visualOption: DeckVisualOption,
    visualizerOption: VisualizerOption,
    trackTitle: String,
    trackArtist: String,
    uiScale: Float,
    onNeedleProgress: (Float) -> Unit,
    onNeedleRelease: () -> Unit,
    onNeedleScratch: (Float) -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    if (visualOption != DeckVisualOption.TURNTABLE) {
        when (visualOption) {
            DeckVisualOption.CAMPFIRE -> CampfireDeck(
                modifier = modifier,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                positionMs = positionMs,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                palette = palette,
                uiScale = uiScale,
                onPrev = onPrev,
                onPlayPause = onPlayPause,
                onNext = onNext
            )
            DeckVisualOption.CD_PLAYER -> CdPlayerDeck(
                modifier = modifier,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                positionMs = positionMs,
                durationMs = durationMs,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                albumArt = albumArt,
                palette = palette,
                uiScale = uiScale,
                onPrev = onPrev,
                onPlayPause = onPlayPause,
                onNext = onNext
            )
            DeckVisualOption.TURNTABLE -> Unit
        }
        return
    }

    val angularSpeed = remember { Animatable(0f) }
    var platterRotation by remember { mutableFloatStateOf(0f) }
    var deckSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isPlaying, playbackSpeed) {
        val targetSpeed = if (isPlaying) 200f * playbackSpeed.coerceIn(0.85f, 1.2f) else 0f
        angularSpeed.animateTo(
            targetValue = targetSpeed,
            animationSpec = tween(
                durationMillis = if (isPlaying) 500 else 900,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(Unit) {
        var previousNanos = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (previousNanos != 0L) {
                    val dt = (now - previousNanos) / 1_000_000_000f
                    platterRotation = (platterRotation + angularSpeed.value * dt) % 360f
                }
                previousNanos = now
            }
        }
    }

    val deckCorner = RoundedCornerShape(scaledDp(26.dp, uiScale, 18.dp))
    Box(
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(
                elevation = scaledDp(18.dp, uiScale, 12.dp),
                shape = deckCorner,
                clip = false
            )
            .clip(deckCorner)
            .background(
                Brush.linearGradient(
                    colors = palette.deckGradient,
                    start = Offset.Zero,
                    end = Offset(1200f, 900f)
                )
            )
            .border(
                1.dp,
                palette.deckBorder,
                deckCorner
            )
            .onSizeChanged { deckSize = it }
            .padding(scaledDp(14.dp, uiScale, 9.dp))
    ) {
        DeckBase(
            palette = palette,
            themeOption = themeOption
        )
        Platter(
            rotation = platterRotation,
            albumArt = albumArt,
            palette = palette,
            uiScale = uiScale,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            positionMs = positionMs
        )
        VisualizerOverlay(
            option = visualizerOption,
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            positionMs = positionMs,
            rotation = platterRotation,
            palette = palette
        )
        Needle(
            progress = needleProgress,
            deckSize = deckSize,
            isPlaying = isPlaying,
            palette = palette,
            onNeedleProgress = onNeedleProgress,
            onNeedleRelease = onNeedleRelease,
            onNeedleScratch = onNeedleScratch
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun CampfireDeck(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    playbackSpeed: Float,
    positionMs: Long,
    trackTitle: String,
    trackArtist: String,
    palette: DeckPalette,
    uiScale: Float,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val touchBoost = remember { Animatable(0f) }
    val moonPhase = remember { currentMoonPhaseState() }
    val transition = rememberInfiniteTransition(label = "campfire-transition")
    val flicker by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 860, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "campfire-flicker"
    )
    val rainPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "campfire-rain-phase"
    )
    val sparkPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "campfire-spark-phase"
    )
    val smokePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "campfire-smoke-phase"
    )
    val speedFactor = playbackSpeed.coerceIn(0.85f, 1.25f)
    val beatPhase = ((positionMs % 1800L).toFloat() / 1800f) * 6.2831855f * speedFactor
    val beatA = max(0f, sin(beatPhase))
    val beatB = max(0f, sin(beatPhase * 1.9f + 1.2f))
    val beatEnergy = if (isPlaying) beatA * 0.64f + beatB * 0.36f else 0f
    val targetBurn = if (isPlaying) {
        (0.82f + beatEnergy * 0.34f + (flicker - 1f) * 0.24f + touchBoost.value * 0.26f).coerceIn(0.74f, 1.48f)
    } else {
        (0.3f + touchBoost.value * 0.2f).coerceIn(0.22f, 0.62f)
    }
    val burn by animateFloatAsState(
        targetValue = targetBurn,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "campfire-burn"
    )
    val corner = RoundedCornerShape(scaledDp(26.dp, uiScale, 18.dp))

    Box(
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(elevation = scaledDp(18.dp, uiScale, 12.dp), shape = corner, clip = false)
            .clip(corner)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0D13), Color(0xFF101724), Color(0xFF18110E))
                )
            )
            .pointerInput(isPlaying, playbackSpeed) {
                detectTapGestures { tap ->
                    val nx = tap.x / size.width.toFloat()
                    val ny = tap.y / size.height.toFloat()
                    if (nx in 0.2f..0.8f && ny in 0.45f..0.9f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            touchBoost.snapTo(1f)
                            touchBoost.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                }
            }
            .padding(scaledDp(12.dp, uiScale, 8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val horizonY = h * 0.64f

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xAA202E47),
                        Color(0x55202C3B),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = horizonY
                )
            )

            // Moon with five-phase mask (crescent/quarter/full).
            val moonCenter = Offset(w * 0.84f, h * 0.16f)
            val moonRadius = w * 0.047f
            val moonDark = Color(0xFF111924)
            val moonLight = Color(0xFFF0D96A)
            drawCircle(
                color = Color(0x332B415F),
                radius = moonRadius * 2.45f,
                center = moonCenter
            )
            drawCircle(
                color = moonDark,
                radius = moonRadius,
                center = moonCenter
            )
            val p = moonPhase.phaseRatio
            when {
                p < 0.12f -> {
                    // Waxing crescent (right side lit).
                    drawCircle(color = moonLight, radius = moonRadius, center = moonCenter)
                    drawCircle(
                        color = moonDark,
                        radius = moonRadius,
                        center = Offset(moonCenter.x - moonRadius * 0.62f, moonCenter.y)
                    )
                }
                p < 0.38f -> {
                    // First quarter (right half lit).
                    drawArc(
                        color = moonLight,
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
                        size = Size(moonRadius * 2f, moonRadius * 2f)
                    )
                }
                p < 0.62f -> {
                    // Full moon.
                    drawCircle(color = moonLight, radius = moonRadius, center = moonCenter)
                }
                p < 0.88f -> {
                    // Last quarter (left half lit).
                    drawArc(
                        color = moonLight,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
                        size = Size(moonRadius * 2f, moonRadius * 2f)
                    )
                }
                else -> {
                    // Waning crescent (left side lit).
                    drawCircle(color = moonLight, radius = moonRadius, center = moonCenter)
                    drawCircle(
                        color = moonDark,
                        radius = moonRadius,
                        center = Offset(moonCenter.x + moonRadius * 0.62f, moonCenter.y)
                    )
                }
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = moonRadius,
                center = moonCenter,
                style = Stroke(width = 1.1f)
            )

            for (i in 0..12) {
                val bw = w * (0.045f + (i % 4) * 0.012f)
                val bx = i * (w / 12.5f)
                val bh = h * (0.07f + (i % 5) * 0.03f)
                drawRoundRect(
                    color = Color(0xCC0C1220),
                    topLeft = Offset(bx, horizonY - bh),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(4f)
                )
                if (i % 2 == 0) {
                    drawRect(
                        color = Color(0x55B8C9DA),
                        topLeft = Offset(bx + bw * 0.24f, horizonY - bh * 0.58f),
                        size = Size(bw * 0.1f, bh * 0.18f)
                    )
                }
            }

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x662B1E18),
                        Color(0xAA1D130F)
                    ),
                    startY = horizonY * 0.94f,
                    endY = h
                ),
                topLeft = Offset(0f, horizonY * 0.9f),
                size = Size(w, h - horizonY * 0.9f)
            )

            val rainCount = 52
            val rainOffsetY = rainPhase * h * 0.22f
            for (i in 0 until rainCount) {
                val x = (i * 37 % 997) / 997f * w
                val yBase = (i * 53 % 991) / 991f * horizonY
                val y = (yBase + rainOffsetY) % horizonY
                drawLine(
                    color = Color(0x44A9C7E8),
                    start = Offset(x, y),
                    end = Offset(x - w * 0.018f, y + h * 0.04f),
                    strokeWidth = 1.4f
                )
            }

            val emberCenter = Offset(w * 0.5f, h * 0.78f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xCCFF9A39), Color(0x66D64E00), Color.Transparent),
                    center = emberCenter,
                    radius = w * 0.25f
                ),
                radius = w * 0.27f,
                center = emberCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x55FFD07A), Color.Transparent),
                    center = Offset(emberCenter.x, emberCenter.y - h * 0.03f),
                    radius = w * 0.18f
                ),
                radius = w * 0.19f,
                center = Offset(emberCenter.x, emberCenter.y - h * 0.03f)
            )

            drawRoundRect(
                color = Color(0xFF3A241A),
                topLeft = Offset(w * 0.37f, h * 0.69f),
                size = Size(w * 0.26f, h * 0.035f),
                cornerRadius = CornerRadius(14f)
            )
            drawRoundRect(
                color = Color(0xFF4A2D1F),
                topLeft = Offset(w * 0.34f, h * 0.73f),
                size = Size(w * 0.32f, h * 0.035f),
                cornerRadius = CornerRadius(14f)
            )
            drawRoundRect(
                color = Color(0xFF2E1A12),
                topLeft = Offset(w * 0.42f, h * 0.75f),
                size = Size(w * 0.22f, h * 0.028f),
                cornerRadius = CornerRadius(12f)
            )
            for (i in 0..8) {
                val tx = w * 0.34f + i * w * 0.035f
                val ty = h * (0.742f + (i % 2) * 0.012f)
                drawLine(
                    color = Color(0x7A1B0F09),
                    start = Offset(tx, ty),
                    end = Offset(tx + w * 0.06f, ty + h * 0.01f),
                    strokeWidth = 1.2f
                )
            }

            val fireCenter = Offset(w * 0.5f, h * 0.66f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xA6FFC98A),
                        Color(0x66FF7D1D),
                        Color.Transparent
                    ),
                    center = fireCenter,
                    radius = w * 0.34f * burn
                ),
                radius = w * 0.36f * burn,
                center = fireCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66FFE3B2), Color.Transparent),
                    center = Offset(fireCenter.x, fireCenter.y - h * 0.02f),
                    radius = w * 0.22f * burn
                ),
                radius = w * 0.24f * burn,
                center = Offset(fireCenter.x, fireCenter.y - h * 0.02f)
            )

            val tongueCount = 15
            for (i in 0 until tongueCount) {
                val lane = (i - (tongueCount - 1) / 2f) / tongueCount
                val swing = sin((sparkPhase * 6.283f) + i * 0.76f) * w * (0.011f + (i % 3) * 0.0015f)
                val top = h * (0.59f - burn * 0.08f) + i * h * 0.0028f
                val flameH = h * (0.09f + (i % 5) * 0.013f) * burn
                val flameW = w * (0.038f + (i % 4) * 0.006f)
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF8FFF2CF),
                            Color(0xEFFFBC56),
                            Color(0xD8FF7A1E),
                            Color(0x7FE54800)
                        )
                    ),
                    topLeft = Offset(
                        w * 0.5f + lane * w * 0.2f + swing - flameW * 0.5f,
                        top
                    ),
                    size = Size(flameW, flameH)
                )
            }

            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xC6FFF3C0), Color(0x99FFBE52), Color.Transparent)
                ),
                topLeft = Offset(w * 0.465f, h * (0.575f - burn * 0.06f)),
                size = Size(w * 0.07f, h * (0.13f * burn))
            )

            for (i in 0 until 30) {
                val sx = w * (0.42f + ((i * 17) % 23) / 100f)
                val sy = h * (0.55f - ((sparkPhase + i * 0.08f) % 1f) * 0.31f)
                drawCircle(
                    color = Color(0xCCFFC56A),
                    radius = 1.2f + (i % 3) * 0.55f,
                    center = Offset(sx, sy)
                )
            }

            for (i in 0 until 11) {
                val drift = sin((smokePhase * 6.283f) + i * 0.7f) * w * 0.022f
                val y = h * (0.52f - ((smokePhase + i * 0.085f) % 1f) * 0.36f)
                val alpha = (0.18f - i * 0.012f).coerceAtLeast(0.03f)
                val radius = w * (0.012f + i * 0.004f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(w * 0.5f + drift, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(scaledDp(6.dp, uiScale, 4.dp))
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF3F7FC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trackArtist.isNotBlank()) {
                Text(
                    text = trackArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB7C6D7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CdPlayerDeck(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    playbackSpeed: Float,
    positionMs: Long,
    durationMs: Long,
    trackTitle: String,
    trackArtist: String,
    albumArt: Bitmap?,
    palette: DeckPalette,
    uiScale: Float,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val angularSpeed = remember { Animatable(0f) }
    var discRotation by remember { mutableFloatStateOf(0f) }
    val corner = RoundedCornerShape(scaledDp(24.dp, uiScale, 16.dp))
    val cdTimeText = if (durationMs > 0L) {
        "${formatMs(positionMs)} / ${formatMs(durationMs)}"
    } else {
        formatMs(positionMs)
    }

    LaunchedEffect(isPlaying, playbackSpeed) {
        angularSpeed.animateTo(
            targetValue = if (isPlaying) 170f * playbackSpeed.coerceIn(0.85f, 1.2f) else 0f,
            animationSpec = tween(durationMillis = if (isPlaying) 380 else 780, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = (now - lastNanos) / 1_000_000_000f
                    discRotation = (discRotation + angularSpeed.value * dt) % 360f
                }
                lastNanos = now
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(elevation = scaledDp(16.dp, uiScale, 12.dp), shape = corner, clip = false)
            .clip(corner)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE8EBF0), Color(0xFFD7DDE6), Color(0xFFC5CDD8)),
                    start = Offset.Zero,
                    end = Offset(980f, 980f)
                )
            )
            .border(1.dp, Color(0xFF8D99A8), corner)
            .padding(scaledDp(12.dp, uiScale, 8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Body panel seams and screws.
            drawRoundRect(
                color = Color(0x66FFFFFF),
                topLeft = Offset(w * 0.03f, h * 0.03f),
                size = Size(w * 0.5f, h * 0.08f),
                cornerRadius = CornerRadius(10f)
            )
            drawRoundRect(
                color = Color(0x33000000),
                topLeft = Offset(w * 0.62f, h * 0.14f),
                size = Size(w * 0.3f, h * 0.05f),
                cornerRadius = CornerRadius(8f)
            )
            val screws = listOf(
                Offset(w * 0.08f, h * 0.09f),
                Offset(w * 0.92f, h * 0.09f),
                Offset(w * 0.08f, h * 0.91f),
                Offset(w * 0.92f, h * 0.91f)
            )
            screws.forEach { s ->
                drawCircle(color = Color(0xFF8B96A4), radius = w * 0.012f, center = s)
                drawLine(
                    color = Color(0x99242A33),
                    start = Offset(s.x - w * 0.007f, s.y),
                    end = Offset(s.x + w * 0.007f, s.y),
                    strokeWidth = 1.2f
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(scaledDp(72.dp, uiScale, 52.dp)),
            shape = RoundedCornerShape(scaledDp(8.dp, uiScale, 6.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A241F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = scaledDp(10.dp, uiScale, 7.dp), vertical = scaledDp(8.dp, uiScale, 6.dp))
            ) {
                Text(
                    text = trackTitle.ifBlank { "No active playback" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCFF9AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trackArtist.isNotBlank()) {
                    Text(
                        text = trackArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9FDBA1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Disc tray frame.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(scaledDp(14.dp, uiScale, 10.dp)))
                .background(Color(0x22161B22))
                .border(1.dp, Color(0x556F7B8A), RoundedCornerShape(scaledDp(14.dp, uiScale, 10.dp)))
                .padding(scaledDp(12.dp, uiScale, 8.dp))
        ) {
        Box(
            modifier = Modifier
                    .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(1f)
                .rotate(discRotation)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFCBD5E0), Color(0xFF818D9B), Color(0xFF4A5461)),
                        center = Offset(center.x - r * 0.22f, center.y - r * 0.2f),
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
                drawCircle(color = Color(0xFF0E1218), radius = r * 0.93f, center = center)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0x668EA3BE),
                            Color(0x33C9D9EA),
                            Color(0x6697AFC8),
                            Color(0x338AA2BC),
                            Color(0x668EA3BE)
                        ),
                        center = center
                    ),
                    radius = r * 0.9f,
                    center = center
                )
                for (i in 1..22) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = r * (0.88f - i * 0.032f),
                        center = center,
                        style = Stroke(width = 0.8f)
                    )
                }
                drawCircle(color = Color(0xFFE6EDF4), radius = r * 0.11f, center = center)
                drawCircle(color = Color(0xFF5E6A78), radius = r * 0.05f, center = center)
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = r * 0.028f,
                    center = Offset(center.x - r * 0.02f, center.y - r * 0.02f)
                )
            }
            RecordLabel(
                albumArt = albumArt,
                palette = palette,
                uiScale = uiScale * 0.92f,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(scaledDp(98.dp, uiScale, 66.dp))
            )
        }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = scaledDp(64.dp, uiScale, 46.dp))
                .fillMaxWidth(0.4f)
                .height(scaledDp(34.dp, uiScale, 24.dp)),
            shape = RoundedCornerShape(scaledDp(8.dp, uiScale, 6.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F1713)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x88485C50))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cdTimeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD7F8AF),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Physical control buttons.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = scaledDp(8.dp, uiScale, 6.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CdPhysicalButton(
                label = "<<",
                uiScale = uiScale,
                onClick = onPrev
            )
            CdPhysicalButton(
                label = if (isPlaying) "||" else ">",
                uiScale = uiScale,
                onClick = onPlayPause
            )
            CdPhysicalButton(
                label = ">>",
                uiScale = uiScale,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun CdPhysicalButton(
    label: String,
    uiScale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "cd-button-press"
    )
    val borderColor = mixColor(Color(0xFF95A2B3), Color(0xFF74859A), pressProgress)
    val baseColor = mixColor(Color(0xFFDCE2EB), Color(0xFFBAC5D2), pressProgress)
    Box(
        modifier = Modifier
            .size(scaledDp(44.dp, uiScale, 34.dp))
            .offset(y = (pressProgress * 2.6f).dp)
            .shadow(
                elevation = scaledDp((4.5f - pressProgress * 3.2f).dp, uiScale, 1.dp),
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(baseColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF3B4654),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DeckBase(
    palette: DeckPalette,
    themeOption: DeckThemeOption
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val screwRadius = size.minDimension * 0.012f
        val plateInset = size.minDimension * 0.02f
        val key = keyLight(size)
        val far = Offset(size.width * 0.88f, size.height * 0.9f)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.82f),
                    Color.White.copy(alpha = 0.18f),
                    Color.Black.copy(alpha = 0.06f)
                ),
                start = key,
                end = far
            ),
            topLeft = Offset(plateInset, plateInset),
            size = Size(size.width - plateInset * 2f, size.height - plateInset * 2f),
            cornerRadius = CornerRadius(size.minDimension * 0.03f)
        )

        when (themeOption) {
            DeckThemeOption.AUTO -> {
                val hatch = size.minDimension * 0.024f
                val count = (size.width / hatch).toInt() + 8
                for (i in -4..count) {
                    val x = i * hatch.toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(x, plateInset),
                        end = Offset(x + size.height * 0.58f, size.height - plateInset),
                        strokeWidth = 1f
                    )
                }
            }
            DeckThemeOption.SILVER -> {
                val lineStep = size.minDimension * 0.012f
                val lineCount = (size.width / lineStep).toInt() + 3
                for (i in 0..lineCount) {
                    val x = i * lineStep
                    val alpha = if (i % 3 == 0) 0.12f else 0.06f
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(x, plateInset),
                        end = Offset(x + size.height * 0.06f, size.height - plateInset),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = alpha * 0.48f),
                        start = Offset(x + 1.4f, plateInset),
                        end = Offset(x + size.height * 0.06f + 1.4f, size.height - plateInset),
                        strokeWidth = 1f
                    )
                }
            }
            DeckThemeOption.BLACK -> {
                val hatch = size.minDimension * 0.028f
                val count = (size.width / hatch).toInt() + 10
                for (i in -5..count) {
                    val x = i * hatch.toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.035f),
                        start = Offset(x, plateInset),
                        end = Offset(x + size.height * 0.62f, size.height - plateInset),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.08f),
                        start = Offset(x + hatch * 0.42f, plateInset),
                        end = Offset(x + hatch * 0.42f + size.height * 0.62f, size.height - plateInset),
                        strokeWidth = 1f
                    )
                }
            }
        }

        val glow = Brush.radialGradient(
            colors = listOf(palette.deckGlow, Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.55f),
            radius = size.minDimension * 0.58f
        )
        drawRect(brush = glow)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.06f),
            size = Size(size.width * 0.34f, size.height * 0.1f),
            cornerRadius = CornerRadius(size.minDimension * 0.025f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(size.width * 0.62f, size.height * 0.18f),
            size = Size(size.width * 0.22f, size.height * 0.07f),
            cornerRadius = CornerRadius(size.minDimension * 0.02f)
        )

        val points = listOf(
            Offset(size.width * 0.08f, size.height * 0.08f),
            Offset(size.width * 0.92f, size.height * 0.08f),
            Offset(size.width * 0.08f, size.height * 0.92f),
            Offset(size.width * 0.92f, size.height * 0.92f)
        )
        points.forEach { p ->
            drawCircle(color = palette.screwColor, radius = screwRadius, center = p)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = screwRadius * 0.38f,
                center = Offset(p.x - screwRadius * 0.35f, p.y - screwRadius * 0.35f)
            )
            drawLine(
                color = palette.screwCut,
                start = Offset(p.x - screwRadius * 0.6f, p.y),
                end = Offset(p.x + screwRadius * 0.6f, p.y),
                strokeWidth = 1.3f
            )
        }
    }
}

@Composable
private fun BoxScope.Platter(
    rotation: Float,
    albumArt: Bitmap?,
    palette: DeckPalette,
    uiScale: Float,
    isPlaying: Boolean,
    playbackSpeed: Float,
    positionMs: Long
) {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .rotate(rotation)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.5f
            val key = keyLight(size)
            val shadowCenter = Offset(
                x = center.x + (center.x - key.x) * 0.18f,
                y = center.y + (center.y - key.y) * 0.18f + radius * 0.04f
            )

            // Depth shadow under the platter.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                    center = shadowCenter,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.02f,
                center = shadowCenter
            )

            // Outer rim (height/thickness)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF646A73), Color(0xFF262A2F), Color(0xFF101215)),
                    start = key,
                    end = Offset(size.width * 0.88f, size.height * 0.9f)
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF0A0B0C),
                radius = radius * 0.963f,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = palette.platterGradient,
                    center = Offset(center.x - radius * 0.16f, center.y - radius * 0.14f),
                    radius = radius
                ),
                radius = radius * 0.955f,
                center = center
            )

            for (i in 0 until 90) {
                val angle = i * 4f
                val r1 = radius * 0.92f
                val r2 = radius * 0.98f
                val a = Math.toRadians(angle.toDouble())
                val p1 = Offset(
                    center.x + (cos(a) * r1).toFloat(),
                    center.y + (sin(a) * r1).toFloat()
                )
                val p2 = Offset(
                    center.x + (cos(a) * r2).toFloat(),
                    center.y + (sin(a) * r2).toFloat()
                )
                drawLine(
                    color = if (i % 2 == 0) palette.strobeA else palette.strobeB,
                    start = p1,
                    end = p2,
                    strokeWidth = 1.2f
                )
            }

            for (i in 1..22) {
                drawCircle(
                    color = palette.grooveColor,
                    radius = radius * 0.94f * (i / 22f),
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            drawDualReflections(
                center = center,
                radius = radius * 0.84f,
                primaryColor = palette.glossColor.copy(alpha = 0.9f),
                secondaryColor = Color.White.copy(alpha = 0.12f),
                strokeWidth = radius * 0.045f
            )

            // Moving rim specular and key light pulse for richer platter lighting.
            val pulse = if (isPlaying) {
                0.72f + 0.28f * (sin((positionMs % 2400L).toFloat() / 2400f * 6.2831855f) * 0.5f + 0.5f)
            } else {
                0.4f
            }
            val specularStart = (210f + rotation * (0.65f + playbackSpeed.coerceIn(0.8f, 1.2f) * 0.25f)) % 360f
            drawArc(
                color = Color.White.copy(alpha = 0.17f * pulse),
                startAngle = specularStart,
                sweepAngle = 34f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.96f, center.y - radius * 0.96f),
                size = Size(radius * 1.92f, radius * 1.92f),
                style = Stroke(width = radius * 0.024f, cap = StrokeCap.Round)
            )
            drawArc(
                color = palette.glossColor.copy(alpha = 0.24f * pulse),
                startAngle = (specularStart + 146f) % 360f,
                sweepAngle = 22f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.9f),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = radius * 0.016f, cap = StrokeCap.Round)
            )

            // Raised center hub for 3D feel.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3E434A), Color(0xFF13161A)),
                    center = Offset(center.x - radius * 0.08f, center.y - radius * 0.08f),
                    radius = radius * 0.17f
                ),
                radius = radius * 0.16f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = radius * 0.055f,
                center = Offset(center.x - radius * 0.035f, center.y - radius * 0.04f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius * 0.03f,
                center = Offset(center.x + radius * 0.045f, center.y + radius * 0.02f)
            )
        }

        RecordLabel(
            albumArt = albumArt,
            palette = palette,
            uiScale = uiScale,
            modifier = Modifier
                .align(Alignment.Center)
                .size(scaledDp(116.dp, uiScale, 78.dp))
        )
    }
}

@Composable
private fun RecordLabel(
    albumArt: Bitmap?,
    palette: DeckPalette,
    uiScale: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, palette.labelRing, CircleShape)
            .background(palette.labelBg),
        contentAlignment = Alignment.Center
    ) {
        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.labelFallbackStart, palette.labelFallbackEnd),
                        center = center,
                        radius = size.minDimension * 0.56f
                    ),
                    radius = size.minDimension * 0.5f,
                    center = center
                )
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "VINYL",
                        center.x - 34f,
                        center.y + 6f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(235, 244, 226, 205)
                            textSize = 28f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(scaledDp(16.dp, uiScale, 10.dp))
                .clip(CircleShape)
                .background(palette.spindle)
        )
    }
}

@Composable
private fun BoxScope.VisualizerOverlay(
    option: VisualizerOption,
    isPlaying: Boolean,
    playbackSpeed: Float,
    positionMs: Long,
    rotation: Float,
    palette: DeckPalette
) {
    if (option == VisualizerOption.OFF) return

    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth(0.84f)
            .aspectRatio(1f)
            .rotate(if (option == VisualizerOption.SPECTRUM) rotation * 0.06f else 0f)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = min(size.width, size.height) * 0.5f
        val playEnergy = if (isPlaying) 1f else 0.28f
        val phase = (positionMs % 5000L).toFloat() / 5000f * 6.2831855f * playbackSpeed.coerceIn(0.85f, 1.25f)

        when (option) {
            VisualizerOption.WAVE -> {
                val samples = 104
                for (i in 0 until samples) {
                    val t = i / samples.toFloat()
                    val theta = t * 6.2831855f
                    val ampA = (sin(phase * 1.25f + i * 0.33f) * 0.5f + 0.5f)
                    val ampB = (sin(phase * 2.4f + i * 0.17f + 1.2f) * 0.5f + 0.5f)
                    val amp = (0.24f + (ampA * 0.7f + ampB * 0.3f) * 0.78f) * playEnergy
                    val inner = r * 0.66f
                    val outer = inner + r * (0.045f + amp * 0.055f)
                    val start = Offset(
                        x = center.x + cos(theta) * inner,
                        y = center.y + sin(theta) * inner
                    )
                    val end = Offset(
                        x = center.x + cos(theta) * outer,
                        y = center.y + sin(theta) * outer
                    )
                    drawLine(
                        color = mixColor(palette.controlTint, Color.White, 0.2f).copy(alpha = 0.24f + amp * 0.32f),
                        start = start,
                        end = end,
                        strokeWidth = r * 0.008f,
                        cap = StrokeCap.Round
                    )
                }
            }
            VisualizerOption.SPECTRUM -> {
                val bars = 72
                for (i in 0 until bars) {
                    val t = i / bars.toFloat()
                    val theta = t * 6.2831855f
                    val band = (sin(phase * 3.15f + i * 0.44f) * 0.5f + 0.5f)
                    val motion = (sin(phase * 1.45f + i * 0.19f + 0.8f) * 0.5f + 0.5f)
                    val amp = (0.18f + band * 0.62f + motion * 0.2f).coerceIn(0f, 1f) * playEnergy
                    val inner = r * 0.73f
                    val outer = inner + r * (0.03f + amp * 0.11f)
                    val start = Offset(
                        x = center.x + cos(theta) * inner,
                        y = center.y + sin(theta) * inner
                    )
                    val end = Offset(
                        x = center.x + cos(theta) * outer,
                        y = center.y + sin(theta) * outer
                    )
                    drawLine(
                        color = mixColor(palette.controlButtonBorder, palette.controlTint, amp)
                            .copy(alpha = 0.2f + amp * 0.46f),
                        start = start,
                        end = end,
                        strokeWidth = r * 0.01f,
                        cap = StrokeCap.Round
                    )
                }
            }
            VisualizerOption.OFF -> Unit
        }
    }
}

@Composable
private fun Needle(
    progress: Float,
    deckSize: IntSize,
    isPlaying: Boolean,
    palette: DeckPalette,
    onNeedleProgress: (Float) -> Unit,
    onNeedleRelease: () -> Unit,
    onNeedleScratch: (Float) -> Unit
) {
    val currentProgress by rememberUpdatedState(progress)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vibrationTransition = rememberInfiniteTransition(label = "needle-vibration")
    val vibration by vibrationTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needle-jitter"
    )

    val baseAngle = progressToNeedleAngle(progress)
    val liveAngle = if (isPlaying && progress >= NEEDLE_UI_PLAY_START) {
        baseAngle + vibration * 0.14f
    } else {
        baseAngle
    }
    val width = deckSize.width.toFloat().coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(width, deckSize.height) {
                var gestureProgress = currentProgress

                fun progressFromPointer(pointer: Offset): Float {
                    val pivot = Offset(size.width * 0.85f, size.height * 0.2f)
                    val angle = Math.toDegrees(
                        atan2(
                            (pivot.y - pointer.y).toDouble(),
                            (pivot.x - pointer.x).toDouble()
                        )
                    ).toFloat()
                    return angleToNeedleProgress(angle)
                }

                detectDragGestures(
                    onDragStart = { startOffset ->
                        gestureProgress = progressFromPointer(startOffset)
                        onNeedleProgress(gestureProgress)
                    },
                    onDrag = { change, dragAmount ->
                        val target = progressFromPointer(change.position)
                        val deltaLimit = 0.16f
                        val delta = (target - gestureProgress).coerceIn(-deltaLimit, deltaLimit)
                        gestureProgress = (gestureProgress + delta).coerceIn(0f, 1f)
                        onNeedleProgress(gestureProgress)

                        val intensity = ((abs(dragAmount.x) + abs(dragAmount.y) * 0.35f) / 56f)
                            .coerceIn(0f, 1f)
                        if (gestureProgress > 0.45f && intensity > 0.08f) {
                            onNeedleScratch(intensity)
                        }
                    },
                    onDragEnd = {
                        val droppedOnRecord = gestureProgress >= NEEDLE_UI_PLAY_START
                        onNeedleRelease()
                        haptic.performHapticFeedback(
                            if (droppedOnRecord) {
                                HapticFeedbackType.LongPress
                            } else {
                                HapticFeedbackType.TextHandleMove
                            }
                        )
                        performNeedleDropVibration(context, strong = droppedOnRecord)
                    },
                    onDragCancel = onNeedleRelease
                )
            }
    ) {
        val pivot = Offset(size.width * 0.85f, size.height * 0.2f)
        val armLength = size.width * 0.45f
        val radians = Math.toRadians(liveAngle.toDouble())
        val bend = Offset(
            x = pivot.x - (armLength * 0.44f * cos(radians)).toFloat() + (armLength * 0.08f * sin(radians)).toFloat(),
            y = pivot.y - (armLength * 0.44f * sin(radians)).toFloat() - (armLength * 0.08f * cos(radians)).toFloat()
        )
        val end = Offset(
            x = pivot.x - (armLength * cos(radians)).toFloat(),
            y = pivot.y - (armLength * sin(radians)).toFloat()
        )

        drawCircle(color = palette.pivotOuter, radius = size.minDimension * 0.046f, center = pivot)
        drawCircle(color = palette.pivotInner, radius = size.minDimension * 0.017f, center = pivot)
        val counterWeightCenter = Offset(
            x = pivot.x + size.minDimension * 0.06f,
            y = pivot.y + size.minDimension * 0.005f
        )
        drawCircle(
            color = palette.pivotOuter.copy(alpha = 0.95f),
            radius = size.minDimension * 0.023f,
            center = counterWeightCenter
        )
        drawCircle(
            color = palette.pivotInner.copy(alpha = 0.75f),
            radius = size.minDimension * 0.010f,
            center = counterWeightCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = size.minDimension * 0.009f,
            center = Offset(pivot.x - size.minDimension * 0.01f, pivot.y - size.minDimension * 0.01f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = size.minDimension * 0.005f,
            center = Offset(
                counterWeightCenter.x - size.minDimension * 0.005f,
                counterWeightCenter.y - size.minDimension * 0.004f
            )
        )

        val lightOrigin = Offset(size.width * KEY_LIGHT_X, size.height * KEY_LIGHT_Y)
        val armCenter = Offset((pivot.x + end.x) * 0.5f, (pivot.y + end.y) * 0.5f)
        val toLight = Offset(lightOrigin.x - armCenter.x, lightOrigin.y - armCenter.y)
        val toLightLen = sqrt(toLight.x * toLight.x + toLight.y * toLight.y).coerceAtLeast(1f)
        val toLightUnit = Offset(toLight.x / toLightLen, toLight.y / toLightLen)

        val armVector = Offset(end.x - pivot.x, end.y - pivot.y)
        val armVectorLen = sqrt(armVector.x * armVector.x + armVector.y * armVector.y).coerceAtLeast(1f)
        val armNormal = Offset(-armVector.y / armVectorLen, armVector.x / armVectorLen)
        val highlightPolarity = (armNormal.x * toLightUnit.x + armNormal.y * toLightUnit.y)
            .coerceIn(-1f, 1f)

        val shadowShift = size.minDimension * 0.011f
        val shadowOffset = Offset(
            x = -toLightUnit.x * shadowShift + armNormal.x * size.minDimension * 0.0015f,
            y = -toLightUnit.y * shadowShift + armNormal.y * size.minDimension * 0.0015f
        )
        val highlightShiftBase = size.minDimension * 0.0065f
        val primaryHighlightOffset = Offset(
            x = armNormal.x * highlightShiftBase * highlightPolarity,
            y = armNormal.y * highlightShiftBase * highlightPolarity
        )
        val secondaryHighlightOffset = Offset(
            x = armNormal.x * highlightShiftBase * (highlightPolarity * 0.5f - 0.35f),
            y = armNormal.y * highlightShiftBase * (highlightPolarity * 0.5f - 0.35f)
        )
        val primaryHighlightColor = palette.armHighlight.copy(
            alpha = (0.58f + 0.28f * max(0f, highlightPolarity)).coerceIn(0f, 1f)
        )
        val secondaryHighlightColor = Color.White.copy(
            alpha = (0.15f + 0.18f * (1f - abs(highlightPolarity))).coerceIn(0f, 0.36f)
        )
        val shadowColor = palette.armShadow.copy(alpha = 0.94f)

        drawLine(
            color = shadowColor,
            start = Offset(pivot.x + shadowOffset.x, pivot.y + shadowOffset.y),
            end = Offset(bend.x + shadowOffset.x, bend.y + shadowOffset.y),
            strokeWidth = size.minDimension * 0.024f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = shadowColor,
            start = Offset(bend.x + shadowOffset.x, bend.y + shadowOffset.y),
            end = Offset(end.x + shadowOffset.x, end.y + shadowOffset.y),
            strokeWidth = size.minDimension * 0.024f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = palette.armBase.copy(alpha = 0.98f),
            start = pivot,
            end = bend,
            strokeWidth = size.minDimension * 0.0175f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = palette.armBase.copy(alpha = 0.98f),
            start = bend,
            end = end,
            strokeWidth = size.minDimension * 0.0175f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryHighlightColor,
            start = Offset(pivot.x + primaryHighlightOffset.x, pivot.y + primaryHighlightOffset.y),
            end = Offset(bend.x + primaryHighlightOffset.x, bend.y + primaryHighlightOffset.y),
            strokeWidth = size.minDimension * 0.0057f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryHighlightColor,
            start = Offset(bend.x + primaryHighlightOffset.x, bend.y + primaryHighlightOffset.y),
            end = Offset(end.x + primaryHighlightOffset.x, end.y + primaryHighlightOffset.y),
            strokeWidth = size.minDimension * 0.0057f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondaryHighlightColor,
            start = Offset(pivot.x + secondaryHighlightOffset.x, pivot.y + secondaryHighlightOffset.y),
            end = Offset(bend.x + secondaryHighlightOffset.x, bend.y + secondaryHighlightOffset.y),
            strokeWidth = size.minDimension * 0.0032f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondaryHighlightColor.copy(alpha = secondaryHighlightColor.alpha * 0.9f),
            start = Offset(bend.x + secondaryHighlightOffset.x, bend.y + secondaryHighlightOffset.y),
            end = Offset(end.x + secondaryHighlightOffset.x, end.y + secondaryHighlightOffset.y),
            strokeWidth = size.minDimension * 0.0032f,
            cap = StrokeCap.Round
        )

        val headWidth = size.minDimension * 0.065f
        val headHeight = size.minDimension * 0.086f
        val headTopLeft = Offset(end.x - headWidth * 0.5f, end.y - headHeight * 0.3f)
        val headPrimaryShift = Offset(
            x = primaryHighlightOffset.x * 0.65f,
            y = primaryHighlightOffset.y * 0.65f
        )
        val headSecondaryShift = Offset(
            x = secondaryHighlightOffset.x * 0.7f,
            y = secondaryHighlightOffset.y * 0.7f
        )
        val headShadowOffset = Offset(
            x = shadowOffset.x * 0.55f + 2f,
            y = shadowOffset.y * 0.55f + 2f
        )

        drawRoundRect(
            color = palette.cartridgeShadow.copy(alpha = 0.9f),
            topLeft = Offset(headTopLeft.x + headShadowOffset.x, headTopLeft.y + headShadowOffset.y),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(headWidth * 0.16f)
        )
        drawRoundRect(
            color = palette.cartridgeBody,
            topLeft = headTopLeft,
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(headWidth * 0.16f)
        )
        drawRoundRect(
            color = palette.cartridgeHighlight,
            topLeft = Offset(
                headTopLeft.x + headWidth * 0.14f + headPrimaryShift.x,
                headTopLeft.y + headHeight * 0.1f + headPrimaryShift.y
            ),
            size = Size(headWidth * 0.72f, headHeight * 0.17f),
            cornerRadius = CornerRadius(headWidth * 0.1f)
        )
        drawRoundRect(
            color = secondaryHighlightColor.copy(alpha = secondaryHighlightColor.alpha * 0.95f),
            topLeft = Offset(
                headTopLeft.x + headWidth * 0.2f + headSecondaryShift.x,
                headTopLeft.y + headHeight * 0.32f + headSecondaryShift.y
            ),
            size = Size(headWidth * 0.48f, headHeight * 0.08f),
            cornerRadius = CornerRadius(headWidth * 0.08f)
        )

        val stylusStart = Offset(end.x, end.y + headHeight * 0.43f)
        val stylusEnd = Offset(end.x - size.minDimension * 0.013f, end.y + headHeight * 0.63f)
        drawLine(
            color = palette.stylus,
            start = stylusStart,
            end = stylusEnd,
            strokeWidth = 2f
        )

        if (isPlaying && progress >= NEEDLE_UI_PLAY_START) {
            drawCircle(
                color = palette.stylusGlow,
                radius = size.minDimension * 0.016f,
                center = stylusEnd
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    enabled: Boolean,
    palette: DeckPalette,
    uiScale: Float,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val sideButtonSize = scaledDp(48.dp, uiScale, 40.dp)
    val mainButtonSize = scaledDp(66.dp, uiScale, 52.dp)
    val sideIconSize = scaledDp(24.dp, uiScale, 18.dp)
    val mainIconSize = scaledDp(28.dp, uiScale, 20.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            enabled = enabled,
            modifier = Modifier.size(sideButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = palette.controlTint,
                modifier = Modifier.size(sideIconSize)
            )
        }
        IconButton(
            onClick = onPlayPause,
            enabled = enabled,
            modifier = Modifier
                .size(mainButtonSize)
                .background(palette.controlButtonBg, CircleShape)
                .border(1.dp, palette.controlButtonBorder, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = palette.controlTint,
                modifier = Modifier.size(mainIconSize)
            )
        }
        IconButton(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier.size(sideButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = palette.controlTint,
                modifier = Modifier.size(sideIconSize)
            )
        }
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    palette: DeckPalette,
    uiScale: Float,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: (Float) -> Unit
) {
    var dragValue by remember { mutableFloatStateOf(-1f) }
    val sliderValue = if (dragValue >= 0f) dragValue else {
        if (durationMs == 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Column {
        Slider(
            value = sliderValue,
            onValueChange = {
                dragValue = it
                onSeekPreview(it)
            },
            onValueChangeFinished = {
                if (dragValue >= 0f) onSeekCommit(dragValue)
                dragValue = -1f
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scaledDp(2.dp, uiScale, 0.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs((sliderValue * durationMs).toLong()), color = palette.seekText)
            Text(formatMs(durationMs), color = palette.seekText)
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

private fun toReadableAppName(packageName: String): String {
    return when (packageName) {
        "com.spotify.music" -> "Spotify"
        "com.google.android.apps.youtube.music" -> "YouTube Music"
        else -> packageName
    }
}

private fun Context.hasPostNotificationsPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private fun parseThemeOption(raw: String?): DeckThemeOption {
    return DeckThemeOption.entries.firstOrNull { it.name == raw } ?: DeckThemeOption.AUTO
}

private fun parseVisualOption(raw: String?): DeckVisualOption {
    return DeckVisualOption.entries.firstOrNull { it.name == raw } ?: DeckVisualOption.TURNTABLE
}

private fun parseVisualizerOption(raw: String?): VisualizerOption {
    return VisualizerOption.entries.firstOrNull { it.name == raw } ?: VisualizerOption.OFF
}
