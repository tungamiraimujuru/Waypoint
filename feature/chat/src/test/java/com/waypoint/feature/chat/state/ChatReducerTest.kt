package com.waypoint.feature.chat.state

import com.google.common.truth.Truth.assertThat
import com.waypoint.core.ai.model.AiStreamEvent
import com.waypoint.core.ai.model.ParseEvent
import com.waypoint.core.domain.error.AiError
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.Day
import com.waypoint.core.domain.model.Itinerary
import kotlinx.datetime.Instant
import org.junit.Test

class ChatReducerTest {

    private val streamingId = "asst-1"

    private val baseState = ChatUiState(
        messages = listOf(
            ChatMessage("user-1", ChatMessage.Role.User, "4 days in Cape Town"),
            ChatMessage(streamingId, ChatMessage.Role.Assistant, "", isStreaming = true)
        ),
        stream = ChatUiState.StreamState.Connecting,
        itineraryPreview = ItineraryPreview()
    )

    @Test
    fun `Token event appends to streaming message and updates stream state`() {
        val result = reduceStreamEvent(
            baseState,
            AiStreamEvent.Token("Hello"),
            streamingId
        )

        val streamingMsg = result.messages.first { it.id == streamingId }
        assertThat(streamingMsg.text).isEqualTo("Hello")
        assertThat(result.stream).isEqualTo(ChatUiState.StreamState.Streaming("Hello"))
    }

    @Test
    fun `multiple Token events accumulate text in order`() {
        val s1 = reduceStreamEvent(baseState, AiStreamEvent.Token("Hello "), streamingId)
        val s2 = reduceStreamEvent(s1, AiStreamEvent.Token("Cape Town"), streamingId)

        val msg = s2.messages.first { it.id == streamingId }
        assertThat(msg.text).isEqualTo("Hello Cape Town")
    }

    @Test
    fun `TitleResolved structured event populates the preview title`() {
        val event = AiStreamEvent.Structured(
            ParseEvent.TitleResolved("4 days in Cape Town", "Cape Town, SA")
        )

        val result = reduceStreamEvent(baseState, event, streamingId)

        assertThat(result.itineraryPreview?.title).isEqualTo("4 days in Cape Town")
        assertThat(result.itineraryPreview?.destination).isEqualTo("Cape Town, SA")
    }

    @Test
    fun `DayStarted appends an empty day to the preview`() {
        val event = AiStreamEvent.Structured(
            ParseEvent.DayStarted(1, "Arrival & V&A Waterfront")
        )

        val result = reduceStreamEvent(baseState, event, streamingId)

        assertThat(result.itineraryPreview?.days).hasSize(1)
        val day = result.itineraryPreview?.days?.first()
        assertThat(day?.dayNumber).isEqualTo(1)
        assertThat(day?.activities).isEmpty()
    }

    @Test
    fun `ActivityEmitted appends activity to its day`() {
        val withDay = reduceStreamEvent(
            baseState,
            AiStreamEvent.Structured(ParseEvent.DayStarted(1, "Arrival")),
            streamingId
        )
        val withActivity = reduceStreamEvent(
            withDay,
            AiStreamEvent.Structured(
                ParseEvent.ActivityEmitted(
                    dayNumber = 1,
                    activity = sampleActivity()
                )
            ),
            streamingId
        )

        val day = withActivity.itineraryPreview?.days?.first()!!
        assertThat(day.activities).hasSize(1)
        assertThat(day.activities[0].title).isEqualTo("Table Mountain")
    }

    @Test
    fun `Done marks streaming complete, clears stream state, and stores itinerary id`() {
        val itinerary = Itinerary(
            id = "it-1",
            title = "Cape Town",
            destination = "Cape Town, SA",
            days = listOf(Day(1, "Arrival", listOf(sampleActivity()))),
            createdAt = Instant.fromEpochSeconds(0)
        )

        val result = reduceStreamEvent(baseState, AiStreamEvent.Done(itinerary), streamingId)

        val streamingMsg = result.messages.first { it.id == streamingId }
        assertThat(streamingMsg.isStreaming).isFalse()
        assertThat(result.stream).isEqualTo(ChatUiState.StreamState.Idle)
        assertThat(result.itineraryPreview?.isComplete).isTrue()
        assertThat(result.savedItineraryId).isEqualTo("it-1")
    }

    @Test
    fun `Failed event records the error and clears stream state`() {
        val result = reduceStreamEvent(
            baseState,
            AiStreamEvent.Failed(AiError.NoNetwork),
            streamingId
        )

        assertThat(result.transientError).isEqualTo(AiError.NoNetwork)
        assertThat(result.stream).isEqualTo(ChatUiState.StreamState.Idle)
        assertThat(result.messages.first { it.id == streamingId }.isStreaming).isFalse()
    }

    private fun sampleActivity() = Activity(
        time = "09:00",
        title = "Table Mountain",
        description = "Cable car",
        locationName = "Table Mountain",
        lat = -33.96,
        lng = 18.41
    )
}