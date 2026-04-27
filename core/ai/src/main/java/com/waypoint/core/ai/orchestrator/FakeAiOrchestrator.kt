package com.waypoint.core.ai.orchestrator

import com.waypoint.core.ai.model.AiRequest
import com.waypoint.core.ai.model.AiStreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * A scripted [AiOrchestrator] that replays a predetermined event sequence.
 *
 * Use cases:
 *  1. Unit tests of the ChatViewModel reducer — feed a known event
 *     sequence, assert UiState transitions.
 *  2. UI development of the chat screen before the real orchestrator
 *     is wired — we can iterate on the streaming animations against
 *     a deterministic stream.
 *  3. Demo mode if the network fails during an interview — swap in
 *     this orchestrator and the demo continues running offline.
 *
 * The [interTokenDelay] makes streaming feel realistic in dev. Set
 * to Duration.ZERO in tests for instant playback.
 */
class FakeAiOrchestrator @Inject constructor(
    private val script: List<AiStreamEvent>,
    private val interTokenDelayMillis: Long = 30L
) : AiOrchestrator {

    override fun streamItinerary(request: AiRequest): Flow<AiStreamEvent> = flow {
        for (event in script) {
            emit(event)
            // Simulate network latency between events. Tokens feel
            // typewriter-paced; structural events arrive between bursts.
            if (interTokenDelayMillis > 0) delay(interTokenDelayMillis)
        }
    }
}