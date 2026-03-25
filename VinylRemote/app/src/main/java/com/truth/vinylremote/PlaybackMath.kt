package com.truth.vinylremote

internal object PlaybackMath {
    const val NEEDLE_PLAY_START = 0.30f
    const val NEEDLE_PLAY_END = 0.98f

    fun estimatePositionMs(
        basePositionMs: Long,
        lastUpdateElapsedRealtimeMs: Long,
        nowElapsedRealtimeMs: Long,
        playbackSpeed: Float,
        isPlaying: Boolean,
        durationMs: Long
    ): Long {
        val base = basePositionMs.coerceAtLeast(0L)
        if (!isPlaying) return clampPosition(base, durationMs)
        if (lastUpdateElapsedRealtimeMs <= 0L) return clampPosition(base, durationMs)

        val elapsed = (nowElapsedRealtimeMs - lastUpdateElapsedRealtimeMs).coerceAtLeast(0L)
        val speed = if (playbackSpeed == 0f) 1f else playbackSpeed
        val estimated = base + (elapsed * speed).toLong()
        return clampPosition(estimated, durationMs)
    }

    fun clampPosition(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return positionMs.coerceAtLeast(0L)
        return positionMs.coerceIn(0L, durationMs)
    }

    fun needleToTrackFraction(needleProgress: Float): Float {
        val p = needleProgress.coerceIn(NEEDLE_PLAY_START, NEEDLE_PLAY_END)
        return (p - NEEDLE_PLAY_START) / (NEEDLE_PLAY_END - NEEDLE_PLAY_START)
    }

    fun trackFractionToNeedle(trackFraction: Float): Float {
        val f = trackFraction.coerceIn(0f, 1f)
        return NEEDLE_PLAY_START + (NEEDLE_PLAY_END - NEEDLE_PLAY_START) * f
    }
}
