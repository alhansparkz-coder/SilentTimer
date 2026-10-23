package com.namazsilent

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build

/**
 * All the logic: start a silent/vibrate period, remember the previous sound
 * profile, and restore it when the timer ends (or when the user taps "restore").
 */
object SilenceController {

    const val ACTION_RESTORE = "com.namazsilent.ACTION_RESTORE"

    private const val PREFS = "silence_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PREV_MODE = "prev_mode"
    private const val KEY_APPLIED_MODE = "applied_mode"
    private const val KEY_END_AT = "end_at"
    private const val KEY_LAST_SILENT = "last_silent"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPolicyAccess(ctx: Context): Boolean =
        ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun isActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVE, false)

    fun endAt(ctx: Context): Long = prefs(ctx).getLong(KEY_END_AT, 0L)

    fun lastChoiceIsSilent(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LAST_SILENT, false)

    /** Returns false if Do Not Disturb access has not been granted (or the change failed). */
    fun start(ctx: Context, minutes: Int, silent: Boolean): Boolean {
        val app = ctx.applicationContext
        if (!hasPolicyAccess(app)) return false

        val audio = app.getSystemService(AudioManager::class.java)
        val p = prefs(app)
        val target = if (silent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_VIBRATE

        // Remember the profile to go back to, unless a session of ours is already running.
        val ourSessionRunning = p.getBoolean(KEY_ACTIVE, false) &&
            audio.ringerMode == p.getInt(KEY_APPLIED_MODE, -1)
        val previous = if (ourSessionRunning) p.getInt(KEY_PREV_MODE, AudioManager.RINGER_MODE_NORMAL)
                       else audio.ringerMode

        try {
            audio.ringerMode = target
        } catch (e: SecurityException) {
            return false
        }

        val end = System.currentTimeMillis() + minutes * 60_000L
        p.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putInt(KEY_PREV_MODE, previous)
            .putInt(KEY_APPLIED_MODE, target)
            .putLong(KEY_END_AT, end)
            .putBoolean(KEY_LAST_SILENT, silent)
            .apply()

        schedule(app, end)
        SilentWidgetProvider.refreshAll(app)
        return true
    }

    /**
     * Puts the previous sound profile back. If you changed the profile yourself
     * while the timer was running, it is left alone.
     */
    fun restore(ctx: Context) {
        val app = ctx.applicationContext
        val p = prefs(app)
        if (!p.getBoolean(KEY_ACTIVE, false)) return

        val audio = app.getSystemService(AudioManager::class.java)
        val previous = p.getInt(KEY_PREV_MODE, AudioManager.RINGER_MODE_NORMAL)
        val applied = p.getInt(KEY_APPLIED_MODE, -1)

        try {
            if (hasPolicyAccess(app) && audio.ringerMode == applied) {
                audio.ringerMode = previous
            }
        } catch (e: SecurityException) {
            // Access was revoked while the timer ran; nothing more we can do.
        }

        cancelAlarm(app)
        p.edit().putBoolean(KEY_ACTIVE, false).apply()
        SilentWidgetProvider.refreshAll(app)
    }

    /** Called after a reboot. */
    fun onBoot(ctx: Context) {
        val app = ctx.applicationContext
        if (!isActive(app)) return
        val end = endAt(app)
        if (end <= System.currentTimeMillis()) restore(app) else schedule(app, end)
    }

    // ---- alarm handling ----

    private fun restorePendingIntent(ctx: Context, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, RestoreReceiver::class.java).setAction(ACTION_RESTORE)
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** PendingIntent used by the widget when you tap it while a timer is running. */
    fun restoreNowIntent(ctx: Context): PendingIntent = restorePendingIntent(ctx, 2)

    private fun schedule(ctx: Context, endAt: Long) {
        val alarm = ctx.getSystemService(AlarmManager::class.java)
        val pi = restorePendingIntent(ctx, 1)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        try {
            if (canBeExact) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
            }
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
        }
    }

    private fun cancelAlarm(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(restorePendingIntent(ctx, 1))
    }
}
