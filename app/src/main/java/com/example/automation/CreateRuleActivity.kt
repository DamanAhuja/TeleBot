package com.example.automation

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

private var selectedLat: Double? = null
private var selectedLng: Double? = null
private var selectedRadius: Int = 200

class CreateRuleActivity : AppCompatActivity() {

    private lateinit var recipientContainer: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var rgRepeat: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_rule)

        recipientContainer = findViewById(R.id.recipientCheckboxContainer)
        etMessage = findViewById(R.id.etRuleMessage)
        rgRepeat = findViewById(R.id.rgRepeat)

        loadRecipients()

        findViewById<Button>(R.id.btnPickLocation).setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            locationPickerLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            createRule()
        }
    }

    private val locationPickerLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                selectedLat = data.getDoubleExtra("lat", 0.0)
                selectedLng = data.getDoubleExtra("lng", 0.0)
                selectedRadius = data.getIntExtra("radius", 200)

                findViewById<TextView>(R.id.tvLocationSummary).text =
                    "📍 Lat: ${"%.4f".format(selectedLat)}, " +
                            "Lng: ${"%.4f".format(selectedLng)}\n" +
                            "Radius: $selectedRadius m"
            }
        }

    private fun loadRecipients() {
        val prefs = getSharedPreferences("recipients", MODE_PRIVATE)

        if (prefs.all.isEmpty()) {
            Toast.makeText(
                this,
                "Add recipients before creating a rule",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        for ((_, value) in prefs.all) {
            val json = org.json.JSONObject(value.toString())

            val chatId = json.getLong("chatId")
            val alias = json.getString("alias")
            val username = json.optString("username", "")

            val cb = CheckBox(this)
            cb.text = if (username.isNotBlank()) {
                "$alias (@$username)"
            } else {
                alias
            }

            cb.tag = chatId          // ✅ STORE CHAT ID
            cb.textSize = 16f

            recipientContainer.addView(cb)
        }
    }

    private fun createRule() {

        val selectedChatIds = mutableListOf<Long>()

        for (i in 0 until recipientContainer.childCount) {
            val view = recipientContainer.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                selectedChatIds.add(view.tag as Long)
            }
        }

        if (selectedChatIds.isEmpty()) {
            Toast.makeText(this, "Select at least one recipient", Toast.LENGTH_SHORT).show()
            return
        }

        val message = etMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = selectedLat ?: run {
            Toast.makeText(this, "Pick a location", Toast.LENGTH_SHORT).show()
            return
        }
        val lng = selectedLng ?: run {
            Toast.makeText(this, "Pick a location", Toast.LENGTH_SHORT).show()
            return
        }

        val repeatType = when (rgRepeat.checkedRadioButtonId) {
            R.id.rbEveryTime -> RepeatType.EVERY_TIME
            R.id.rbDaily -> RepeatType.DAILY
            else -> RepeatType.ONCE
        }

        val rule = Rule(
            id = System.currentTimeMillis(),
            recipientChatIds = selectedChatIds,
            locationName = "Selected location",
            latitude = lat,
            longitude = lng,
            radiusMeters = selectedRadius,
            message = message,
            repeatType = repeatType
        )

        RuleStorage.saveRule(this, rule)

        Toast.makeText(this, "Rule created", Toast.LENGTH_SHORT).show()
        finish()
    }
}
