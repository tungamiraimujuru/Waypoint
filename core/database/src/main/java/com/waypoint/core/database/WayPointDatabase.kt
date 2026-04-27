package com.waypoint.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.waypoint.core.database.dao.ItineraryDao
import com.waypoint.core.database.entity.ItineraryEntity

@Database(
    entities = [ItineraryEntity::class],
    version = 1,
    exportSchema = false   // turn on for production projects; off for v1
)
abstract class WayPointDatabase : RoomDatabase() {
    abstract fun itineraryDao(): ItineraryDao

    companion object {
        const val DB_NAME = "waypoint.db"
    }
}