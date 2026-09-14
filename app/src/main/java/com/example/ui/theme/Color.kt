package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// EARTHY SAGE THEME — Natural, Organic, Calm, Warm, Premium, Minimal
// =============================================================================
// A single cohesive aesthetic inspired by:
// - Sage leaves, moss, eucalyptus
// - Natural paper, stone, clay, linen
// - Muted botanical colors, soft daylight, earthy ceramics
// =============================================================================

// -----------------------------------------------------------------------------
// BASE PALETTE — Earthy Sage Foundation Colors
// -----------------------------------------------------------------------------

// Primary Background & Surface Colors
val Ivory = Color(0xFFF2F3F1)           // Primary light background, large open surfaces
val Blush = Color(0xFFDCCFBF)           // Warm sandy neutral, secondary surfaces
val SageHint = Color(0xFFBFCFBB)        // Very light sage backgrounds, subtle containers
val Mint = Color(0xFFBFCFBB)            // Light secondary sage (same as SageHint, different semantic role)

// Core Sage Family
val SageLight = Color(0xFF8EA58C)       // Primary interactive elements, active controls
val Sage = Color(0xFFA1A69F)            // Central natural sage, selected states, accents
val Moss = Color(0xFF738A6E)            // Stronger interactive states, borders, icons
val Evergreen = Color(0xFF66756C)       // Deep muted green, strong text, important icons
val EvergreenDeep = Color(0xFF344C3D)   // Strongest text, headings, high-contrast elements

// Accent Colors
val Terracotta = Color(0xFFB37B64)      // Warm earthy accent, important secondary actions

// -----------------------------------------------------------------------------
// SEMANTIC COLOR TOKENS — Light Theme (Default)
// -----------------------------------------------------------------------------

// Background & Surface Tokens
val EarthySageBackground = Ivory
val EarthySageBackgroundSecondary = SageHint
val EarthySageSurface = Ivory
val EarthySageSurfaceElevated = Ivory
val EarthySageSurfaceVariant = SageHint
val EarthySageSurfaceWarm = Blush

// Primary Color Tokens
val EarthySagePrimary = SageLight
val EarthySagePrimaryContainer = SageHint
val EarthySageOnPrimary = EvergreenDeep

// Secondary Color Tokens
val EarthySageSecondary = Moss
val EarthySageSecondaryContainer = Sage
val EarthySageOnSecondary = Ivory

// Tertiary & Accent Tokens
val EarthySageTertiary = Terracotta
val EarthySageAccent = Sage
val EarthySageOnTertiary = Ivory

// Text & Content Tokens
val EarthySageOnBackground = EvergreenDeep
val EarthySageOnSurface = EvergreenDeep
val EarthySageOnSurfaceVariant = Moss
val EarthySageOutline = Moss
val EarthySageOutlineVariant = Sage

// State & Status Tokens
val EarthySageSuccess = SageLight
val EarthySageWarning = Terracotta
val EarthySageError = Terracotta
val EarthySageOverdue = Terracotta
val EarthySageCompleted = Sage
val EarthySagePinned = Evergreen
val EarthySageReminder = Moss

// Timeline Specific Tokens
val EarthySageTimelineLine = Sage
val EarthySageTimelineNodeActive = SageLight
val EarthySageTimelineNodeCompleted = Moss
val EarthySageTimelineCurrentTime = Evergreen
val EarthySageTimelineCardBorder = Sage
val EarthySageTimelineAccentGlow = SageLight

// -----------------------------------------------------------------------------
// SEMANTIC COLOR TOKENS — Dark Theme (Natural Forest Dusk)
// -----------------------------------------------------------------------------

// Dark Background & Surface Colors (derived from Evergreen/Moss/Sage family)
val EarthySageDarkBackground = Color(0xFF1A1F1B)    // Deep forest floor
val EarthySageDarkSurface = Color(0xFF232924)       // Muted earth surface
val EarthySageDarkSurfaceVariant = Color(0xFF2D352E) // Slightly lighter earth
val EarthySageDarkSurfaceElevated = Color(0xFF2A312B)
val EarthySageDarkSurfaceWarm = Color(0xFF2A2520)   // Warm dark earth

// Dark Primary & Secondary
val EarthySageDarkPrimary = SageLight
val EarthySageDarkPrimaryContainer = Moss
val EarthySageDarkOnPrimary = Ivory

val EarthySageDarkSecondary = Sage
val EarthySageDarkSecondaryContainer = Evergreen
val EarthySageDarkOnSecondary = Ivory

// Dark Tertiary & Accent
val EarthySageDarkTertiary = Terracotta
val EarthySageDarkAccent = SageLight
val EarthySageDarkOnTertiary = Ivory

// Dark Text & Content
val EarthySageDarkOnBackground = Color(0xFFE8EBE6)  // Soft ivory text
val EarthySageDarkOnSurface = Color(0xFFE8EBE6)
val EarthySageDarkOnSurfaceVariant = SageHint
val EarthySageDarkOutline = Sage
val EarthySageDarkOutlineVariant = Moss

// Dark State & Status
val EarthySageDarkSuccess = Sage
val EarthySageDarkWarning = Terracotta
val EarthySageDarkError = Terracotta
val EarthySageDarkOverdue = Terracotta
val EarthySageDarkCompleted = Moss
val EarthySageDarkPinned = SageLight
val EarthySageDarkReminder = Sage

// Dark Timeline Tokens
val EarthySageDarkTimelineLine = Evergreen
val EarthySageDarkTimelineNodeActive = SageLight
val EarthySageDarkTimelineNodeCompleted = Moss
val EarthySageDarkTimelineCurrentTime = Sage
val EarthySageDarkTimelineCardBorder = Moss
val EarthySageDarkTimelineAccentGlow = SageLight
