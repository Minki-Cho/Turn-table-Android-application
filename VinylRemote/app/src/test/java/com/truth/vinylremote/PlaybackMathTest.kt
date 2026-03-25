package com.truth.vinylremote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMathTest {
    @Test
    fun clampPosition_respectsDurationBounds() {
        assertEquals(0L, PlaybackMath.clampPosition(-5L, 120L))
        assertEquals(60L, PlaybackMath.clampPosition(60L, 120L))
        assertEquals(120L, PlaybackMath.clampPosition(999L, 120L))
    }

    @Test
    fun estimatePosition_appliesElapsedAndSpeed_whenPlaying() {
        val estimated = PlaybackMath.estimatePositionMs(
            basePositionMs = 1_000L,
            lastUpdateElapsedRealtimeMs = 10_000L,
            nowElapsedRealtimeMs = 12_000L,
            playbackSpeed = 1.5f,
            isPlaying = true,
            durationMs = 10_000L
        )
        assertEquals(4_000L, estimated)
    }

    @Test
    fun needleFractionMapping_roundTripsWithinTolerance() {
        val needle = PlaybackMath.trackFractionToNeedle(0.42f)
        val fraction = PlaybackMath.needleToTrackFraction(needle)
        assertTrue(kotlin.math.abs(fraction - 0.42f) < 0.0001f)
    }
}
