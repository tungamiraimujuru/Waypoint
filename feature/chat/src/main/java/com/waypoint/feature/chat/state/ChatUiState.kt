package com.waypoint.feature.chat.state

import com.waypoint.core.domain.error.AiError
import com.waypoint.core.domain.model.Day

/**
 * Top-level state for the chat screen.
 *
 * One data class instead of a top-level sealed `UiState` because the
 * chat screen *always* shows messages — it's not a Loading-or-Content
 * choice. Stream state and errors overlay onto persistent message
 * history rather than replacing it.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val stream: StreamState = StreamState.Idle,
    val itineraryPreview: ItineraryPreview? = null,
    val transientError: AiError? = null,
    val savedItineraryId: String? = null
) {
    val canSend: Boolean get() = draft.isNotBlank() && stream is StreamState.Idle

    sealed interface StreamState {
        data object Idle : StreamState
        data object Connecting : StreamState
        data class Streaming(val partialText: String) : StreamState
    }
}

data class ChatMessage(
    val id: String,
    val role: Role,
    val text: String,
    val isStreaming: Boolean = false
) {
    enum class Role { User, Assistant }
}

/**
 * Incrementally-built representation of the itinerary as it streams.
 * Bound by both the chat preview card and the itinerary detail screen.
 *
 * `isComplete = true` indicates the stream finalised successfully —
 * after which we have a real `Itinerary` saved and the UI can offer
 * navigation to the detail screen.
 */
data class ItineraryPreview(
    val title: String? = null,
    val destination: String? = null,
    val days: List<Day> = emptyList(),
    val isComplete: Boolean = false
) {
    val activityCount: Int get() = days.sumOf { it.activities.size }
    val hasContent: Boolean get() = title != null || days.isNotEmpty()
}