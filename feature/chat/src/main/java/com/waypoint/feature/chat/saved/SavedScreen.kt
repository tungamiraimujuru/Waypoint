package com.waypoint.feature.chat.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waypoint.core.domain.model.Activity
import com.waypoint.core.domain.model.Day
import com.waypoint.core.domain.model.Itinerary
import com.waypoint.core.ui.theme.WayPointTheme
import com.waypoint.feature.chat.viewmodel.SavedViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Instant

const val SAVED_ROUTE = "saved"

@Composable
fun SavedRoute(onBack: () -> Unit, onOpenItinerary: (String) -> Unit, viewModel: SavedViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavedScreen(
        state = state,
        onBack = onBack,
        onOpenItinerary = onOpenItinerary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedScreen(state: SavedState, onBack: () -> Unit, onOpenItinerary: (String) -> Unit) {
    val borderColor = Color.Gray.copy(alpha = 0.45f)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth,
                    )
                },
                title = {
                    Text(
                        text = "WAYPOINT",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                SavedState.Loading -> {
                    /* trivially fast — no spinner needed */
                }

                SavedState.Empty -> EmptyState()
                is SavedState.Loaded -> ItineraryList(
                    itineraries = state.itineraries.toImmutableList(),
                    onOpenItinerary = onOpenItinerary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SAVED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No saved trips yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Itineraries you generate will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ItineraryList(itineraries: ImmutableList<Itinerary>, onOpenItinerary: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(
                    text = "SAVED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your itineraries",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${itineraries.size} ${if (itineraries.size == 1) "trip" else "trips"} planned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        items(itineraries, key = { it.id }) { itinerary ->
            ItineraryRow(
                itinerary = itinerary,
                onClick = { onOpenItinerary(itinerary.id) },
            )
        }
    }
}

@Composable
private fun ItineraryRow(itinerary: Itinerary, onClick: () -> Unit) {
    val activityCount = itinerary.days.sumOf { it.activities.size }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Amber circle with location pin
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itinerary.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = itinerary.destination,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MetaChip(text = "${itinerary.days.size} days")
                    MetaChip(text = "$activityCount stops")
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
private fun SavedScreenLoadedPreview() {
    WayPointTheme {
        SavedScreen(
            state = SavedState.Loaded(
                listOf(
                    Itinerary(
                        id = "1",
                        title = "4 days in Cape Town",
                        destination = "Cape Town, South Africa",
                        days = listOf(
                            Day(1, "Arrival", List(5) { sampleActivity(it) }),
                            Day(2, "Cape Point", List(6) { sampleActivity(it) }),
                        ),
                        createdAt = Instant.fromEpochSeconds(0),
                    ),
                    Itinerary(
                        id = "2",
                        title = "Weekend in Lisbon",
                        destination = "Lisbon, Portugal",
                        days = listOf(Day(1, "Alfama", List(4) { sampleActivity(it) })),
                        createdAt = Instant.fromEpochSeconds(0),
                    ),
                ),
            ),
            onBack = {},
            onOpenItinerary = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0F11)
@Composable
private fun SavedScreenEmptyPreview() {
    WayPointTheme {
        SavedScreen(state = SavedState.Empty, onBack = {}, onOpenItinerary = {})
    }
}

private fun sampleActivity(seed: Int) = Activity(
    time = "0$seed:00",
    title = "Activity $seed",
    description = "",
    locationName = "Place $seed",
    lat = null,
    lng = null,
)
