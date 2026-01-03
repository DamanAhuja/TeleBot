package com.example.automation

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AddRecipientActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etAlias: EditText
    private lateinit var btnVerify: Button
    private lateinit var btnSave: Button

    private var foundChatId: Long? = null
    private var normalizedUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipient)

        etUsername = findViewById(R.id.etUsername)
        etAlias = findViewById(R.id.etAlias)
        btnVerify = findViewById(R.id.btnVerify)
        btnSave = findViewById(R.id.btnSave)

        etAlias.visibility = EditText.GONE
        btnSave.visibility = Button.GONE

        btnVerify.setOnClickListener { verifyUsername() }
        btnSave.setOnClickListener { saveRecipient() }
    }

    private fun verifyUsername() {
        val input = etUsername.text.toString().trim()
        if (input.isEmpty()) {
            toast("Enter username")
            return
        }

        // Normalize username
        normalizedUsername = input.removePrefix("@").lowercase()

        val prefs = getSharedPreferences("recipients", MODE_PRIVATE)
        if (prefs.contains("recipient_$normalizedUsername")) {
            toast("Recipient already added")
            return
        }

        val request = Request.Builder()
            .url("https://api.telegram.org/bot${TelegramConfig.BOT_TOKEN}/getUpdates")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { toast("Network error") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                response.close()

                if (body == null) {
                    runOnUiThread { toast("Empty response") }
                    return
                }

                val json = JSONObject(body)
                val results = json.getJSONArray("result")

                for (i in 0 until results.length()) {
                    val update = results.getJSONObject(i)
                    val message = update.optJSONObject("message") ?: continue
                    val from = message.optJSONObject("from") ?: continue

                    val username = from.optString("username", "").lowercase()
                    if (username == normalizedUsername) {

                        foundChatId = message
                            .getJSONObject("chat")
                            .getLong("id")

                        runOnUiThread {
                            etAlias.visibility = EditText.VISIBLE
                            btnSave.visibility = Button.VISIBLE
                            toast("User verified. Set alias.")
                        }
                        return
                    }
                }

                runOnUiThread {
                    toast("User not found. Ask them to press Start on the bot.")
                }
            }
        })
    }

    private fun saveRecipient() {
        val alias = etAlias.text.toString().trim()
        val chatId = foundChatId

        if (chatId == null) {
            toast("Verify user first")
            return
        }

        if (alias.isEmpty()) {
            toast("Enter alias")
            return
        }

        val prefs = getSharedPreferences("recipients", MODE_PRIVATE)

        val json = JSONObject().apply {
            put("chatId", chatId)
            put("username", normalizedUsername)
            put("alias", alias)
        }

        prefs.edit()
            .putString("recipient_$chatId", json.toString())
            .apply()

        toast("Recipient saved")
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
