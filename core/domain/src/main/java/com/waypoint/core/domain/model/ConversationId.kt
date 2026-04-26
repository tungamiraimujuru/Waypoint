package com.waypoint.core.domain.model

import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for a chat conversation identifier.
 *
 * Inline class = zero runtime overhead (the JVM still uses a String),
 * but the compiler refuses to let you pass a raw String where a
 * ConversationId is expected. Cheap insurance against the kind of
 * bug that ships to production.
 */
@JvmInline
value class ConversationId(val value: String) {
    init {
        require(value.isNotBlank()) { "ConversationId cannot be blank" }
    }

    companion object {
        const val DEFAULT_VALUE = "default"
        val Default = ConversationId(DEFAULT_VALUE)
    }
}