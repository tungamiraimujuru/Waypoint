package com.waypoint.core.ai.model

import com.waypoint.core.domain.model.Activity

/**
 * Structural events emitted by the streaming JSON parser as it walks
 * the model's output incrementally.
 *
 * The parser doesn't expose JSON nodes. It exposes domain events:
 * "the title is now resolved", "day 2 has started", "this activity
 * belongs to day 1". The UI binds to these — it never sees JSON.
 *
 * This separation is the single most important architectural decision
 * in the streaming pipeline: the moment we leak `JsonObject` past the
 * parser, the UI layer becomes coupled to the wire format.
 */
sealed interface ParseEvent {

    /** Title and destination have been parsed. Always emitted before any DayStarted. */
    data class TitleResolved(
        val title: String,
        val destination: String
    ) : ParseEvent

    /** A new day's header is complete. Activities for it will follow. */
    data class DayStarted(
        val dayNumber: Int,
        val summary: String
    ) : ParseEvent

    /** A complete activity has been parsed. Belongs to [dayNumber]. */
    data class ActivityEmitted(
        val dayNumber: Int,
        val activity: Activity
    ) : ParseEvent

    /** Terminal — the JSON root object closed cleanly. */
    data object ItineraryComplete : ParseEvent
}