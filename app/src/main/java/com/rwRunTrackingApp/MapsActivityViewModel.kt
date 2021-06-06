package com.rwRunTrackingApp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MapsActivityViewModel(val trackingRepository: TrackingRepository): ViewModel() {
  val allTrackingEntities: LiveData<List<TrackingEntity>> = trackingRepository.trackingEntityList.asLiveData()
  val lastTrackingEntity: LiveData<TrackingEntity?> = trackingRepository.lastTrackingEntity.asLiveData()

  fun insert(trackingEntity: TrackingEntity) = viewModelScope.launch {
    val lastTrackingEntityRecord = trackingRepository.getLastTrackingEntityRecord()
    trackingEntity.distanceTravelled = trackingEntity.distanceTo(lastTrackingEntityRecord)
    trackingRepository.insert(trackingEntity)
  }

  fun deleteAllTrackingEntity() = viewModelScope.launch {
    trackingRepository.deleteAll()
  }
}