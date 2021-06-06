package com.rwRunTrackingApp

import androidx.annotation.WorkerThread
import androidx.lifecycle.asLiveData

class TrackingRepository(private val trackingDao: TrackingDao) {
  val allTrackingEntities = trackingDao.getAllTrackingEntities()
  val lastTrackingEntity = trackingDao.getLastTrackingEntity()
  val totalDistanceTravelled = trackingDao.getTotalDistanceTravelled()

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getLastTrackingEntityRecord() = trackingDao.getLastTrackingEntityRecord()

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun insert(trackingEntity: TrackingEntity) {
    trackingDao.insert(trackingEntity)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAll() {
    trackingDao.deleteAll()
  }
}