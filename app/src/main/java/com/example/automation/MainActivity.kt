package com.example.automation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1️⃣ Request notification permission (Android 13+)
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

        // 2️⃣ Request location permission
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        // 3️⃣ Start foreground service ONLY after permissions
        startForegroundTrackingService()

        // 4️⃣ Test Telegram
        //sendTelegramMessage()
    }

    // 🔹 Permission helpers
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

    // 🔹 Foreground service starter
    private fun startForegroundTrackingService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(intent)
    }

    // 🔹 Handle ALL permissions in ONE place
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty()) return

        when (requestCode) {
            2001 -> { // Notification permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recreate() // restart activity to continue flow
                }
            }

            1001 -> { // Location permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startForegroundTrackingService()
                }
            }
        }
    }

    // 🔹 Telegram test
    private fun sendTelegramMessage() {
        android.util.Log.d("TELEGRAM_TEST", "sendTelegramMessage() called")

        val client = OkHttpClient()

        val body = FormBody.Builder()
            .add("chat_id", TelegramConfig.CHAT_ID)
            .add("text", "Hello from Android 🚀 Telegram works!")
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot${TelegramConfig.BOT_TOKEN}/sendMessage")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.util.Log.e("TELEGRAM_TEST", "Failed", e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                android.util.Log.d("TELEGRAM_TEST", "Success: ${response.code}")
                response.close()
            }
        })
    }
}
