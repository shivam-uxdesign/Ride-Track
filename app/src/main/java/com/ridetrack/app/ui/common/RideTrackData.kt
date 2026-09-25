package com.ridetrack.app.ui.common

import com.ridetrack.app.ui.components.GeoPoint
import com.ridetrack.telemetry.model.TelemetrySample

/** Route points from samples that actually had a GPS fix. */
fun List<TelemetrySample>.routePoints(): List<GeoPoint> = mapNotNull { s ->
    val lat = s.latitude
    val lon = s.longitude
    if (lat != null && lon != null) GeoPoint(lat, lon) else null
}

/** Index of the sample at or just before [timeMillis] (binary search); -1 when empty. */
fun List<TelemetrySample>.indexAt(timeMillis: Long): Int {
    if (isEmpty()) return -1
    if (timeMillis <= this[0].timeMillis) return 0
    var lo = 0
    var hi = size - 1
    if (timeMillis >= this[hi].timeMillis) return hi
    while (lo < hi) {
        val mid = (lo + hi + 1) ushr 1
        if (this[mid].timeMillis <= timeMillis) lo = mid else hi = mid - 1
    }
    return lo
}

fun List<TelemetrySample>.sampleAt(timeMillis: Long): TelemetrySample? = indexAt(timeMillis).takeIf { it >= 0 }?.let { this[it] }

/** Nearest earlier sample that had a position (for the map marker). */
fun List<TelemetrySample>.positionAt(timeMillis: Long): GeoPoint? {
    for (i in indexAt(timeMillis) downTo 0) {
        val lat = this[i].latitude
        val lon = this[i].longitude
        if (lat != null && lon != null) return GeoPoint(lat, lon)
    }
    return null
}
