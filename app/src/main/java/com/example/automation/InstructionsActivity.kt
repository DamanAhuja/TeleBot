package com.example.automation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivity(
                Intent(this, AddRecipientActivity::class.java)
            )
        }
    }
}
