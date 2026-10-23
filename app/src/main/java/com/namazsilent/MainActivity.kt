package com.namazsilent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.Date

/** Setup screen: grant permission, test it, and see how to add the widget. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        findViewById<Button>(R.id.btn_try).setOnClickListener {
            startActivity(Intent(this, DurationPickerActivity::class.java))
        }
        findViewById<Button>(R.id.btn_restore).setOnClickListener {
            SilenceController.restore(this)
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val access = SilenceController.hasPolicyAccess(this)
        findViewById<TextView>(R.id.status_access).setText(
            if (access) R.string.access_ok else R.string.access_missing
        )
        findViewById<Button>(R.id.btn_grant).visibility = if (access) View.GONE else View.VISIBLE

        val active = SilenceController.isActive(this)
        val statusTimer = findViewById<TextView>(R.id.status_timer)
        val restoreBtn = findViewById<Button>(R.id.btn_restore)
        if (active) {
            val until = DateFormat.getTimeFormat(this).format(Date(SilenceController.endAt(this)))
            statusTimer.text = getString(R.string.timer_running, until)
            statusTimer.visibility = View.VISIBLE
            restoreBtn.visibility = View.VISIBLE
        } else {
            statusTimer.visibility = View.GONE
            restoreBtn.visibility = View.GONE
        }
    }
}
