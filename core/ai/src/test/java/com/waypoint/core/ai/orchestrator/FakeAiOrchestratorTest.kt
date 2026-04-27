package com.waypoint.core.ai.orchestrator

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.waypoint.core.ai.model.AiRequest
import com.waypoint.core.ai.model.AiStreamEvent
import com.waypoint.core.ai.model.ParseEvent
import com.waypoint.core.domain.error.AiError
import com.waypoint.core.domain.model.ConversationId
import com.waypoint.core.domain.model.Itinerary
import com.waypoint.core.domain.model.TripContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test

class FakeAiOrchestratorTest {

    @Test
    fun `replays scripted events in order`() = runTest {
        val orchestrator = FakeAiOrchestrator(
            script = listOf(
                AiStreamEvent.Token("{"),
                AiStreamEvent.Token("\"title\":\"Cape Town\""),
                AiStreamEvent.Structured(
                    ParseEvent.TitleResolved("Cape Town", "Cape Town, South Africa")
                ),
                AiStreamEvent.Structured(
                    ParseEvent.DayStarted(1, "Arrival")
                ),
                AiStreamEvent.Done(sampleItinerary())
            ),
            interTokenDelayMillis = 0L
        )

        orchestrator.streamItinerary(sampleRequest()).test {
            assertThat((awaitItem() as AiStreamEvent.Token).text).isEqualTo("{")
            assertThat((awaitItem() as AiStreamEvent.Token).text).contains("Cape Town")
            assertThat(awaitItem()).isInstanceOf(AiStreamEvent.Structured::class.java)
            assertThat(awaitItem()).isInstanceOf(AiStreamEvent.Structured::class.java)
            assertThat(awaitItem()).isInstanceOf(AiStreamEvent.Done::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `terminal Failed event ends the stream`() = runTest {
        val orchestrator = FakeAiOrchestrator(
            script = listOf(
                AiStreamEvent.Token("Hello"),
                AiStreamEvent.Failed(AiError.NoNetwork)
            ),
            interTokenDelayMillis = 0L
        )

        orchestrator.streamItinerary(sampleRequest()).test {
            assertThat(awaitItem()).isInstanceOf(AiStreamEvent.Token::class.java)
            val terminal = awaitItem() as AiStreamEvent.Failed
            assertThat(terminal.error).isEqualTo(AiError.NoNetwork)
            awaitComplete()
        }
    }

    // --- helpers ---

    private fun sampleRequest() = AiRequest(
        userPrompt = "test",
        context = TripContext.Empty,
        conversationId = ConversationId.Default
    )

    private fun sampleItinerary() = Itinerary(
        id = "test",
        title = "Test",
        destination = "Test",
        days = emptyList(),
        createdAt = Instant.fromEpochSeconds(0)
    )
}