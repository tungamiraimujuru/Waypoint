package com.waypoint.core.data

import com.waypoint.core.data.mapper.ItineraryMapper
import com.waypoint.core.database.dao.ItineraryDao
import com.waypoint.core.domain.model.Itinerary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomItineraryRepository @Inject constructor(
    private val dao: ItineraryDao,
    json: Json
) : ItineraryRepository {

    private val mapper = ItineraryMapper(json)

    override suspend fun save(itinerary: Itinerary) {
        dao.upsert(mapper.toEntity(itinerary))
    }

    override suspend fun get(id: String): Itinerary? =
        dao.getById(id)?.let(mapper::toDomain)

    override fun observeAll(): Flow<List<Itinerary>> =
        dao.observeAll().map { entities -> entities.map(mapper::toDomain) }
}