package com.rwRunTrackingApp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackingEntity::class], version = 1)
abstract class TrackingDatabase : RoomDatabase() {

  abstract fun getTrackingDao(): TrackingDao

  companion object {
    @Volatile
    private var INSTANCE: TrackingDatabase? = null

    fun getDatabase(context: Context): TrackingDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE = Room.databaseBuilder(context.applicationContext, TrackingDatabase::class.java,"word_database").build()
        INSTANCE!!
      }
    }
  }
}