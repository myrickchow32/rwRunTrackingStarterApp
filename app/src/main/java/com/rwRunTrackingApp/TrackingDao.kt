package com.rwRunTrackingApp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Query("SELECT * FROM trackingentity")
    fun getAll(): Flow<List<TrackingEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trackingEntity: TrackingEntity)

    @Query("DELETE FROM trackingentity")
    suspend fun deleteAll()
}