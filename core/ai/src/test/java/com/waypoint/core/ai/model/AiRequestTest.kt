package com.waypoint.core.ai.model

import com.google.common.truth.Truth.assertThat
import com.waypoint.core.domain.model.ConversationId
import com.waypoint.core.domain.model.TripContext
import org.junit.Test

class AiRequestTest {

    @Test
    fun `AiRequest accepts a valid prompt`() {
        val request = AiRequest(
            userPrompt = "4 days in Cape Town",
            context = TripContext.Empty,
            conversationId = ConversationId.Default
        )

        assertThat(request.userPrompt).isEqualTo("4 days in Cape Town")
        assertThat(request.context).isEqualTo(TripContext.Empty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AiRequest rejects an empty prompt`() {
        AiRequest(
            userPrompt = "",
            context = TripContext.Empty,
            conversationId = ConversationId.Default
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `AiRequest rejects a blank prompt`() {
        AiRequest(
            userPrompt = "   \t\n",
            context = TripContext.Empty,
            conversationId = ConversationId.Default
        )
    }
}