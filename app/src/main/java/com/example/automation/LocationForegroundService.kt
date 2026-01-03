package com.example.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class LocationForegroundService : Service() {

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        startLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------------- FOREGROUND ---------------- */

    private fun startForegroundServiceWithNotification() {
        val channelId = "location_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Automation Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification: Notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Automation running")
                .setContentText("Monitoring location-based rules")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(1, notification)
        }
    }

    /* ---------------- LOCATION ---------------- */

    private fun startLocationUpdates() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            5 * 60 * 1000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                evaluateRules(
                    LatLng(location.latitude, location.longitude)
                )
            }
        }

        locationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    /* ---------------- RULE ENGINE ---------------- */

    private fun evaluateRules(currentLatLng: LatLng) {
        val rules = RuleStorage.loadRules(this)
        val now = System.currentTimeMillis()

        for (rule in rules) {

            val shouldTrigger = RuleEvaluator.shouldTrigger(
                rule,
                currentLatLng,
                now
            )
            val distanceMeters = android.location.Location.distanceBetween(
                rule.latitude,
                rule.longitude,
                currentLatLng.latitude,
                currentLatLng.longitude,
                FloatArray(1)
            ).let {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    rule.latitude,
                    rule.longitude,
                    currentLatLng.latitude,
                    currentLatLng.longitude,
                    results
                )
                results[0]
            }

            val isInsideNow = distanceMeters <= rule.radiusMeters

            if (shouldTrigger) {
                Log.d("RULE_TRIGGER", "Triggered rule ${rule.id}")
                sendTelegram(rule)
            }

            val updatedRule = RuleEvaluator.updateRuleState(
                rule = rule,
                isInsideNow = isInsideNow,
                triggered = shouldTrigger,
                currentTimeMillis = now
            )

            RuleStorage.saveRule(this, updatedRule)
        }
    }

    /* ---------------- TELEGRAM ---------------- */

    private fun sendTelegram(rule: Rule) {
        val client = OkHttpClient()

        for (chatId in rule.recipientChatIds) {

            val body = FormBody.Builder()
                .add("chat_id", chatId.toString())
                .add("text", rule.message)
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot${TelegramConfig.BOT_TOKEN}/sendMessage")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("TELEGRAM", "Failed to send", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("TELEGRAM", "Sent to $chatId")
                    response.close()
                }
            })
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::locationClient.isInitialized) {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }
}
