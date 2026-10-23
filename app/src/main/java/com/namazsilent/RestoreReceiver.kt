package com.namazsilent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when the timer ends (or when you tap the widget to restore early). */
class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SilenceController.ACTION_RESTORE) {
            SilenceController.restore(context)
        }
    }
}
