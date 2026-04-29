package com.waypoint.core.ai.model

import com.waypoint.core.domain.error.AiError
import com.waypoint.core.domain.model.Itinerary

/**
 * The unified event type emitted from the orchestrator's Flow.
 *
 * Why one Flow and not two:
 *   We want raw tokens (for the chat bubble's typewriter effect) AND
 *   structural events (for the materialising itinerary card) to arrive
 *   in the same stream, in the order the model produced them. Two
 *   separate Flows would race; one Flow with a sealed event type is
 *   trivial to consume in a single `collect`.
 *
 * Why Done and Failed are part of the same hierarchy:
 *   The ViewModel's reducer becomes a single `when` over AiStreamEvent.
 *   That's exhaustive, easy to test, and impossible to forget a case.
 */
sealed interface AiStreamEvent {

    /** Raw text delta from the model — append to the assistant's chat bubble. */
    data class Token(val text: String) : AiStreamEvent

    /** A structural event from the parser — drive itinerary preview updates. */
    data class Structured(val event: ParseEvent) : AiStreamEvent

    /** Terminal success. The complete itinerary, ready to persist. */
    data class Done(val itinerary: Itinerary) : AiStreamEvent

    /** Terminal failure. The UI handles by error type, not by string. */
    data class Failed(val error: AiError) : AiStreamEvent
}
