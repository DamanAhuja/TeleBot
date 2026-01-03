package com.example.automation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import android.widget.EditText
class RecipientsActivity : AppCompatActivity() {

    private lateinit var recipientContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipients)

        recipientContainer = findViewById(R.id.recipientContainer)
        val btnAddRecipient = findViewById<Button>(R.id.btnAddRecipient)

        loadRecipients()

        btnAddRecipient.setOnClickListener {
            startActivity(
                Intent(this, InstructionsActivity::class.java)
            )
        }
    }

    private fun loadRecipients() {
        val prefs = getSharedPreferences("recipients", MODE_PRIVATE)

        recipientContainer.removeAllViews()

        if (prefs.all.isEmpty()) {
            Toast.makeText(this, "No recipients added yet", Toast.LENGTH_SHORT).show()
            return
        }

        for ((_, value) in prefs.all) {
            try {
                val json = JSONObject(value.toString())

                val alias = json.getString("alias")
                val username = json.optString("username", "")

                val tv = TextView(this)
                tv.text = if (username.isNotBlank()) {
                    "$alias (@$username)"
                } else {
                    alias
                }
                tv.textSize = 16f
                tv.setPadding(16, 16, 16, 16)

                tv.setOnClickListener {
                    showEditRecipientDialog(json)
                }

                recipientContainer.addView(tv)


            } catch (_: Exception) {
                // Ignore corrupted entry safely
            }
        }
    }
    private fun showEditRecipientDialog(json: JSONObject) {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_edit_recipient,
            null
        )

        val chatId = json.getLong("chatId")
        val alias = json.getString("alias")
        val username = json.optString("username", "")

        val tvUsername = dialogView.findViewById<TextView>(R.id.tvUsername)
        val etAlias = dialogView.findViewById<EditText>(R.id.etAlias)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        tvUsername.text = if (username.isNotBlank()) "@$username" else "Telegram User"
        etAlias.setText(alias)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val prefs = getSharedPreferences("recipients", MODE_PRIVATE)

        btnSave.setOnClickListener {
            val newAlias = etAlias.text.toString().trim()
            if (newAlias.isEmpty()) {
                Toast.makeText(this, "Alias cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            json.put("alias", newAlias)
            prefs.edit()
                .putString("recipient_$chatId", json.toString())
                .apply()

            dialog.dismiss()
            loadRecipients()
        }

        btnDelete.setOnClickListener {
            prefs.edit()
                .remove("recipient_$chatId")
                .apply()

            dialog.dismiss()
            loadRecipients()
        }

        dialog.show()
    }
}
