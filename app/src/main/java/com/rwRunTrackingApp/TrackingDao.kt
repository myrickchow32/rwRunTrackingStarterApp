package com.rwRunTrackingApp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrackingDao {
    @Query("SELECT * FROM trackingentity")
    fun getAll(): List<TrackingEntity>

    @Insert
    fun insert(trackingEntity: TrackingEntity)

    @Query("DELETE FROM trackingentity")
    fun delete()
}