package com.example.automation

import android.app.Application
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                getMapsApiKey()
            )
        }
    }

    private fun getMapsApiKey(): String {
        val ai = packageManager.getApplicationInfo(
            packageName,
            PackageManager.GET_META_DATA
        )
        return ai.metaData.getString("com.google.android.geo.API_KEY")
            ?: error("Maps API key missing")
    }
}
