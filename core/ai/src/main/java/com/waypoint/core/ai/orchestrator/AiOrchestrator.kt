package com.waypoint.core.ai.orchestrator

import com.waypoint.core.ai.model.AiRequest
import com.waypoint.core.ai.model.AiStreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point into the AI layer.
 *
 * One method. One input type. One output type. Everything else —
 * prompt construction, HTTP, streaming, parsing, retries, error
 * mapping — is internal to the implementation.
 *
 * The Flow is cold: nothing happens until the consumer collects it,
 * and cancelling collection cancels the underlying SSE call.
 *
 * Implementations must:
 *  - Honour cancellation cooperatively (no leaked sockets on cancel).
 *  - Emit exactly one terminal event ([AiStreamEvent.Done] or
 *    [AiStreamEvent.Failed]) before completing the Flow.
 *  - Never throw — all failures collapse into [AiStreamEvent.Failed].
 */
interface AiOrchestrator {
    fun streamItinerary(request: AiRequest): Flow<AiStreamEvent>
}