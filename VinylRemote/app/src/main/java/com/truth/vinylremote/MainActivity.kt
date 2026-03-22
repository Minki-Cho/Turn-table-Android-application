package com.truth.vinylremote

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private enum class DeckThemeOption {
    SILVER,
    BLACK,
    BRONZE
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
        DeckThemeOption.BRONZE -> DeckPalette(
            pageGradient = listOf(Color(0xFF2D2218), Color(0xFF493321), Color(0xFF1F1812)),
            cardColor = Color(0x4D000000),
            titleColor = Color(0xFFFFE4BF),
            subtitleColor = Color(0xFFEBCFAE),
            bodyColor = Color(0xFFFFF5E8),
            helperText = Color(0xFFD8C5B2),
            deckGradient = listOf(Color(0xFF181410), Color(0xFF2B241D), Color(0xFF13100D)),
            deckBorder = Color(0x803F2E1F),
            deckGlow = Color(0x334E3B28),
            screwColor = Color(0xFF786658),
            screwCut = Color(0x5531261F),
            platterGradient = listOf(Color(0xFF4B4A4A), Color(0xFF1C1C1C), Color(0xFF060606)),
            strobeA = Color(0xFFB0A08D),
            strobeB = Color(0xFF6E6257),
            grooveColor = Color(0xFF1E1E1E),
            glossColor = Color(0x33FFFFFF),
            labelRing = Color(0xB3F4D2AE),
            labelBg = Color(0xFF2D2015),
            labelFallbackStart = Color(0xFFAE5B28),
            labelFallbackEnd = Color(0xFF6D3517),
            spindle = Color(0xFFE7D9C7),
            controlTint = Color(0xFFF1DAB8),
            controlButtonBg = Color(0xFF2A2118),
            controlButtonBorder = Color(0xFF8C6B4B),
            seekText = Color(0xFFFFE8C8),
            armBase = Color(0xFFE7E5E1),
            armShadow = Color(0x88747474),
            armHighlight = Color(0xFFEFF4F9),
            pivotOuter = Color(0xFFAA8D6D),
            pivotInner = Color(0xFF2A251F),
            cartridgeBody = Color(0xFF8D8B88),
            cartridgeShadow = Color(0x8A1D1D1D),
            cartridgeHighlight = Color(0xFFC5C7CB),
            stylus = Color(0xFF272727),
            stylusGlow = Color(0x33F8DDBB)
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
private const val LYRICS_SCROLL_HOLD_MS = 15_000L

private data class LyricCue(
    val timeMs: Long,
    val text: String
)

private data class LrcParseResult(
    val cues: List<LyricCue>,
    val offsetMs: Long
)

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

private fun parseLrc(rawLyrics: String): LrcParseResult {
    val timestampRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    val offsetRegex = Regex("""\[(?i:offset)\s*:\s*([+-]?\d+)]""")
    val cues = mutableListOf<LyricCue>()
    var offsetMs = 0L

    rawLyrics
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()
        .forEach { line ->
            offsetRegex.find(line)?.let { match ->
                offsetMs = match.groupValues.getOrNull(1)?.toLongOrNull() ?: 0L
            }

            val matches = timestampRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach

            val text = line.replace(timestampRegex, "").trim()
            if (text.isBlank()) return@forEach

            matches.forEach { m ->
                val min = m.groupValues.getOrNull(1)?.toLongOrNull() ?: 0L
                val sec = m.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
                val fracRaw = m.groupValues.getOrNull(3).orEmpty()
                val millis = when (fracRaw.length) {
                    0 -> 0L
                    1 -> fracRaw.toLongOrNull()?.times(100L) ?: 0L
                    2 -> fracRaw.toLongOrNull()?.times(10L) ?: 0L
                    else -> fracRaw.take(3).toLongOrNull() ?: 0L
                }
                cues += LyricCue(
                    timeMs = min * 60_000L + sec * 1_000L + millis,
                    text = text
                )
            }
        }

    return LrcParseResult(
        cues = cues
            .sortedBy { it.timeMs }
            .distinctBy { it.timeMs to it.text },
        offsetMs = offsetMs
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VinylScreen()
                }
            }
        }
    }
}

@Composable
private fun VinylScreen(vm: VinylViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scratchEngine = remember { ScratchSoundEngine(context) }
    var selectedTheme by rememberSaveable { mutableStateOf(DeckThemeOption.SILVER) }
    val palette = remember(selectedTheme) { paletteFor(selectedTheme) }

    DisposableEffect(Unit) {
        onDispose { scratchEngine.release() }
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
                        uiScale = uiScale,
                        onSaveManualLyrics = vm::saveManualLyrics,
                        onClearManualLyrics = vm::clearManualLyrics
                    )
                    ControlPanel(
                        state = state,
                        palette = palette,
                        uiScale = uiScale,
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it },
                        onOpenSettings = vm::openNotificationAccessSettings,
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
                        albumArt = state.albumArt,
                        palette = palette,
                        themeOption = selectedTheme,
                        uiScale = uiScale,
                        onNeedleProgress = vm::setNeedleProgress,
                        onNeedleRelease = vm::snapNeedleAndControlPlayback,
                        onNeedleScratch = scratchEngine::playScrub
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
                    albumArt = state.albumArt,
                    palette = palette,
                    themeOption = selectedTheme,
                    uiScale = uiScale,
                    onNeedleProgress = vm::setNeedleProgress,
                    onNeedleRelease = vm::snapNeedleAndControlPlayback,
                    onNeedleScratch = scratchEngine::playScrub
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
                    albumArt = state.albumArt,
                    palette = palette,
                    themeOption = selectedTheme,
                    uiScale = uiScale,
                    onNeedleProgress = vm::setNeedleProgress,
                    onNeedleRelease = vm::snapNeedleAndControlPlayback,
                    onNeedleScratch = scratchEngine::playScrub
                )

                NowPlayingPanel(
                    state = state,
                    palette = palette,
                    uiScale = uiScale,
                    onSaveManualLyrics = vm::saveManualLyrics,
                    onClearManualLyrics = vm::clearManualLyrics
                )

                ControlPanel(
                    state = state,
                    palette = palette,
                    uiScale = uiScale,
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it },
                    onOpenSettings = vm::openNotificationAccessSettings,
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
    state: VinylUiState,
    palette: DeckPalette,
    uiScale: Float,
    onSaveManualLyrics: (String) -> Unit,
    onClearManualLyrics: () -> Unit
) {
    val normalizedLyrics = state.lyrics
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
    val appName = state.connectedPackage?.let(::toReadableAppName)
    val lrcParseResult = remember(normalizedLyrics) { parseLrc(normalizedLyrics) }
    val lrcCues = lrcParseResult.cues
    val hasLrcSync = lrcCues.isNotEmpty()
    val lrcPositionMs = (state.positionMs + lrcParseResult.offsetMs).coerceAtLeast(0L)
    val lyricsScroll = rememberScrollState()
    val lrcListState = rememberLazyListState()
    var isEditing by rememberSaveable(state.title, state.artist) { mutableStateOf(false) }
    var editValue by rememberSaveable(state.title, state.artist) { mutableStateOf(normalizedLyrics) }
    val lyricsTargetProgress = if (
        !isEditing &&
        normalizedLyrics.isNotBlank() &&
        state.durationMs > LYRICS_SCROLL_HOLD_MS
    ) {
        val effectivePos = (state.positionMs - LYRICS_SCROLL_HOLD_MS).coerceAtLeast(0L)
        val effectiveDur = (state.durationMs - LYRICS_SCROLL_HOLD_MS).coerceAtLeast(1L)
        (effectivePos.toFloat() / effectiveDur.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val smoothLyricsProgress by animateFloatAsState(
        targetValue = lyricsTargetProgress,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "lyrics-smooth-progress"
    )
    val activeCueIndex = remember(lrcCues, lrcPositionMs) {
        if (lrcCues.isEmpty()) {
            -1
        } else {
            val idx = lrcCues.indexOfLast { lrcPositionMs >= it.timeMs }
            if (idx >= 0) idx else -1
        }
    }

    LaunchedEffect(normalizedLyrics, isEditing) {
        if (!isEditing) {
            editValue = normalizedLyrics
        }
    }
    LaunchedEffect(smoothLyricsProgress, lyricsScroll.maxValue, normalizedLyrics, isEditing) {
        if (
            isEditing ||
            hasLrcSync ||
            normalizedLyrics.isBlank() ||
            lyricsScroll.maxValue <= 0
        ) return@LaunchedEffect
        val target = (lyricsScroll.maxValue * smoothLyricsProgress).toInt()
        if (abs(target - lyricsScroll.value) > 1) {
            lyricsScroll.scrollTo(target)
        }
    }
    LaunchedEffect(activeCueIndex, hasLrcSync, isEditing) {
        if (!hasLrcSync || isEditing || activeCueIndex < 0) return@LaunchedEffect
        val targetItem = (activeCueIndex - 2).coerceAtLeast(0)
        if (abs(lrcListState.firstVisibleItemIndex - targetItem) > 0) {
            lrcListState.animateScrollToItem(targetItem)
        }
    }

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

            if (isEditing) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = scaledDp(130.dp, uiScale, 96.dp), max = scaledDp(240.dp, uiScale, 170.dp)),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = 6,
                    maxLines = 12
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        isEditing = false
                        editValue = normalizedLyrics
                    }) {
                        Text("Cancel", color = palette.subtitleColor)
                    }
                    TextButton(onClick = {
                        onSaveManualLyrics(editValue)
                        isEditing = false
                    }) {
                        Text("Save", color = palette.controlTint)
                    }
                    if (normalizedLyrics.isNotBlank()) {
                        TextButton(onClick = {
                            onClearManualLyrics()
                            isEditing = false
                        }) {
                            Text("Clear", color = palette.subtitleColor)
                        }
                    }
                }
            } else {
                if (normalizedLyrics.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = scaledDp(70.dp, uiScale, 46.dp), max = scaledDp(136.dp, uiScale, 90.dp))
                            .clip(RoundedCornerShape(scaledDp(12.dp, uiScale, 8.dp)))
                            .background(Color.Black.copy(alpha = 0.08f))
                            .padding(scaledDp(10.dp, uiScale, 8.dp))
                            .let { base ->
                                if (hasLrcSync) base else base.verticalScroll(lyricsScroll)
                            }
                    ) {
                        if (hasLrcSync) {
                            LazyColumn(
                                state = lrcListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(lrcCues) { index, cue ->
                                    val isActive = index == activeCueIndex
                                    Text(
                                        text = cue.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) palette.titleColor else palette.bodyColor.copy(alpha = 0.74f),
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = scaledDp(2.dp, uiScale, 1.dp))
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = normalizedLyrics,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.bodyColor
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        editValue = normalizedLyrics
                        isEditing = true
                    }) {
                        Text(
                            text = if (normalizedLyrics.isBlank()) "Add Lyrics" else "Edit Lyrics",
                            color = palette.controlTint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    state: VinylUiState,
    palette: DeckPalette,
    uiScale: Float,
    selectedTheme: DeckThemeOption,
    onThemeSelected: (DeckThemeOption) -> Unit,
    onOpenSettings: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekPreview: (Float) -> Unit,
    onSeekCommit: (Float) -> Unit
) {
    if (!state.hasNotificationAccess) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(scaledDp(16.dp, uiScale, 12.dp)),
            colors = CardDefaults.cardColors(containerColor = palette.cardColor)
        ) {
            Column(modifier = Modifier.padding(scaledDp(12.dp, uiScale, 8.dp))) {
                Text(
                    text = "Notification access is required.",
                    color = palette.bodyColor
                )
                Spacer(modifier = Modifier.height(scaledDp(6.dp, uiScale, 4.dp)))
                Button(onClick = onOpenSettings) {
                    Text("Open Settings")
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
        ThemeChip(
            text = "BRONZE",
            selected = selectedTheme == DeckThemeOption.BRONZE,
            palette = palette,
            uiScale = uiScale,
            onClick = { onThemeSelected(DeckThemeOption.BRONZE) }
        )
    }
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
                text = "Vinyl Remote",
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
                ThemeChip(
                    text = "BRONZE",
                    selected = selectedTheme == DeckThemeOption.BRONZE,
                    palette = palette,
                    uiScale = uiScale,
                    onClick = { onThemeSelected(DeckThemeOption.BRONZE) }
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
    albumArt: Bitmap?,
    palette: DeckPalette,
    themeOption: DeckThemeOption,
    uiScale: Float,
    onNeedleProgress: (Float) -> Unit,
    onNeedleRelease: () -> Unit,
    onNeedleScratch: (Float) -> Unit
) {
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
            uiScale = uiScale
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
            DeckThemeOption.BRONZE -> {
                val lineStep = size.minDimension * 0.018f
                val lineCount = (size.height / lineStep).toInt() + 3
                for (i in 0..lineCount) {
                    val y = i * lineStep
                    val wave = sin(i * 0.31f) * size.width * 0.015f
                    drawLine(
                        color = Color(0x66A36E44),
                        start = Offset(plateInset, y),
                        end = Offset(size.width - plateInset + wave, y + wave * 0.14f),
                        strokeWidth = 1.1f
                    )
                    drawLine(
                        color = Color(0x335A3A24),
                        start = Offset(plateInset, y + lineStep * 0.46f),
                        end = Offset(size.width - plateInset + wave * 0.5f, y + lineStep * 0.46f),
                        strokeWidth = 0.8f
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
    uiScale: Float
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
