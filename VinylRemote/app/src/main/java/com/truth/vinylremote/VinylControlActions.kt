package com.truth.vinylremote

import android.content.Context
import android.content.Intent

internal object VinylControlActions {
    const val ACTION_PREV = "com.truth.vinylremote.action.PREV"
    const val ACTION_TOGGLE_PLAY_PAUSE = "com.truth.vinylremote.action.TOGGLE_PLAY_PAUSE"
    const val ACTION_NEXT = "com.truth.vinylremote.action.NEXT"
    const val ACTION_NEEDLE_IN = "com.truth.vinylremote.action.NEEDLE_IN"
    const val ACTION_NEEDLE_OUT = "com.truth.vinylremote.action.NEEDLE_OUT"

    const val NEEDLE_IN = "needle_in"
    const val NEEDLE_OUT = "needle_out"

    private const val PREFS = "vinyl_remote_control_bridge"
    private const val KEY_NEEDLE_SEQ = "needle_seq"
    private const val KEY_NEEDLE_COMMAND = "needle_command"

    data class NeedleCommand(
        val seq: Long,
        val command: String
    )

    fun intent(context: Context, action: String): Intent {
        return Intent(context, VinylControlReceiver::class.java).setAction(action)
    }

    fun writeNeedleCommand(context: Context, command: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seq = prefs.getLong(KEY_NEEDLE_SEQ, 0L) + 1L
        prefs.edit()
            .putLong(KEY_NEEDLE_SEQ, seq)
            .putString(KEY_NEEDLE_COMMAND, command)
            .apply()
    }

    fun readNeedleCommand(context: Context): NeedleCommand {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NeedleCommand(
            seq = prefs.getLong(KEY_NEEDLE_SEQ, 0L),
            command = prefs.getString(KEY_NEEDLE_COMMAND, "").orEmpty()
        )
    }
}
