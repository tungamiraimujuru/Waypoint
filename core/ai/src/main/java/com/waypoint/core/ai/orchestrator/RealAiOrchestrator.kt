package com.waypoint.core.ai.orchestrator

import com.waypoint.core.ai.model.AiRequest
import com.waypoint.core.ai.model.AiStreamEvent
import com.waypoint.core.ai.network.SseClient
import com.waypoint.core.ai.prompt.PromptBuilder
import com.waypoint.core.ai.streaming.StreamingJsonParser
import com.waypoint.core.domain.error.AiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [AiOrchestrator] backed by Anthropic's streaming API.
 *
 * The pipeline:
 *   request                     → PromptBuilder
 *   prompt                      → SseClient.streamCompletion → Flow<String>
 *   each text delta             → emit Token + feed StreamingJsonParser
 *   each parsed structural unit → emit Structured(ParseEvent)
 *   stream completion           → parser.finalize() → emit Done(Itinerary)
 *   any failure                 → emit Failed(AiError) (never throws past Flow)
 *
 * Cancellation: cooperative via the underlying coroutine; collecting
 * caller cancels Flow → SseClient.streamCompletion completes → we exit.
 *
 * Retries: bounded (max 2) and only on transient errors. Network
 * blips and 5xx responses retry; parse errors and 4xx do not.
 */
@Singleton
internal class RealAiOrchestrator @Inject constructor(
    private val promptBuilder: PromptBuilder,
    private val sseClient: SseClient,
    private val parser: StreamingJsonParser,
) : AiOrchestrator {

    override fun streamItinerary(request: AiRequest): Flow<AiStreamEvent> = flow {
        parser.reset()

        val messages = promptBuilder.buildItineraryMessages(
            userPrompt = request.userPrompt,
            context = request.context,
        )

        sseClient.streamCompletion(messages).collect { delta ->
            // Always emit the raw token first — chat bubble updates immediately.
            emit(AiStreamEvent.Token(delta))

            // Feed the parser; emit any structural events that became available.
            val parseEvents = parser.feed(delta)
            for (parseEvent in parseEvents) {
                emit(AiStreamEvent.Structured(parseEvent))
            }
        }

        // Stream ended cleanly. Try to finalise an itinerary.
        val itinerary = parser.finalize()
            ?: throw AiError.Parse(
                snippet = "Stream completed but no valid itinerary could be parsed",
            )
        emit(AiStreamEvent.Done(itinerary))
    }
        .retryWhen { cause, attempt -> cause.isTransient() && attempt < MAX_RETRY_ATTEMPTS }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(AiStreamEvent.Failed(cause.toAiError()))
        }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 2L
    }
}

private fun Throwable.isTransient(): Boolean = when (this) {
    is IOException -> true
    is AiError.Http -> code in 500..599
    is AiError.RateLimited -> true
    else -> false
}

private fun Throwable.toAiError(): AiError = when (this) {
    is AiError -> this
    is IOException -> AiError.NoNetwork
    else -> AiError.Unknown(this)
}
