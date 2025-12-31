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
import kotlin.math.*

class LocationForegroundService : Service() {

    private val COLLEGE_LAT = 28.538921
    private val COLLEGE_LON = 77.255365
    private val RADIUS_METERS = 200

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceWithNotification()
        startLocationUpdates()
    }
    private fun Double.toRadians(): Double = this * Math.PI / 180

    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {

        val earthRadius = 6371000.0 // meters

        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()

        val a = sin(dLat / 2).pow(2.0) +
                cos(lat1.toRadians()) *
                cos(lat2.toRadians()) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceWithNotification() {
        val channelId = "location_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Automation running")
            .setContentText("Tracking location in background")
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
    private fun alreadyNotifiedToday(): Boolean {
        val prefs = getSharedPreferences("arrival_prefs", MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        return prefs.getString("last_sent", "") == today
    }

    private fun markNotifiedToday() {
        val prefs = getSharedPreferences("arrival_prefs", MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        prefs.edit().putString("last_sent", today).apply()
    }
    private fun sendArrivalTelegram() {
        val client = okhttp3.OkHttpClient()

        val body = okhttp3.FormBody.Builder()
            .add("chat_id", TelegramConfig.CHAT_ID)
            .add("text", "Hi Maa ❤️ I’ve reached college safely.")
            .build()

        val request = okhttp3.Request.Builder()
            .url("https://api.telegram.org/bot${TelegramConfig.BOT_TOKEN}/sendMessage")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.util.Log.e("COLLEGE_GEOFENCE", "Telegram failed", e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                android.util.Log.d("COLLEGE_GEOFENCE", "Telegram sent")
                response.close()
            }
        })
    }

    private fun startLocationUpdates() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            5 * 60 * 1000 // every 5 minutes
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                val distance = distanceMeters(
                    location.latitude,
                    location.longitude,
                    COLLEGE_LAT,
                    COLLEGE_LON
                )

                Log.d(
                    "COLLEGE_GEOFENCE",
                    "Distance to college: $distance meters"
                )

                if (distance <= RADIUS_METERS && !alreadyNotifiedToday()) {
                    sendArrivalTelegram()
                    markNotifiedToday()
                }
            }
        }

        locationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationClient.isInitialized) {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }
}
