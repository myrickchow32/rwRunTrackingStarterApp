package com.rwRunTrackingApp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Query("SELECT * FROM trackingentity")
    fun getAllTrackingEntities(): Flow<List<TrackingEntity>>

    @Query("SELECT SUM(distanceTravelled) FROM trackingentity")
    fun getTotalDistanceTravelled(): Flow<Float>

    @Query("SELECT * FROM trackingentity ORDER BY timestamp DESC LIMIT 1")
    fun getLastTrackingEntity(): Flow<TrackingEntity?>

    @Query("SELECT * FROM trackingentity ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrackingEntityRecord(): TrackingEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trackingEntity: TrackingEntity)

    @Query("DELETE FROM trackingentity")
    suspend fun deleteAll()
}