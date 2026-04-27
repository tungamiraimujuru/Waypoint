package com.waypoint.core.data.mapper

import com.waypoint.core.database.entity.ItineraryEntity
import com.waypoint.core.domain.model.Itinerary
import kotlinx.serialization.json.Json

internal class ItineraryMapper(private val json: Json) {

    fun toEntity(itinerary: Itinerary): ItineraryEntity = ItineraryEntity(
        id = itinerary.id,
        title = itinerary.title,
        destination = itinerary.destination,
        createdAtEpochMillis = itinerary.createdAt.toEpochMilliseconds(),
        payloadJson = json.encodeToString(Itinerary.serializer(), itinerary)
    )

    fun toDomain(entity: ItineraryEntity): Itinerary =
        json.decodeFromString(Itinerary.serializer(), entity.payloadJson)
}