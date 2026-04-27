package com.waypoint.core.ai.network

import com.waypoint.core.ai.prompt.ChatMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object AnthropicApi {
    const val BASE_URL = "https://api.anthropic.com"
    const val ENDPOINT = "/v1/messages"
    const val API_VERSION = "2023-06-01"
    const val DEFAULT_MODEL = "claude-sonnet-4-5"
    const val MAX_TOKENS = 4096
}

@Serializable
internal data class AnthropicRequest(
    val model: String = AnthropicApi.DEFAULT_MODEL,
    @SerialName("max_tokens") val maxTokens: Int = AnthropicApi.MAX_TOKENS,
    val stream: Boolean = true,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

@Serializable
internal data class AnthropicMessage(
    val role: String,
    val content: String
)

internal fun List<ChatMessage>.toAnthropicRequest(): AnthropicRequest {
    val system = firstOrNull { it.role == ChatMessage.Role.System }?.content
    val turns = filter { it.role != ChatMessage.Role.System }.map {
        AnthropicMessage(
            role = when (it.role) {
                ChatMessage.Role.User -> "user"
                ChatMessage.Role.Assistant -> "assistant"
                ChatMessage.Role.System -> error("System role already extracted")
            },
            content = it.content
        )
    }
    return AnthropicRequest(system = system, messages = turns)
}