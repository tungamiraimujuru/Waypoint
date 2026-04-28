package com.waypoint.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WayPoint design tokens — amber + dark.
 *
 * Color philosophy: warm primary (sunset/golden hour) on near-black
 * surfaces, with a cool tertiary blue as accent contrast. Inspired by
 * editorial travel publications: Cereal magazine, Kinfolk, Off Assignment.
 *
 * Token names are literal (Amber500, Surface950) rather than semantic
 * (Primary, Background) so we can compose two semantic schemes from
 * one token set if we ever add light mode in v2.
 */

// ─── primary ramp (warm amber / saffron) ──────────────────────
internal val Amber50  = Color(0xFFFFF7E5)
internal val Amber100 = Color(0xFFFFE9B8)
internal val Amber200 = Color(0xFFFCD78A)
internal val Amber300 = Color(0xFFF5C161)
internal val Amber400 = Color(0xFFEFB04A)
internal val Amber500 = Color(0xFFE8A63C)   // hero
internal val Amber600 = Color(0xFFC78B2C)
internal val Amber700 = Color(0xFF9E6E22)
internal val Amber800 = Color(0xFF6E4D17)
internal val Amber900 = Color(0xFF3D2C0E)

// ─── secondary ramp (warm gold / tan, more muted) ─────────────
internal val Gold50  = Color(0xFFF5EBD8)
internal val Gold200 = Color(0xFFD9BC8A)
internal val Gold500 = Color(0xFF917246)   // hero
internal val Gold700 = Color(0xFF614C2E)
internal val Gold900 = Color(0xFF2A2014)

// ─── tertiary ramp (cool sky blue accent — the spice) ─────────
internal val Sky200 = Color(0xFFB5DCFD)
internal val Sky400 = Color(0xFF8AC8FB)
internal val Sky500 = Color(0xFF6EB9FA)   // hero
internal val Sky700 = Color(0xFF447FAE)

// ─── neutrals (near-black surface, warm-tinted) ───────────────
internal val Surface950 = Color(0xFF0E0F11)   // app background
internal val Surface900 = Color(0xFF161719)   // raised surfaces
internal val Surface800 = Color(0xFF1F2023)   // input fields, cards
internal val Surface700 = Color(0xFF2A2C30)   // borders, dividers
internal val Surface500 = Color(0xFF6E7178)   // secondary text
internal val Surface300 = Color(0xFFB6B9BF)   // body text
internal val Surface100 = Color(0xFFE8E9EC)   // primary text on dark
internal val Surface50  = Color(0xFFF6F6F8)   // pure light text

// ─── semantic colors ──────────────────────────────────────────
internal val ErrorRed     = Color(0xFFE5564A)
internal val ErrorRedDark = Color(0xFF3D1815)
internal val SuccessGreen = Color(0xFF6FB66E)