package com.example.automation

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object RuleEvaluator {

    fun shouldTrigger(
        rule: Rule,
        currentLatLng: LatLng,
        currentTimeMillis: Long
    ): Boolean {

        if (!rule.enabled) return false

        val distance = distanceMeters(
            rule.latitude,
            rule.longitude,
            currentLatLng.latitude,
            currentLatLng.longitude
        )

        val isInsideNow = distance <= rule.radiusMeters
        val wasInsideBefore = rule.wasInsideLastCheck

        // ENTER condition
        val entered = !wasInsideBefore && isInsideNow
        if (!entered) return false

        return when (rule.repeatType) {

            RepeatType.ONCE ->
                rule.lastTriggeredAt == 0L

            RepeatType.EVERY_TIME ->
                true

            RepeatType.DAILY ->
                !isSameDay(rule.lastTriggeredAt, currentTimeMillis)
        }
    }

    fun updateRuleState(
        rule: Rule,
        isInsideNow: Boolean,
        triggered: Boolean,
        currentTimeMillis: Long
    ): Rule {
        return rule.copy(
            wasInsideLastCheck = isInsideNow,
            lastTriggeredAt = if (triggered) currentTimeMillis else rule.lastTriggeredAt
        )
    }

    // --- helpers ---

    private fun distanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val dayMillis = 24 * 60 * 60 * 1000L
        return t1 / dayMillis == t2 / dayMillis
    }
}
