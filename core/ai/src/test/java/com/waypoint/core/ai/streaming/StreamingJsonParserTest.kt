package com.waypoint.core.ai.streaming

import com.google.common.truth.Truth.assertThat
import com.waypoint.core.ai.model.ParseEvent
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class StreamingJsonParserTest {

    private val parser = StreamingJsonParser()

    @Test
    fun `feeds a complete itinerary as one delta - emits all events in order`() {
        val json = """
            {
              "title": "4 days in Cape Town",
              "destination": "Cape Town, South Africa",
              "days": [
                {
                  "day": 1,
                  "summary": "Arrival & V&A Waterfront",
                  "activities": [
                    { "time": "09:00", "title": "Table Mountain", "description": "Cable car",
                      "locationName": "Table Mountain", "lat": -33.96, "lng": 18.41 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val events = parser.feed(json)

        assertThat(events).hasSize(3)
        assertThat(events[0]).isInstanceOf(ParseEvent.TitleResolved::class.java)
        assertThat(events[1]).isInstanceOf(ParseEvent.DayStarted::class.java)
        assertThat(events[2]).isInstanceOf(ParseEvent.ActivityEmitted::class.java)

        val title = events[0] as ParseEvent.TitleResolved
        assertThat(title.title).isEqualTo("4 days in Cape Town")
        assertThat(title.destination).isEqualTo("Cape Town, South Africa")
    }

    @Test
    fun `strips a leading markdown fence`() {
        val wrapped = """
```json
            { "title": "X", "destination": "Y", "days": [] }
```
        """.trimIndent()

        val events = parser.feed(wrapped)
        assertThat(events).contains(ParseEvent.TitleResolved("X", "Y"))
    }

    @Test
    fun `feeding the same content one character at a time produces the same events`() {
        val json = """
            {"title":"X","destination":"Y","days":[
              {"day":1,"summary":"S","activities":[
                {"time":"09:00","title":"T","description":"D","locationName":"L","lat":1.0,"lng":2.0}
              ]}
            ]}
        """.trimIndent()

        val all = mutableListOf<ParseEvent>()
        for (c in json) all += parser.feed(c.toString())

        // Same events as feeding it whole.
        assertThat(all.filterIsInstance<ParseEvent.TitleResolved>()).hasSize(1)
        assertThat(all.filterIsInstance<ParseEvent.DayStarted>()).hasSize(1)
        assertThat(all.filterIsInstance<ParseEvent.ActivityEmitted>()).hasSize(1)
    }

    @Test
    fun `processes the recorded Cape Town fixture - all 23 activities emitted`() {
        val deltas = loadCapeTownFixture()

        val events = mutableListOf<ParseEvent>()
        for (delta in deltas) events += parser.feed(delta)

        val days = events.filterIsInstance<ParseEvent.DayStarted>()
        val activities = events.filterIsInstance<ParseEvent.ActivityEmitted>()

        assertThat(days).hasSize(4)
        assertThat(activities).hasSize(23)

        // Sanity-check ordering: day numbers are 1, 2, 3, 4.
        assertThat(days.map { it.dayNumber }).containsExactly(1, 2, 3, 4).inOrder()

        // The final itinerary builds.
        val itinerary = parser.finalize()!!
        assertThat(itinerary.title).isNotEmpty()
        assertThat(itinerary.days).hasSize(4)
        assertThat(itinerary.days.sumOf { it.activities.size }).isEqualTo(23)
    }

    @Test
    fun `finalize returns null when no days were extracted`() {
        parser.feed("""{"title":"X","destination":"Y","days":[""")
        assertThat(parser.finalize()).isNull()
    }

    /**
     * Read the recorded SSE fixture and extract the text deltas in order.
     * This mirrors what SseClient.parseDelta would have produced.
     */
    private fun loadCapeTownFixture(): List<String> {
        val resource = javaClass.classLoader!!.getResource("fixtures/cape_town_4day.sse")
            ?: error("fixture not found")
        val raw = resource.readText()

        return raw.lineSequence()
            .filter { it.startsWith("data:") }
            .mapNotNull { line ->
                val payload = line.removePrefix("data:").trim()
                runCatching {
                    val event = kotlinx.serialization.json.Json
                        .parseToJsonElement(payload)
                        .jsonObject
                    if (event["type"]?.jsonPrimitive?.content == "content_block_delta") {
                        event["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                    } else null
                }.getOrNull()
            }
            .toList()
    }
}