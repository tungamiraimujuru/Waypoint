package com.waypoint.feature.chat.state

/**
 * Every user-driven action on the chat screen, as a sealed type.
 * The ViewModel's `onIntent(...)` is the only entry point for the UI.
 */
sealed interface ChatIntent {
    data class DraftChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data object CancelStream : ChatIntent
    data object RetryLast : ChatIntent
    data object DismissError : ChatIntent
    data object OpenItinerary : ChatIntent
}

/**
 * Navigation events emitted as one-shot signals — separate from state.
 * Collected via a Channel/Flow so they don't replay on configuration change.
 */
sealed interface ChatNavEvent {
    data class OpenItinerary(val itineraryId: String) : ChatNavEvent
}
