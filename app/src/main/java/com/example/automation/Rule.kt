package com.example.automation

data class Rule(
    val id: Long,

    // ✅ SYSTEM IDENTITY (Telegram)
    val recipientChatIds: List<Long>,

    // Location data
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,

    // Message to send
    val message: String,

    val enabled: Boolean = true,

    // Alarm-like behavior
    val repeatType: RepeatType = RepeatType.ONCE,
    val lastTriggeredAt: Long = 0L,
    val wasInsideLastCheck: Boolean = false
)
