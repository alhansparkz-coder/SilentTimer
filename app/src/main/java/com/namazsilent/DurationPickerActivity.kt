package com.namazsilent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import java.util.Date

/** The small pop-up that appears when you tap the widget. */
class DurationPickerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val modeGroup = findViewById<RadioGroup>(R.id.mode_group)
        modeGroup.check(
            if (SilenceController.lastChoiceIsSilent(this)) R.id.mode_silent else R.id.mode_vibrate
        )

        val durations = listOf(
            R.id.btn_15 to 15,
            R.id.btn_20 to 20,
            R.id.btn_30 to 30,
            R.id.btn_60 to 60,
            R.id.btn_120 to 120
        )
        for ((buttonId, minutes) in durations) {
            findViewById<Button>(buttonId).setOnClickListener {
                begin(minutes, modeGroup.checkedRadioButtonId == R.id.mode_silent)
            }
        }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }
    }

    private fun begin(minutes: Int, silent: Boolean) {
        if (!SilenceController.hasPolicyAccess(this)) {
            Toast.makeText(this, R.string.need_access, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        if (SilenceController.start(this, minutes, silent)) {
            val until = DateFormat.getTimeFormat(this).format(Date(SilenceController.endAt(this)))
            val label = getString(if (silent) R.string.mode_silent else R.string.mode_vibrate)
            Toast.makeText(this, getString(R.string.started_toast, label, until), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.start_failed, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
