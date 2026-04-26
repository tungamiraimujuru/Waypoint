package com.waypoint.core.domain.model

/**
 * Everything the AI needs to know about the user beyond the immediate prompt.
 *
 * Kept deliberately thin — the lead-level discipline is to inject only what's
 * actually used in prompt templates. Anything unused here would just inflate
 * the context window and cost tokens.
 */
data class TripContext(
    val preferences: List<String>,
    val budget: Budget,
    val previousTrips: List<String>
) {
    enum class Budget { Low, Mid, High }

    companion object {
        /** A safe empty context — used by the first session before the user has set anything. */
        val Empty = TripContext(
            preferences = emptyList(),
            budget = Budget.Mid,
            previousTrips = emptyList()
        )
    }
}