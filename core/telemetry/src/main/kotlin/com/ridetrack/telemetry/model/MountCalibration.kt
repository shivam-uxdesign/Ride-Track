package com.ridetrack.telemetry.model

import com.ridetrack.telemetry.math.Vec3

/**
 * Phone mounting offset captured while the motorcycle is upright and stationary.
 *
 * All vectors are unit vectors expressed in the phone's sensor frame. [up] is measured;
 * [forward] is derived from the phone's back-facing normal (the screen faces the rider),
 * projected onto the plane perpendicular to [up].
 */
data class MountCalibration(
    val up: Vec3,
    val createdAtMillis: Long,
) {
    val forward: Vec3
    val right: Vec3

    init {
        val u = up.normalized()
        val back = Vec3(0.0, 0.0, -1.0)
        var f = back - u * (u dot back)
        if (f.norm < 0.3) {
            // Phone lies (nearly) flat: assume its top edge points forward instead.
            val top = Vec3.Y
            f = top - u * (u dot top)
        }
        forward = f.normalized()
        right = (forward cross u).normalized()
    }
}
