package com.ridetrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BikeEntity::class, RideEntity::class, SampleEntity::class, EventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RideTrackDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun rideDao(): RideDao

    companion object {
        fun create(context: Context): RideTrackDatabase =
            Room.databaseBuilder(context, RideTrackDatabase::class.java, "ridetrack.db")
                // WAL keeps frequent small ride writes cheap and durable.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
