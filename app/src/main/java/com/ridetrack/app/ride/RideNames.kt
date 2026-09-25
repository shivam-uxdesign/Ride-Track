package com.ridetrack.app.ride

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object RideNames {
    /** e.g. "Sunday Evening Ride". */
    fun forStart(startMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        val t = Instant.ofEpochMilli(startMillis).atZone(zone)
        val day = t.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val part = when (t.hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        return "$day $part Ride"
    }
}
