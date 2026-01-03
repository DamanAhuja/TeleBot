package com.example.automation

import android.content.Context
import org.json.JSONObject

object RuleStorage {

    fun loadRules(context: Context): List<Rule> {
        val prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)
        val rules = mutableListOf<Rule>()

        for ((key, value) in prefs.all) {
            if (!key.startsWith("rule_")) continue
            val jsonString = value as? String ?: continue

            try {
                val json = JSONObject(jsonString)

                val chatIds = json
                    .getString("recipientChatIds")
                    .split(",")
                    .mapNotNull { it.toLongOrNull() }

                rules.add(
                    Rule(
                        id = json.getLong("id"),
                        recipientChatIds = chatIds,
                        locationName = json.getString("locationName"),
                        latitude = json.getDouble("latitude"),
                        longitude = json.getDouble("longitude"),
                        radiusMeters = json.getInt("radius"),
                        message = json.getString("message"),
                        enabled = json.optBoolean("enabled", true),
                        repeatType = RepeatType.valueOf(
                            json.optString("repeatType", RepeatType.ONCE.name)
                        ),
                        lastTriggeredAt = json.optLong("lastTriggeredAt", 0L),
                        wasInsideLastCheck = json.optBoolean("wasInsideLastCheck", false)
                    )
                )
            } catch (_: Exception) {}
        }
        return rules
    }

    fun saveRule(context: Context, rule: Rule) {
        val prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)

        val json = JSONObject().apply {
            put("id", rule.id)
            put("recipientChatIds", rule.recipientChatIds.joinToString(","))
            put("locationName", rule.locationName)
            put("latitude", rule.latitude)
            put("longitude", rule.longitude)
            put("radius", rule.radiusMeters)
            put("message", rule.message)
            put("enabled", rule.enabled)
            put("repeatType", rule.repeatType.name)
            put("lastTriggeredAt", rule.lastTriggeredAt)
            put("wasInsideLastCheck", rule.wasInsideLastCheck)
        }

        prefs.edit()
            .putString("rule_${rule.id}", json.toString())
            .apply()
    }
}
