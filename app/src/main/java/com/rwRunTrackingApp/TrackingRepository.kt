package com.rwRunTrackingApp

import androidx.annotation.WorkerThread
import androidx.lifecycle.asLiveData

class TrackingRepository(private val trackingDao: TrackingDao) {
  val trackingEntityList = trackingDao.getAll()

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