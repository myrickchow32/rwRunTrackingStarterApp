package com.rwRunTrackingApp

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class MapsActivityViewModel(val trackingRepository: TrackingRepository): ViewModel() {
  val allTrackingEntities: LiveData<List<TrackingEntity>> = trackingRepository.allTrackingEntities.asLiveData()
  val lastTrackingEntity: LiveData<TrackingEntity?> = trackingRepository.lastTrackingEntity.asLiveData()
  val totalDistanceTravelled: LiveData<Float?> = trackingRepository.totalDistanceTravelled.asLiveData()
  val currentNumberOfStepCount = MutableLiveData(0)
  var initialStepCount = 0

  fun insert(trackingEntity: TrackingEntity) = viewModelScope.launch {
    trackingRepository.getLastTrackingEntityRecord()?.let {
      trackingEntity.distanceTravelled = trackingEntity.distanceTo(it)
    }
    trackingRepository.insert(trackingEntity)
  }

  fun deleteAllTrackingEntity() = viewModelScope.launch {
    currentNumberOfStepCount.value = 0
    initialStepCount = 0
    trackingRepository.deleteAll()
  }
}