package com.waypoint.core.ai.model

import com.waypoint.core.domain.model.ConversationId
import com.waypoint.core.domain.model.TripContext

data class AiRequest(
    val userPrompt: String,
    val context: TripContext,
    val conversationId: ConversationId
) {
    init {
        require(userPrompt.isNotBlank()) { "userPrompt cannot be blank" }
    }
}