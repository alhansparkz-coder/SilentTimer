package com.namazsilent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** If the phone restarts mid-timer, finish the job. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SilenceController.onBoot(context)
        }
    }
}
