package com.ridetrack.app.data

import com.ridetrack.app.data.db.BikeDao
import com.ridetrack.telemetry.model.Bike
import com.ridetrack.telemetry.model.MountCalibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BikeRepository(private val dao: BikeDao) {
    fun observeBikes(): Flow<List<Bike>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeBike(id: String): Flow<Bike?> = dao.observe(id).map { it?.toModel() }

    suspend fun get(id: String): Bike? = dao.get(id)?.toModel()

    suspend fun save(bike: Bike) = dao.upsert(bike.toEntity())

    suspend fun setCalibration(bikeId: String, calibration: MountCalibration) =
        dao.setCalibration(bikeId, calibration.up.x, calibration.up.y, calibration.up.z, calibration.createdAtMillis)

    /** Bikes with recorded rides are kept so every ride stays associated with its bike. */
    suspend fun deleteIfUnused(id: String): Boolean {
        if (dao.rideCount(id) > 0) return false
        dao.delete(id)
        return true
    }
}
