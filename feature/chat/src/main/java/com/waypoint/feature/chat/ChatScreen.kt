package com.waypoint.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waypoint.core.domain.error.AiError
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.Day
import com.waypoint.feature.chat.state.ChatIntent
import com.waypoint.feature.chat.state.ChatMessage
import com.waypoint.feature.chat.state.ChatNavEvent
import com.waypoint.feature.chat.state.ChatUiState
import com.waypoint.feature.chat.state.ItineraryPreview
import com.waypoint.feature.chat.viewmodel.ChatViewModel

@Composable
fun ChatRoute(
    onOpenItinerary: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is ChatNavEvent.OpenItinerary -> onOpenItinerary(event.itineraryId)
            }
        }
    }

    ChatScreen(state = state, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreen(
    state: ChatUiState,
    onIntent: (ChatIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to the bottom whenever a new message or token arrives.
    LaunchedEffect(state.messages.size, state.stream) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "WayPoint",
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = { /* future: history */ }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                draft = state.draft,
                canSend = state.canSend,
                isStreaming = state.stream !is ChatUiState.StreamState.Idle,
                onDraftChange = { onIntent(ChatIntent.DraftChanged(it)) },
                onSend = { onIntent(ChatIntent.Send) },
                onCancel = { onIntent(ChatIntent.CancelStream) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ErrorBanner(
                error = state.transientError,
                onRetry = { onIntent(ChatIntent.RetryLast) },
                onDismiss = { onIntent(ChatIntent.DismissError) }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item { EmptyState() }
                }

                items(state.messages, key = { it.id }) { message ->
                    MessageRow(message = message)
                }

                state.itineraryPreview
                    ?.takeIf { it.hasContent }
                    ?.let { preview ->
                        item(key = "itinerary-preview") {
                            ItineraryPreviewCard(
                                preview = preview,
                                onOpen = { onIntent(ChatIntent.OpenItinerary) }
                            )
                        }
                    }
            }
        }
    }
}

// ───────────────────────── empty state ─────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Where do you want to go?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tell me a destination, how long you have, and what you love to do.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ───────────────────────── message rows ────────────────────────

@Composable
private fun MessageRow(message: ChatMessage) {
    when (message.role) {
        ChatMessage.Role.User -> UserMessage(message)
        ChatMessage.Role.Assistant -> AssistantMessage(message)
    }
}

@Composable
private fun UserMessage(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage) {
    if (message.text.isEmpty() && message.isStreaming) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TypingIndicator()
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (i == 0) alpha else alpha * 0.7f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )

    Box(
        modifier = Modifier
            .size(width = 2.dp, height = 18.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

// ───────────────────── itinerary preview card ─────────────────

@Composable
private fun ItineraryPreviewCard(
    preview: ItineraryPreview,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (preview.isComplete) "ITINERARY" else "ITINERARY · STREAMING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(6.dp))

            Text(
                text = preview.title ?: "Building your itinerary…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            preview.destination?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (preview.days.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                preview.days.forEach { day ->
                    DayBlock(day = day)
                    Spacer(Modifier.height(8.dp))
                }
            }

            AnimatedVisibility(
                visible = preview.isComplete,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpen) {
                        Text("Open full itinerary")
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBlock(day: Day) {
    Column {
        Text(
            text = "Day ${day.dayNumber} · ${day.summary}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))

        if (day.activities.isEmpty()) {
            ActivityShimmer()
        } else {
            day.activities.forEach { activity ->
                ActivityRow(activity = activity)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = activity.time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(56.dp)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (activity.description.isNotBlank()) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ActivityShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

// ───────────────────────── input bar ───────────────────────────

@Composable
private fun ChatInputBar(
    draft: String,
    canSend: Boolean,
    isStreaming: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your trip…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                enabled = !isStreaming
            )

            if (isStreaming) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ───────────────────────── error banner ────────────────────────

@Composable
private fun ErrorBanner(
    error: AiError?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = error?.userMessage().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                if (error.isRetryable()) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun AiError?.userMessage(): String = when (this) {
    null -> ""
    AiError.NoNetwork -> "No network. Connect and try again."
    is AiError.RateLimited -> "Slow down — try again in a moment."
    is AiError.Http -> "Server hiccup ($code). Try again."
    is AiError.Parse -> "Got a malformed response. Try again."
    AiError.Cancelled -> ""
    is AiError.Unknown -> "Something went wrong: ${cause.message ?: "unknown"}"
}

private fun AiError?.isRetryable(): Boolean = when (this) {
    null, AiError.Cancelled, is AiError.Parse -> false
    else -> true
}

// ───────────────────────── previews ────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatUiState(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenStreamingPreview() {
    MaterialTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage("u1", ChatMessage.Role.User, "4 days in Cape Town"),
                    ChatMessage(
                        "a1",
                        ChatMessage.Role.Assistant,
                        "Here's a 4-day Cape Town plan…",
                        isStreaming = true
                    )
                ),
                stream = ChatUiState.StreamState.Streaming("Here's…"),
                itineraryPreview = ItineraryPreview(
                    title = "4 days in Cape Town",
                    destination = "Cape Town, South Africa",
                    days = listOf(
                        Day(
                            dayNumber = 1,
                            summary = "Arrival & V&A Waterfront",
                            activities = listOf(
                                Activity(
                                    time = "09:00",
                                    title = "Table Mountain hike",
                                    description = "Platteklip Gorge trail",
                                    locationName = "Platteklip Gorge",
                                    lat = -33.96,
                                    lng = 18.41
                                )
                            )
                        ),
                        Day(2, "Cape Point", emptyList())
                    )
                )
            ),
            onIntent = {}
        )
    }
}