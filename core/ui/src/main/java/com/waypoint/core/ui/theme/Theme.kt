package com.waypoint.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The single entry point for WayPoint's visual language.
 *
 * Dark-only by design — itinerary content (food, sunsets, landscapes)
 * reads better on dark surfaces, and committing to one mode lets us
 * design every state deliberately rather than supporting two compromised
 * variants. v2 could add a light scheme by mapping the same tokens
 * differently, without restructuring components.
 */
private val WayPointDarkColors = darkColorScheme(
    // Primary — the warm amber that defines the brand
    primary             = Amber500,
    onPrimary           = Surface950,
    primaryContainer    = Amber900,
    onPrimaryContainer  = Amber200,

    // Secondary — muted gold companion
    secondary           = Gold500,
    onSecondary         = Surface950,
    secondaryContainer  = Gold900,
    onSecondaryContainer = Gold200,

    // Tertiary — cool sky blue, the spice
    tertiary            = Sky500,
    onTertiary          = Surface950,
    tertiaryContainer   = Surface800,
    onTertiaryContainer = Sky200,

    // Backgrounds
    background          = Surface950,
    onBackground        = Surface100,

    // Surfaces (cards, sheets, bars)
    surface             = Surface950,
    onSurface           = Surface100,
    surfaceVariant      = Surface800,
    onSurfaceVariant    = Surface300,

    // Borders, dividers, hairlines
    outline             = Surface700,
    outlineVariant      = Surface800,

    // Errors
    error               = ErrorRed,
    onError             = Surface50,
    errorContainer      = ErrorRedDark,
    onErrorContainer    = Color(0xFFFFD3CF),

    // Inverse (rarely used in dark themes; kept for completeness)
    inverseSurface      = Surface100,
    inverseOnSurface    = Surface950,
    inversePrimary      = Amber700,

    // Scrims
    scrim               = Color(0xCC000000),
)

@Composable
fun WayPointTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WayPointDarkColors,
        typography = WayPointTypography,
        shapes = WayPointShapes,
        content = content
    )
}

private val Color = androidx.compose.ui.graphics.Color