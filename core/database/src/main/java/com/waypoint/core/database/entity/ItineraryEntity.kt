package com.waypoint.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a saved itinerary.
 *
 * Schema choice: we store the full Itinerary tree as a JSON blob
 * rather than normalising into Day/Activity tables. Why:
 *   - We only ever query `getAll()` and `getById()` — no "search by
 *     activity title" or "filter by day count" use cases.
 *   - The Itinerary domain type is already @Serializable, so the
 *     conversion is a one-line kotlinx-serialization call.
 *   - Simpler migrations: schema changes inside the JSON don't
 *     require Room version bumps.
 *
 * If we ever need cross-itinerary queries (e.g. "find activities
 * in Cape Town"), we'd promote the relevant fields to columns.
 */
@Entity(tableName = "itineraries")
data class ItineraryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val createdAtEpochMillis: Long,
    /** Full Itinerary as JSON. Source of truth for non-indexed fields. */
    val payloadJson: String
)