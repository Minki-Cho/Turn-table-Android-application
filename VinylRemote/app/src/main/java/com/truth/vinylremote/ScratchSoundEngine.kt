package com.truth.vinylremote

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

class ScratchSoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var sampleId: Int = 0
    private var sampleReady = false
    private var lastTriggerAtMs = 0L

    init {
        val sampleFile = ensureSampleFile()
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (id == sampleId && status == 0) {
                sampleReady = true
            }
        }
        sampleId = soundPool.load(sampleFile.absolutePath, 1)
    }

    fun playScrub(intensity: Float) {
        if (!sampleReady) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAtMs < 35L) return
        lastTriggerAtMs = now

        val clamped = intensity.coerceIn(0f, 1f)
        val volume = (0.05f + clamped * 0.22f).coerceAtMost(0.34f)
        val rate = 0.7f + clamped * 1.1f
        soundPool.play(sampleId, volume, volume, 1, 0, rate)
    }

    fun release() {
        soundPool.release()
    }

    private fun ensureSampleFile(): File {
        val sampleFile = File(appContext.cacheDir, "vinyl_scratch.wav")
        if (!sampleFile.exists()) {
            sampleFile.writeBytes(buildScratchWave())
        }
        return sampleFile
    }

    private fun buildScratchWave(): ByteArray {
        val sampleRate = 22050
        val durationMs = 130
        val sampleCount = sampleRate * durationMs / 1000
        val pcm = ShortArray(sampleCount)
        val random = Random(19)

        for (i in 0 until sampleCount) {
            val t = i.toFloat() / sampleCount.toFloat()
            val envelope = 1f - t
            val noise = (random.nextFloat() * 2f - 1f) * (0.74f * envelope)
            val grit = if (random.nextFloat() > 0.96f) 0.9f else 0f
            val resonant = kotlin.math.sin(i * 0.11f) * 0.09f
            val sample = (noise + grit + resonant).coerceIn(-1f, 1f)
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        val dataSize = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)

        pcm.forEach { sample -> buffer.putShort(sample) }
        return buffer.array()
    }
}
