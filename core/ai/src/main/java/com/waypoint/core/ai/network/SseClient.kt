package com.waypoint.core.ai.network

import com.waypoint.core.ai.BuildConfig
import com.waypoint.core.ai.prompt.ChatMessage
import com.waypoint.core.domain.error.AiError
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams Anthropic SSE events as raw text deltas.
 *
 * Responsibilities, deliberately narrow:
 *  - Build the HTTP request (auth header, JSON body, stream=true).
 *  - Open the SSE session.
 *  - Parse `data:` payloads of `content_block_delta` events.
 *  - Emit only the text deltas — never JSON, never wire types.
 *  - Map HTTP/network failures into [AiError].
 *
 * Does NOT:
 *  - Build prompts (PromptBuilder does).
 *  - Parse the model's structured output (StreamingJsonParser does, next round).
 *  - Retry (orchestrator does).
 */
@Singleton
internal class SseClient @Inject constructor(private val httpClient: HttpClient, private val json: Json) {

    /**
     * Cold flow of text deltas from Claude.
     *
     * Cancelling the collector cancels the SSE session.
     * Throws [AiError] subtypes on failure — the orchestrator catches
     * and converts to [AiStreamEvent.Failed].
     */
    fun streamCompletion(messages: List<ChatMessage>): Flow<String> = flow {
        val request = messages.toAnthropicRequest()

        try {
            httpClient.sse(
                request = {
                    method = HttpMethod.Post
                    url("${AnthropicApi.BASE_URL}${AnthropicApi.ENDPOINT}")
                    headers {
                        append("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                        append("anthropic-version", AnthropicApi.API_VERSION)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(request)
                },
            ) {
                incoming.collect { event ->
                    val data = event.data ?: return@collect
                    val delta = parseDelta(data)
                    if (!delta.isNullOrEmpty()) emit(delta)
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            android.util.Log.e("SseClient", "SSE error", t)
            throw t.toAiError()
        }
    }

    /**
     * Parse one SSE `data:` line. Most are content_block_delta events
     * containing a text fragment; the rest (message_start, message_stop,
     * ping, etc.) we silently ignore by returning null.
     */
    private fun parseDelta(data: String): String? {
        return try {
            val element = json.parseToJsonElement(data).jsonObject
            val type = element["type"]?.jsonPrimitive?.content ?: return null
            if (type != "content_block_delta") return null

            element["delta"]
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
        } catch (_: Throwable) {
            null
        }
    }
}

private fun Throwable.toAiError(): AiError = when (this) {
    is AiError -> this
    is java.net.UnknownHostException, is java.io.IOException -> AiError.NoNetwork
    else -> AiError.Unknown(this)
}

/**
 * Provides an HttpClient configured for Anthropic SSE.
 * Lives here next to the consumer; small enough not to deserve its own file.
 */
internal fun buildHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
    install(SSE)
    install(ContentNegotiation) {
        json(json)
    }
    engine {
        config {
            // SSE streams can run for many seconds; don't time them out aggressively.
            readTimeout(60_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            connectTimeout(15_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
    }
}

/**
 * The HttpRequestBuilder import didn't expose a top-level `url(String)`
 * helper in older Ktor versions — this small extension keeps the call
 * site readable regardless.
 */
private fun HttpRequestBuilder.url(value: String) {
    url.takeFrom(value)
}

private fun io.ktor.http.URLBuilder.takeFrom(spec: String) {
    val url = io.ktor.http.Url(spec)
    protocol = url.protocol
    host = url.host
    port = url.port
    encodedPath = url.encodedPath
}
