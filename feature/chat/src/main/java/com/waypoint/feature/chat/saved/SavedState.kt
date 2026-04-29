package com.waypoint.feature.chat.saved

import com.waypoint.core.domain.model.Itinerary

sealed interface SavedState {
    data object Loading : SavedState
    data object Empty : SavedState
    data class Loaded(val itineraries: List<Itinerary>) : SavedState
}