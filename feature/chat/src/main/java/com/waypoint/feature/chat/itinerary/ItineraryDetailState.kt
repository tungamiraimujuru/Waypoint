package com.waypoint.feature.chat.itinerary

import com.waypoint.core.domain.model.Itinerary

sealed interface ItineraryDetailState {
    data object Loading : ItineraryDetailState
    data object NotFound : ItineraryDetailState
    data class Ready(val itinerary: Itinerary) : ItineraryDetailState
}