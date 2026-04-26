package com.waypoint.core.domain.error

/**
 * Domain-level error taxonomy for AI calls.
 *
 * Every failure mode the UI might handle differently gets its own type:
 *  - Network errors → "we're offline" UI
 *  - RateLimited → "try again in N seconds"
 *  - Parse errors → "something went wrong, the model returned malformed data"
 *  - Cancelled → silent (user-initiated, not really an error)
 *
 * We extend Throwable so it composes with Flow.catch and coroutines'
 * exception machinery. We do NOT extend RuntimeException because we
 * always handle these — they are checked at the boundary, not crashes.
 */
sealed class AiError(
    message: String,
    cause: Throwable? = null
) : Throwable(message, cause) {

    /** User cancelled, navigated away, or otherwise aborted the stream. */
    data object Cancelled : AiError("Stream cancelled")

    /** Device has no network. */
    data object NoNetwork : AiError("No network available")

    /** The HTTP layer returned a non-success status. */
    data class Http(val code: Int) : AiError("HTTP $code")

    /** Server told us to back off. retryAfter is in seconds, may be 0 if unknown. */
    data class RateLimited(val retryAfter: Long) : AiError("Rate limited (retry after ${retryAfter}s)")

    /** The model output couldn't be parsed into an itinerary. snippet is the tail of the buffer for debugging. */
    data class Parse(val snippet: String) : AiError("Malformed model output: $snippet")

    /** Catch-all for anything we didn't model explicitly. */
    data class Unknown(override val cause: Throwable) : AiError("Unknown error: ${cause.message}", cause)
}