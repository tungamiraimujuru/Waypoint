package com.waypoint.core.ai.prompt

import com.google.common.truth.Truth.assertThat
import com.waypoint.core.domain.model.TripContext
import org.junit.Test

class PromptBuilderTest {

    private val builder = PromptBuilder()

    @Test
    fun `builds two messages - system first, user second`() {
        val messages = builder.buildItineraryMessages(
            userPrompt = "4 days in Cape Town",
            context = TripContext.Empty
        )

        assertThat(messages).hasSize(2)
        assertThat(messages[0].role).isEqualTo(ChatMessage.Role.System)
        assertThat(messages[1].role).isEqualTo(ChatMessage.Role.User)
    }

    @Test
    fun `system prompt forbids markdown fences`() {
        val messages = builder.buildItineraryMessages("any", TripContext.Empty)
        val system = messages.first { it.role == ChatMessage.Role.System }

        assertThat(system.content).contains("DO NOT wrap")
        assertThat(system.content).contains("```")
    }

    @Test
    fun `user block embeds the prompt`() {
        val messages = builder.buildItineraryMessages(
            userPrompt = "4 days in Cape Town, hiking and food",
            context = TripContext.Empty
        )
        val user = messages.first { it.role == ChatMessage.Role.User }

        assertThat(user.content).contains("4 days in Cape Town, hiking and food")
        assertThat(user.content).contains("USER_REQUEST")
    }

    @Test
    fun `user block embeds preferences when present`() {
        val context = TripContext(
            preferences = listOf("hiking", "food"),
            budget = TripContext.Budget.Mid,
            previousTrips = emptyList()
        )

        val messages = builder.buildItineraryMessages("any", context)
        val user = messages.first { it.role == ChatMessage.Role.User }

        assertThat(user.content).contains("hiking, food")
        assertThat(user.content).contains("budget: mid")
        assertThat(user.content).contains("previous trips: none")
    }

    @Test
    fun `user block writes 'none' for empty preferences`() {
        val messages = builder.buildItineraryMessages("any", TripContext.Empty)
        val user = messages.first { it.role == ChatMessage.Role.User }

        assertThat(user.content).contains("preferences: none")
    }
}