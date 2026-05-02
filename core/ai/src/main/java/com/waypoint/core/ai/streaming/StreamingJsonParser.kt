package com.waypoint.core.ai.streaming

import com.waypoint.core.ai.model.ParseEvent
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.Day
import com.waypoint.core.domain.model.Itinerary
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject

/**
 * Incrementally parses Claude's streamed JSON itinerary into domain events.
 *
 * Strategy:
 *   We maintain a buffer that grows as deltas arrive. Once we've stripped
 *   the leading markdown fence (if any) and the root object's opening
 *   brace, every following child object inside the "days" array can be
 *   located by scanning forward for the next `{`. We then find the
 *   matching `}` (depth-tracked, string-literal-safe) and parse the
 *   substring with kotlinx-serialization's tree API.
 *
 * State machine:
 *   - WaitingForRoot  → looking for the opening `{` of the root object
 *   - InsideRoot      → consuming child day objects
 *   - Complete        → root closed, no more events
 *
 * Robustness:
 *   - Tolerates ```json ... ``` markdown fences around the JSON.
 *   - Tolerates whitespace, newlines, and partial deltas at any boundary.
 *   - String literals containing braces are handled correctly.
 *
 * NOT thread-safe — each call to streamItinerary() in the orchestrator
 * gets a fresh instance via [reset].
 */
class StreamingJsonParser @Inject constructor() {

    private val buffer = StringBuilder()
    private var state: State = State.WaitingForRoot
    private var titleResolved = false
    private var currentDayNumber: Int? = null
    private val completedDays = mutableListOf<Day>()
    private var currentDayActivities = mutableListOf<Activity>()
    private var currentDaySummary: String = ""
    private var title: String = ""
    private var destination: String = ""

    fun reset() {
        buffer.clear()
        state = State.WaitingForRoot
        titleResolved = false
        currentDayNumber = null
        completedDays.clear()
        currentDayActivities.clear()
        currentDaySummary = ""
        title = ""
        destination = ""
    }

    fun feed(delta: String): List<ParseEvent> {
        buffer.append(delta)
        val events = mutableListOf<ParseEvent>()

        if (state == State.WaitingForRoot) {
            if (!enterRoot()) return events // need more data
        }

        if (state == State.Complete) return events

        if (!titleResolved) tryEmitTitle(events)

        // Extract any number of complete child objects from the days array.
        while (tryExtractNextChild(events)) { /* loop */ }

        return events
    }

    fun finalize(): Itinerary? {
        // Flush any in-progress day whose activities arrived but whose
        // closing `}` never did (truncated stream — shouldn't happen on
        // clean completion but we'll be tolerant).
        if (currentDayNumber != null && currentDayActivities.isNotEmpty()) {
            completedDays += Day(
                dayNumber = currentDayNumber!!,
                summary = currentDaySummary,
                activities = currentDayActivities.toList(),
            )
        }

        if (title.isBlank() || completedDays.isEmpty()) return null

        return Itinerary(
            id = UUID.randomUUID().toString(),
            title = title,
            destination = destination,
            days = completedDays.toList(),
            createdAt = Clock.System.now(),
        )
    }

    // ─── enter root: skip fence + opening brace ─────────────────────

    /**
     * Strip leading markdown fence (if any) and the root opening `{`.
     * Returns true if we successfully entered root. Returns false if
     * we need more data.
     */
    private fun enterRoot(): Boolean {
        val firstNonWs = buffer.indexOfFirst { !it.isWhitespace() }
        if (firstNonWs < 0) return false

        // Optional ```json fence.
        if (buffer.startsWith("```", firstNonWs)) {
            val newlineIdx = buffer.indexOf('\n', firstNonWs + 3)
            if (newlineIdx < 0) return false
            buffer.delete(0, newlineIdx + 1)
            return enterRoot() // re-attempt with fence stripped
        }

        if (buffer[firstNonWs] != '{') return false // shouldn't happen but defensive

        // Consume the opening `{`. We are now logically inside the root.
        buffer.delete(0, firstNonWs + 1)
        state = State.InsideRoot
        return true
    }

    // ─── title and destination ──────────────────────────────────────

    private fun tryEmitTitle(events: MutableList<ParseEvent>) {
        val titleMatch = TITLE_REGEX.find(buffer) ?: return
        val destMatch = DESTINATION_REGEX.find(buffer) ?: return
        title = titleMatch.groupValues[1]
        destination = destMatch.groupValues[1]
        titleResolved = true
        events += ParseEvent.TitleResolved(title, destination)
    }

    // ─── extract one child object ───────────────────────────────────

    /**
     * Find the next `{` in the buffer (skipping array brackets, commas,
     * and whitespace), then find its matching `}`, then parse and emit.
     * Returns true if we made progress.
     */
    private fun tryExtractNextChild(events: MutableList<ParseEvent>): Boolean {
        val start = findNextChildStart() ?: return false
        val end = findMatchingClose(start) ?: return false

        val objectText = buffer.substring(start, end + 1)
        val parsed = runCatching { Json.parseToJsonElement(objectText).jsonObject }
            .getOrNull()

        if (parsed != null) {
            classifyAndEmit(parsed, events)
        }

        // Always consume — even on parse failure, otherwise we'd loop forever.
        buffer.delete(0, end + 1)
        return true
    }

    private fun classifyAndEmit(obj: JsonObject, events: MutableList<ParseEvent>) {
        when {
            obj["day"] != null && obj["activities"] != null ->
                handleDayObject(obj, events)
            obj["time"] != null && obj["title"] != null ->
                handleActivityObject(obj, events)
            // Unknown shape — silently skip.
        }
    }

    private fun handleDayObject(obj: JsonObject, events: MutableList<ParseEvent>) {
        // Close out any previous day.
        currentDayNumber?.let { prev ->
            completedDays += Day(
                dayNumber = prev,
                summary = currentDaySummary,
                activities = currentDayActivities.toList(),
            )
            currentDayActivities.clear()
        }

        val dayNum = obj["day"]?.jsonPrimitive?.intOrNull ?: return
        val summary = obj["summary"]?.jsonPrimitive?.contentOrNull.orEmpty()

        currentDayNumber = dayNum
        currentDaySummary = summary
        events += ParseEvent.DayStarted(dayNum, summary)

        val activitiesArray = obj["activities"]?.jsonArray ?: return
        for (activityElement in activitiesArray) {
            val activity = parseActivity(activityElement.jsonObject) ?: continue
            currentDayActivities += activity
            events += ParseEvent.ActivityEmitted(dayNum, activity)
        }
    }

    private fun handleActivityObject(obj: JsonObject, events: MutableList<ParseEvent>) {
        val dayNum = currentDayNumber ?: return
        val activity = parseActivity(obj) ?: return
        currentDayActivities += activity
        events += ParseEvent.ActivityEmitted(dayNum, activity)
    }

    private fun parseActivity(obj: JsonObject): Activity? {
        val time = obj["time"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val locationName = obj["locationName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
        val lng = obj["lng"]?.jsonPrimitive?.doubleOrNull
        return Activity(time, title, description, locationName, lat, lng)
    }

    // ─── scanning helpers ───────────────────────────────────────────

    /**
     * Find the next `{` in the buffer at the top level of the root,
     * skipping array brackets, commas, whitespace, and the "days":
     * label that precedes the array. We don't need to track depth here
     * because by definition `tryExtractNextChild` is only called when
     * the buffer starts somewhere inside the root and outside any child.
     *
     * However, we must be careful: the very first scan might encounter
     * `"title":"X","destination":"Y","days":[` before the first child.
     * We handle that by string-aware skipping.
     */
    private fun findNextChildStart(): Int? {
        var i = 0
        var inString = false
        var escape = false
        while (i < buffer.length) {
            val c = buffer[i]
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> return i
                c == '}' -> {
                    // Reached the root's closing brace. We're done.
                    state = State.Complete
                    return null
                }
            }
            i++
        }
        return null
    }

    /**
     * Given the index of an opening `{`, find its matching `}`,
     * tracking nesting depth and string literals.
     */
    private fun findMatchingClose(openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escape = false
        for (i in openIndex until buffer.length) {
            val c = buffer[i]
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private enum class State { WaitingForRoot, InsideRoot, Complete }

    private companion object {
        val TITLE_REGEX = Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val DESTINATION_REGEX = Regex(""""destination"\s*:\s*"((?:[^"\\]|\\.)*)"""")
    }
}
