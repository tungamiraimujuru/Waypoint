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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.waypoint.core.ui.theme.WayPointTheme
import com.waypoint.feature.chat.state.ChatIntent
import com.waypoint.feature.chat.state.ChatMessage
import com.waypoint.feature.chat.state.ChatNavEvent
import com.waypoint.feature.chat.state.ChatUiState
import com.waypoint.feature.chat.state.ItineraryPreview
import com.waypoint.feature.chat.viewmodel.ChatViewModel

@Composable
fun ChatRoute(
    onOpenItinerary: (String) -> Unit,
    onOpenSaved: () -> Unit,
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

    ChatScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenSaved = onOpenSaved
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreen(
    state: ChatUiState,
    onIntent: (ChatIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSaved: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.stream) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val borderColor = Color.Gray.copy(alpha = 0.45f)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                },
                title = {
                    Text(
                        text = "WAYPOINT",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSaved) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Saved trips",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
                    .padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
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
            .padding(top = 64.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Where do you want to go?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
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
        ChatMessage.Role.User -> UserMessage(message = message)
        ChatMessage.Role.Assistant -> AssistantMessage(message)
    }
}

@Composable
private fun UserMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End

    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(
                    start = 16.dp,
                    end = 0.dp
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 4.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 24.dp,
                bottomEnd = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
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
    // Otherwise render nothing — the itinerary card carries the response.
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
                        color = MaterialTheme.colorScheme.primary
                            .copy(alpha = if (i == 0) alpha else alpha * 0.7f),
                        shape = CircleShape
                    )
            )
        }
    }
}

// ───────────────────── itinerary preview card ─────────────────

@Composable
private fun ItineraryPreviewCard(
    preview: ItineraryPreview,
    onOpen: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val accentColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape) // 👈 THIS is the missing piece
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerRadius = 16.dp.toPx()
                val accentWidth = 3.dp.toPx()

                // Rounded border
                drawRoundRect(
                    color = borderColor,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = strokeWidth)
                )

                // Left accent (now clipped correctly)
                drawRect(
                    color = accentColor,
                    size = Size(accentWidth, size.height)
                )
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (preview.isComplete) "ITINERARY" else "ITINERARY · STREAMING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = preview.title ?: "Building your itinerary…",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
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
                Spacer(Modifier.height(20.dp))
                TimelineColumn(days = preview.days)
            }

            AnimatedVisibility(
                visible = preview.isComplete,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    OpenItineraryButton(onClick = onOpen)
                }
            }
        }
    }
}

@Composable
private fun OpenItineraryButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Open full itinerary",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ───────────────────── unified timeline ────────────────────────

/**
 * Renders all days as one unified vertical timeline.
 *
 * The amber spine is a single line drawn once for the whole column —
 * day markers and activity bullets all sit on it. This is what makes
 * the card read as one cohesive timeline rather than independent day
 * sections.
 */
@Composable
private fun TimelineColumn(days: List<Day>) {
    val spineColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // The spine — one continuous line down the whole column,
                // drawn at the centre of the marker hit-area (8dp from left).
                val x = 8.dp.toPx()
                drawLine(
                    color = spineColor.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            days.forEachIndexed { index, day ->
                TimelineDay(
                    day = day,
                    spineColor = spineColor,
                    isFirst = index == 0
                )
            }
        }
    }
}

@Composable
private fun TimelineDay(
    day: Day,
    spineColor: Color,
    isFirst: Boolean
) {
    // A day whose activities haven't arrived yet is "streaming in".
    val isStreamingIn = day.activities.isEmpty() && !isFirst
    val labelAlpha = if (isStreamingIn) 0.45f else 1f

    Column {
        // Day label row, anchored to the spine with a marker.
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayMarker(
                filled = !isStreamingIn,
                color = spineColor
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "DAY ${day.dayNumber}: ${day.summary.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = labelAlpha),
                letterSpacing = 1.2.sp
            )
        }

        if (day.activities.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.padding(start = 28.dp)) {
                ActivityShimmer()
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                day.activities.forEach { activity ->
                    TimelineActivity(activity = activity, spineColor = spineColor)
                }
            }
        }
    }
}

/**
 * The dot on the spine marking the start of a day.
 * Filled = day has arrived. Hollow = upcoming/streaming in.
 */
@Composable
private fun DayMarker(filled: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(
                        width = 1.5.dp,
                        color = color.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
private fun TimelineActivity(activity: Activity, spineColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Small bullet on the spine, vertically aligned with the time text.
        Box(
            modifier = Modifier
                .size(16.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(spineColor.copy(alpha = 0.6f), CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        // Time + content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            if (activity.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivityShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Ask about your trip…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            textStyle = LocalTextStyle.current.merge(
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            maxLines = 4,
                            enabled = !isStreaming,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isStreaming) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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

@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
private fun ChatScreenEmptyPreview() {
    WayPointTheme {
        ChatScreen(state = ChatUiState(), onIntent = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
private fun ChatScreenStreamingPreview() {
    WayPointTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        "u1",
                        ChatMessage.Role.User,
                        "4 days in Cape Town, mid-range budget, hiking and food."
                    ),
                    ChatMessage("a1", ChatMessage.Role.Assistant, "", isStreaming = true)
                ),
                stream = ChatUiState.StreamState.Streaming(""),
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
                                    title = "Table Mountain cable car",
                                    description = "Stunning aerial views of the city and coastline. Quick ascent to the plateau.",
                                    locationName = "Table Mountain",
                                    lat = -33.96, lng = 18.41
                                ),
                                Activity(
                                    time = "13:30",
                                    title = "Lunch at The Test Kitchen",
                                    description = "A curated culinary journey featuring locally sourced ingredients.",
                                    locationName = "Test Kitchen",
                                    lat = null, lng = null
                                )
                            )
                        ),
                        Day(2, "Cape Point & Boulders", emptyList())
                    )
                )
            ),
            onIntent = {}
        )
    }
}