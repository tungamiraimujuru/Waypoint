package com.waypoint.feature.chat.itinerary

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.Day
import com.waypoint.core.domain.model.Itinerary
import com.waypoint.feature.chat.viewmodel.ItineraryDetailViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

const val ITINERARY_ROUTE_PATTERN = "itinerary/{id}"
const val ITINERARY_ARG_ID = "id"
fun itineraryRoute(id: String) = "itinerary/$id"

@Composable
fun ItineraryDetailRoute(
    onBack: () -> Unit,
    viewModel: ItineraryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        ItineraryDetailState.Loading -> LoadingScreen(onBack = onBack)
        ItineraryDetailState.NotFound -> NotFoundScreen(onBack = onBack)
        is ItineraryDetailState.Ready -> ItineraryDetailScreen(
            itinerary = s.itinerary,
            onBack = onBack
        )
    }
}

// ─── states ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(onBack = onBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotFoundScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(onBack = onBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "We couldn't find that itinerary.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItineraryDetailScreen(
    itinerary: Itinerary,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { ItineraryHeader(itinerary = itinerary) }
            item { ItinerarySummaryChip(itinerary = itinerary) }
            item { Spacer(Modifier.height(4.dp)) }
            item { TimelineColumn(days = itinerary.days.toImmutableList()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    val borderColor = Color.Gray.copy(alpha = 0.45f)
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
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ─── header ────────────────────────────────────────────────────

@Composable
private fun ItineraryHeader(itinerary: Itinerary) {
    Column {
        Text(
            text = "ITINERARY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = itinerary.title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = itinerary.destination,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItinerarySummaryChip(itinerary: Itinerary) {
    val activityCount = itinerary.days.sumOf { it.activities.size }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${itinerary.days.size} days · $activityCount activities",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = itinerary.destination,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── timeline ──────────────────────────────────────────────────

/**
 * Same visual language as the chat preview card's timeline:
 * one continuous amber spine, with day markers and activity bullets
 * pinned to it. The detail screen variant gives each activity its own
 * card-shaped surface for legibility, since this is the long-form view.
 */
@Composable
private fun TimelineColumn(days: ImmutableList<Day>) {
    val spineColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 8.dp.toPx()
                drawLine(
                    color = spineColor.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            days.forEach { day ->
                TimelineDay(day = day, spineColor = spineColor)
            }
        }
    }
}

@Composable
private fun TimelineDay(day: Day, spineColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayMarker(color = spineColor)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "DAY ${day.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.summary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.padding(start = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            day.activities.forEach { activity ->
                ActivityCard(activity = activity)
            }
        }
    }
}

@Composable
private fun DayMarker(color: Color) {
    Box(
        modifier = Modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
private fun ActivityCard(activity: Activity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
            if (activity.locationName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(start = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activity.locationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}