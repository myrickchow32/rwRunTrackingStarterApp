package com.rwRunTrackingApp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MapsActivityViewModel(val trackingRepository: TrackingRepository): ViewModel() {
  val allTrackingEntities: LiveData<List<TrackingEntity>> = trackingRepository.trackingEntityList.asLiveData()

  fun insert(trackingEntity: TrackingEntity) = viewModelScope.launch {
    trackingRepository.insert(trackingEntity)
  }

  fun deleteAllTrackingEntity() = viewModelScope.launch {
    trackingRepository.deleteAll()
  }
}