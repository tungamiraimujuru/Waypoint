package com.waypoint.core.ai.prompt

import com.waypoint.core.domain.model.TripContext
import javax.inject.Inject

/**
 * A single message in a chat-style request to Claude.
 * Public because it's the type the orchestrator passes to the SseClient.
 */
data class ChatMessage(val role: Role, val content: String) {
    enum class Role { System, User, Assistant }
}

/**
 * Constructs the message array for a streaming itinerary request.
 *
 * Pure: given the same input, always returns the same output. No
 * side effects, no I/O. This means we can unit-test it cheaply and
 * snapshot the output to detect prompt regressions.
 *
 * Lives behind the orchestrator — the rest of the app never touches
 * prompt strings directly.
 */
class PromptBuilder @Inject constructor() {

    fun buildItineraryMessages(userPrompt: String, context: TripContext): List<ChatMessage> = listOf(
        ChatMessage(
            role = ChatMessage.Role.System,
            content = SystemPrompts.ITINERARY_V2,
        ),
        ChatMessage(
            role = ChatMessage.Role.User,
            content = buildUserBlock(userPrompt, context),
        ),
    )

    private fun buildUserBlock(prompt: String, ctx: TripContext): String = buildString {
        append("USER_REQUEST:\n")
        append(prompt.trim())
        append("\n\n")
        append("TRAVELLER_CONTEXT:\n")
        append("- preferences: ").append(ctx.preferences.joinIfEmpty("none")).append('\n')
        append("- budget: ").append(ctx.budget.name.lowercase()).append('\n')
        append("- previous trips: ").append(ctx.previousTrips.joinIfEmpty("none")).append('\n')
    }

    private fun List<String>.joinIfEmpty(emptyText: String): String = if (isEmpty()) emptyText else joinToString(", ")
}
