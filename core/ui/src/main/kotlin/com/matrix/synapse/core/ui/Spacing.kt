package com.matrix.synapse.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for consistent spacing across the app.
 * Use these instead of hardcoded dp values for screen layout.
 *
 * - [ScreenPadding]: Horizontal (and often vertical) padding for screen content inside Scaffold.
 * - [SectionSpacing]: Vertical spacing between major sections (e.g. 24 dp).
 * - [FieldSpacing]: Spacing between related fields within a section (e.g. 16 dp).
 */
object Spacing {
    /** Horizontal and outer padding for screen content. Use for Scaffold content padding. */
    val ScreenPadding: Dp = 24.dp

    /** Vertical spacing between major sections on a screen. */
    val SectionSpacing: Dp = 24.dp

    /** Spacing between related fields (e.g. form fields, list item internals). */
    val FieldSpacing: Dp = 16.dp

    /** Smaller gap (e.g. between title and subtitle, or tight rows). */
    val TightSpacing: Dp = 8.dp
}
