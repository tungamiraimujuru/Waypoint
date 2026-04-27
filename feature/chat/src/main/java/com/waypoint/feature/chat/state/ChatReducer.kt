package com.waypoint.feature.chat.state

import com.waypoint.core.ai.model.AiStreamEvent
import com.waypoint.core.ai.model.ParseEvent
import com.waypoint.core.domain.model.Day

/**
 * Pure reducer: applies a streaming event to the current state and
 * returns a new state. No coroutines, no side effects, no I/O.
 *
 * Lives outside the ViewModel so it can be tested in microseconds
 * with a scripted event list — no Android, no scope, no lifecycle.
 */
internal fun reduceStreamEvent(
    state: ChatUiState,
    event: AiStreamEvent,
    streamingMessageId: String
): ChatUiState = when (event) {
    is AiStreamEvent.Token -> {
        val updatedMessages = state.messages.map { msg ->
            if (msg.id == streamingMessageId) msg.copy(text = msg.text + event.text) else msg
        }
        val updatedStream = when (val s = state.stream) {
            ChatUiState.StreamState.Connecting,
            ChatUiState.StreamState.Idle ->
                ChatUiState.StreamState.Streaming(event.text)
            is ChatUiState.StreamState.Streaming ->
                ChatUiState.StreamState.Streaming(s.partialText + event.text)
        }
        state.copy(messages = updatedMessages, stream = updatedStream)
    }

    is AiStreamEvent.Structured ->
        state.copy(itineraryPreview = state.itineraryPreview?.apply(event.event))

    is AiStreamEvent.Done -> state.copy(
        messages = state.messages.map {
            if (it.id == streamingMessageId) it.copy(isStreaming = false) else it
        },
        stream = ChatUiState.StreamState.Idle,
        itineraryPreview = state.itineraryPreview?.copy(isComplete = true),
        savedItineraryId = event.itinerary.id
    )

    is AiStreamEvent.Failed -> state.copy(
        messages = state.messages.map {
            if (it.id == streamingMessageId) it.copy(isStreaming = false) else it
        },
        stream = ChatUiState.StreamState.Idle,
        transientError = event.error
    )
}

/** Apply one ParseEvent to an ItineraryPreview, producing a new preview. */
private fun ItineraryPreview.apply(parseEvent: ParseEvent): ItineraryPreview = when (parseEvent) {
    is ParseEvent.TitleResolved -> copy(
        title = parseEvent.title,
        destination = parseEvent.destination
    )
    is ParseEvent.DayStarted -> copy(
        days = days + Day(
            dayNumber = parseEvent.dayNumber,
            summary = parseEvent.summary,
            activities = emptyList()
        )
    )
    is ParseEvent.ActivityEmitted -> copy(
        days = days.map { d ->
            if (d.dayNumber == parseEvent.dayNumber) {
                d.copy(activities = d.activities + parseEvent.activity)
            } else d
        }
    )
    ParseEvent.ItineraryComplete -> copy(isComplete = true)
}