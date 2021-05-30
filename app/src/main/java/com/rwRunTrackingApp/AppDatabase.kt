package com.rwRunTrackingApp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(TrackingEntity::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao
}