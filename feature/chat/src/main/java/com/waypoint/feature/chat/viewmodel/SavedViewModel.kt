package com.waypoint.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waypoint.core.data.ItineraryRepository
import com.waypoint.feature.chat.saved.SavedState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(repository: ItineraryRepository) : ViewModel() {

    val state: StateFlow<SavedState> = flow {
        repository.observeAll().collect { itineraries ->
            emit(
                if (itineraries.isEmpty()) {
                    SavedState.Empty
                } else {
                    SavedState.Loaded(itineraries)
                },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedState.Loading,
    )
}
