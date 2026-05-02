package com.waypoint.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waypoint.core.database.entity.ItineraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ItineraryEntity)

    @Query("SELECT * FROM itineraries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ItineraryEntity?

    @Query("SELECT * FROM itineraries ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ItineraryEntity>>

    @Query("DELETE FROM itineraries WHERE id = :id")
    suspend fun delete(id: String)
}
