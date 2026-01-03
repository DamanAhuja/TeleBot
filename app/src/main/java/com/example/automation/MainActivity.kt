package com.example.automation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rulesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rulesContainer = findViewById(R.id.rulesContainer)

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2001
                )
                return
            }
        }

        // Location permission
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        startForegroundTrackingService()

        findViewById<ImageButton>(R.id.btnRecipients).setOnClickListener {
            startActivity(Intent(this, RecipientsActivity::class.java))
        }

        findViewById<Button>(R.id.btnCreateRule).setOnClickListener {
            startActivity(Intent(this, CreateRuleActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadRules()
    }

    // ───────────────── Permissions ─────────────────

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        when (requestCode) {
            2001 -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) recreate()
            1001 -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startForegroundTrackingService()
        }
    }

    // ───────────────── Foreground Service ─────────────────

    private fun startForegroundTrackingService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(intent)
    }

    // ───────────────── Rules UI ─────────────────

    private fun loadRules() {
        rulesContainer.removeAllViews()

        val rules = RuleStorage.loadRules(this)

        if (rules.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No rules created yet"
            tv.setTextColor(0xFF888888.toInt())
            tv.setPadding(16, 16, 16, 16)
            rulesContainer.addView(tv)
            return
        }

        for (rule in rules) {
            val tv = TextView(this)

            tv.text =
                "📍 ${"%.4f".format(rule.latitude)}, ${"%.4f".format(rule.longitude)}\n" +
                        "📏 Radius: ${rule.radiusMeters} m\n" +
                        "✉️ ${rule.message}\n" +
                        if (rule.enabled) "✅ Enabled" else "⛔ Disabled"

            tv.textSize = 15f
            tv.setPadding(16, 16, 16, 16)

            tv.setOnClickListener {
                showEditRuleDialog(rule)
            }

            rulesContainer.addView(tv)
        }
    }

    // ───────────────── Edit Rule Popup ─────────────────

    private fun showEditRuleDialog(rule: Rule) {

        val view = layoutInflater.inflate(R.layout.dialog_edit_rule, null)

        val switchEnabled = view.findViewById<Switch>(R.id.switchEnabled)
        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        val seekRadius = view.findViewById<SeekBar>(R.id.seekRadius)
        val rgRepeat = view.findViewById<RadioGroup>(R.id.rgRepeat)

        switchEnabled.isChecked = rule.enabled
        etMessage.setText(rule.message)
        seekRadius.progress = rule.radiusMeters

        when (rule.repeatType) {
            RepeatType.ONCE -> rgRepeat.check(R.id.rbOnce)
            RepeatType.EVERY_TIME -> rgRepeat.check(R.id.rbEveryTime)
            RepeatType.DAILY -> rgRepeat.check(R.id.rbDaily)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {

            val newMessage = etMessage.text.toString().trim()
            if (newMessage.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedRule = rule.copy(
                enabled = switchEnabled.isChecked,
                message = newMessage,
                radiusMeters = seekRadius.progress,
                repeatType = when (rgRepeat.checkedRadioButtonId) {
                    R.id.rbEveryTime -> RepeatType.EVERY_TIME
                    R.id.rbDaily -> RepeatType.DAILY
                    else -> RepeatType.ONCE
                }
            )

            RuleStorage.saveRule(this, updatedRule)
            dialog.dismiss()
            loadRules()
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            getSharedPreferences("rules", MODE_PRIVATE)
                .edit()
                .remove("rule_${rule.id}")
                .apply()

            dialog.dismiss()
            loadRules()
        }

        dialog.show()
    }
}
