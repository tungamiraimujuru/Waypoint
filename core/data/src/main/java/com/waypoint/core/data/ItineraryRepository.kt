package com.waypoint.core.data

import com.waypoint.core.domain.model.Itinerary
import kotlinx.coroutines.flow.Flow

/**
 * Persists generated itineraries.
 *
 * Implementations may store in-memory, in Room, or remote — the
 * orchestrator and feature modules see only this interface.
 */
interface ItineraryRepository {
    /** Save (or replace) an itinerary. */
    suspend fun save(itinerary: Itinerary)

    /** Fetch one itinerary by id, or null if it doesn't exist. */
    suspend fun get(id: String): Itinerary?

    /** Live-updating list of all saved itineraries, newest first. */
    fun observeAll(): Flow<List<Itinerary>>
}