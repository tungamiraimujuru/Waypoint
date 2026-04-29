package com.waypoint.feature.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waypoint.core.data.ItineraryRepository
import com.waypoint.feature.chat.itinerary.ITINERARY_ARG_ID
import com.waypoint.feature.chat.itinerary.ItineraryDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItineraryDetailViewModel @Inject constructor(
    private val repository: ItineraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow<ItineraryDetailState>(ItineraryDetailState.Loading)
    val state: StateFlow<ItineraryDetailState> = _state.asStateFlow()

    init {
        val id: String = savedStateHandle[ITINERARY_ARG_ID] ?: error("Missing $ITINERARY_ARG_ID")
        viewModelScope.launch {
            val itinerary = repository.get(id)
            _state.value = if (itinerary != null) {
                ItineraryDetailState.Ready(itinerary)
            } else {
                ItineraryDetailState.NotFound
            }
        }
    }
}
