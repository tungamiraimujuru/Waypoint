package com.waypoint.core.domain

import com.google.common.truth.Truth.assertThat
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.ConversationId
import com.waypoint.core.domain.model.Day
import com.waypoint.core.domain.model.Itinerary
import kotlinx.datetime.Instant
import org.junit.Test

class ItineraryTest {

    @Test
    fun `activity hasCoordinates is true only when both lat and lng are present`() {
        val withCoords = sampleActivity(lat = -33.96, lng = 18.41)
        val withoutLat = sampleActivity(lat = null, lng = 18.41)
        val withoutLng = sampleActivity(lat = -33.96, lng = null)
        val withNeither = sampleActivity(lat = null, lng = null)

        assertThat(withCoords.hasCoordinates).isTrue()
        assertThat(withoutLat.hasCoordinates).isFalse()
        assertThat(withoutLng.hasCoordinates).isFalse()
        assertThat(withNeither.hasCoordinates).isFalse()
    }

    @Test
    fun `itinerary copy with new day list produces an independent itinerary`() {
        val original = sampleItinerary()
        val updated = original.copy(days = original.days + sampleDay(dayNumber = 2))

        assertThat(updated.days).hasSize(2)
        assertThat(original.days).hasSize(1) // unchanged
        assertThat(updated.id).isEqualTo(original.id)
    }

    @Test
    fun `ConversationId rejects blank input`() {
        assertThrows(IllegalArgumentException::class.java) {
            ConversationId("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConversationId("   ")
        }
    }

    // --- helpers ---

    private fun sampleActivity(lat: Double? = -33.96, lng: Double? = 18.41) = Activity(
        time = "09:00",
        title = "Table Mountain hike",
        description = "Platteklip Gorge trail",
        locationName = "Platteklip Gorge",
        lat = lat,
        lng = lng,
    )

    private fun sampleDay(dayNumber: Int = 1) = Day(
        dayNumber = dayNumber,
        summary = "Arrival & V&A Waterfront",
        activities = listOf(sampleActivity()),
    )

    private fun sampleItinerary() = Itinerary(
        id = "test-id",
        title = "4 days in Cape Town",
        destination = "Cape Town, South Africa",
        days = listOf(sampleDay()),
        createdAt = Instant.fromEpochSeconds(0),
    )
}

private fun assertThrows(expected: Class<out Throwable>, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        if (expected.isInstance(t)) return
        throw AssertionError("Expected ${expected.simpleName} but got ${t::class.simpleName}", t)
    }
    throw AssertionError("Expected ${expected.simpleName} but no exception was thrown")
}
