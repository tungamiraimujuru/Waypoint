package com.waypoint.core.data

import com.waypoint.core.domain.model.Itinerary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory ItineraryRepository.
 *
 * Backing field is a StateFlow so observeAll() naturally re-emits
 * on every save. Sorted newest-first by createdAt at read time.
 *
 * Limitations:
 *   - Lost on process death (acceptable for v1; Room replaces this).
 *   - Not concurrency-tested under heavy contention (mobile single-user).
 */
@Singleton
internal class InMemoryItineraryRepository @Inject constructor() : ItineraryRepository {

    private val items = MutableStateFlow<Map<String, Itinerary>>(emptyMap())

    override suspend fun save(itinerary: Itinerary) {
        items.update { current -> current + (itinerary.id to itinerary) }
    }

    override suspend fun get(id: String): Itinerary? = items.value[id]

    override fun observeAll(): Flow<List<Itinerary>> = items
        .asStateFlow()
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { map ->
                    emit(map.values.sortedByDescending { it.createdAt })
                }
            }
        }
}
