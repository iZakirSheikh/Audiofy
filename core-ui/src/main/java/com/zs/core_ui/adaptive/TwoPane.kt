@file:OptIn(ExperimentalContracts::class)
@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 03-10-2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core_ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.FabPosition
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.primex.core.thenIf
import com.zs.core_ui.AppTheme
import kotlin.contracts.ExperimentalContracts

private const val TAG = "TwoPane"

/**
 * The empty top-padding
 */
private val PaddingNone = PaddingValues(0.dp)

// The indexes of the content slots
private const val INDEX_CONTENT = 0
private const val INDEX_TOP_BAR = 1
private const val INDEX_FAB = 2
private const val INDEX_DETAILS = 3
private const val INDEX_SCRIM = 4

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

private val DEFAULT_SPACING = 0.dp
private val DEFAULT_SCRIM_COLOR = Color(0xB3000000)

/**
 * A simple slot for holding content within the TwoPane layout.
 *
 * @param content The composable content to be displayed within the slot.
 */
@Composable
private inline fun Slot(content: @Composable () -> Unit) =
    Box(content = { content() })


private val TwoPaneStrategy.scrim get() =
    if (this is StackedTwoPaneStrategy) DEFAULT_SCRIM_COLOR else Color.Transparent

/**
 * A two-pane layout that displays content and details using a configurable strategy.
 *
 * This component offers a flexible approach to displaying content and details within a
 * two-pane structure. It's particularly useful in situations where traditional shared
 * element transitions with dialogs are not suitable, such as when using a
 * [StackedTwoPaneStrategy].
 *
 * **Adaptive Layout:**
 * The layout dynamically adjusts based on the presence of details content. If `details`
 * has a size of 0, it is not rendered, optimizing the layout for single-pane scenarios.
 *
 * **Seamless Integration:**
 * - Supports a top app bar, automatically applying its indent to the content via
 *   [WindowInsets.Companion.contentInsets].
 * - Includes a floating action button (FAB) with customizable positioning relative to
 *   the `content` using [fabPosition].
 *
 * **Customization:**
 * Offers various options for customization, including spacing, background color, content
 * color, scrim color, and FAB position.
 *
 * **Dismissal:**
 * Provides an `onDismissRequest` callback for handling clicks outside the details pane.
 * This callback is only active when details are visible. You can effectively disable
 * this behavior by setting [scrim] to `Color.Transparent` and not providing an
 * `onDismissRequest` lambda.
 *
 * @param content The primary content to be displayed. This content occupies at least the
 *   fraction specified in the strategy when the details pane is visible. Otherwise, it
 *   fills the available space.
 * @param modifier Modifier to be applied to the layout.
 * @param spacing The spacing between the main content and details pane for horizontal or
 *   vertical strategies. This parameter is ignored for stacked strategies or when no
 *   details are present.
 * @param background The background color of the layout.
 * @param onColor The content color of this layout, typically used for text or icons.
 * @param scrim The color of the scrim overlay applied to the `content` when details are
 *   visible. Set to `Color.Transparent` to disable the scrim.
 * @param details The optional details content to be displayed. When present, it occupies
 *   at most the remaining space after the content pane's minimum allocation.
 * @param topBar The composable content of the top app bar.
 * @param floatingActionButton The composable content of the floating action button.
 * @param fabPosition The position of the floating action button relative to the content.
 * @param strategy The strategy to use for positioning the details pane relative to the
 *   content.
 * @param onDismissRequest Called when the user clicks outside the details pane. This
 *   callback is only invoked when details are visible.
 */
@Composable
fun TwoPane(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = DEFAULT_SPACING,
    background: Color = AppTheme.colors.background,
    onColor: Color = AppTheme.colors.onBackground,
    strategy: TwoPaneStrategy = VerticalTwoPaneStrategy(0.5f),
    scrim: Color = strategy.scrim,
    details: @Composable () -> Unit = { },
    topBar: @Composable () -> Unit = { },
    floatingActionButton: @Composable () -> Unit = { },
    onDismissRequest: (() -> Unit)? = null,
    fabPosition: FabPosition = FabPosition.End,
) {
    // The indent propagated through window.contentIndent
    // The removes its old value; which means child has access to only topBar indent.
    val (indent, onIndentUpdated) = remember { mutableStateOf(PaddingNone) }
    Layout(
        modifier = modifier
            .background(background)
            .fillMaxSize(),
        content = {
            // Content (index 0)
            Slot {
                CompositionLocalProvider(
                    LocalContentColor provides onColor,
                    LocalContentInsets provides indent,
                    content = content
                )
            }
            // Top Bar (index 1)
            Slot(topBar)
            // Floating Action Button (index 2)
            Slot(floatingActionButton)
            // Details (index 3)
            Slot(details)
            // Scrim
            // Consume interactions to prevent clicks passing through the scrim
            Spacer(
                Modifier
                    .thenIf(onDismissRequest != null) {
                        clickable(null, null, onClick = onDismissRequest!!)
                    }
                    .background(scrim)
            )
        },
        measurePolicy = remember(strategy) {
            when (strategy) {
                is VerticalTwoPaneStrategy -> TwoPaneVerticalMeasurePolicy(strategy, fabPosition, spacing, onIndentUpdated)
                is HorizontalTwoPaneStrategy -> TwoPaneHorizontalMeasurePolicy(strategy, fabPosition, spacing, onIndentUpdated)
                is StackedTwoPaneStrategy -> StackedMeasurePolicy(strategy, fabPosition, onIndentUpdated)
            }
        }
    )
}

/**
 * A [MeasurePolicy] That places details below content if available
 */
private data class TwoPaneVerticalMeasurePolicy(
    private val strategy: VerticalTwoPaneStrategy,
    private val fabPosition: FabPosition,
    private val spacing: Dp,
    private val onUpdateIntent: (PaddingValues) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        c: Constraints
    ): MeasureResult {
        val width = c.maxWidth;
        val height = c.maxHeight
        // measure content with original coordinates.
        // Loose constraints for initial measurements
        var constraints = c.copy(0, minHeight = 0)
        val topBarPlaceable = measurables[INDEX_TOP_BAR].measure(constraints)
        val fabPlaceable = measurables[INDEX_FAB].measure(constraints)
        // Determine the vertical split point for the layout.
        // Both content and details will have half of the gapWidth subtracted from their maximum heights
        // to ensure proper spacing.
        val splitAtY = strategy.calculate(IntSize(width, height))
        val gapWidthPx = spacing.roundToPx()
        // Measure Details Pane (if present), limiting its maximum height to the space below the split point
        val detailsMaxHeight = height - splitAtY + gapWidthPx / 2
        constraints = c.copy(minWidth = 0, minHeight = 0, maxHeight = detailsMaxHeight)
        val detailsPlaceable = measurables[INDEX_DETAILS].measure(constraints)
        // Check if details pane is effectively absent (null or zero height)
        val isDetailsAbsent = detailsPlaceable.height == 0
        // Measure Content, dynamically allocating space based on the presence and size of the details pane.
        // If details are absent, content gets the full available space.
        // If details are present, content height is limited to either the space above the split
        // or the actual height of the details pane, whichever is greater.
        // This ensures content fills the available space efficiently, even if the details pane
        // doesn't fully utilize its allocated area.
        val contentAllocatedWidth = splitAtY - gapWidthPx / 2
        val detailsMeasured = detailsPlaceable.height + gapWidthPx / 2
        val remaining = maxOf(contentAllocatedWidth, height - detailsMeasured)
        constraints = if (isDetailsAbsent) c else
            c.copy(minHeight = remaining, maxHeight = remaining)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        val scrimPlaceable = measurables[INDEX_SCRIM].measure(constraints)

        // Update content insets to account for Top Bar height
        onUpdateIntent(PaddingValues(top = topBarPlaceable.height.toDp()))
        return layout(width, height) {
            // place the content at top
            contentPlaceable.placeRelative(0, 0)
            // place topBar at top the content
            topBarPlaceable.placeRelative(0, 0)
            // place fab according to fabPosition.
            val fabSpacingPx = FabSpacing.roundToPx()
            fabPlaceable.placeRelative(
                y = contentPlaceable.height - fabPlaceable.height - fabSpacingPx,
                x = when (fabPosition) {
                    FabPosition.End -> contentPlaceable.width - fabPlaceable.width - fabSpacingPx
                    FabPosition.Center -> (contentPlaceable.width - fabPlaceable.width) / 2
                    FabPosition.Start -> fabSpacingPx
                    else -> error("Invalid fab position")
                }
            )
            if (isDetailsAbsent) return@layout
            // overlay content with scrim
            scrimPlaceable.placeRelative(0, 0)
            // after the gap place the details at the bottom of the content
            detailsPlaceable.placeRelative(
                width / 2 - detailsPlaceable.width / 2,
                height - detailsPlaceable.height
            )
        }
    }
}

/**
 * A [MeasurePolicy] That places details to the right of content if available
 */
private data class TwoPaneHorizontalMeasurePolicy(
    private val strategy: HorizontalTwoPaneStrategy,
    private val fabPosition: FabPosition,
    private val spacing: Dp,
    private val onUpdateIntent: (PaddingValues) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        c: Constraints
    ): MeasureResult {
        val width = c.maxWidth;
        val height = c.maxHeight
        // Determine the horizontal split point for the layout.
        // Both content and details will have half of the gapWidth subtracted from their maximum widths
        // to ensure proper spacing.
        val splitAtX = strategy.calculate(IntSize(width, height))
        val gapWidthPx = spacing.roundToPx()
        // measure details first
        // Measure Details Pane (if present), limiting its maximum height to the space below the split point
        val detailsMaxWidth = width - splitAtX + gapWidthPx / 2
        var constraints = c.copy(0, minHeight = 0, maxWidth = detailsMaxWidth)
        val detailsPlaceable = measurables[INDEX_DETAILS].measure(constraints)
        // Check if details pane is effectively absent (null or zero height)
        val isDetailsAbsent = detailsPlaceable.height == 0
        // measure others
        // Others are restricted to the size of the remaining space.
        val contentAllocatedWidth = splitAtX - gapWidthPx / 2
        // if details has shorter width than suggested; then content will take that space.
        val remaining = maxOf(contentAllocatedWidth, detailsPlaceable.measuredWidth + gapWidthPx /2)
        constraints = if (isDetailsAbsent) c else c.copy(maxWidth = remaining, minWidth = remaining)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        val scrimPlaceable = measurables[INDEX_SCRIM].measure(constraints)
        constraints = constraints.copy(minHeight = 0, minWidth = 0)
        val topBarPlaceable = measurables[INDEX_TOP_BAR].measure(constraints)
        val fabPlaceable = measurables[INDEX_FAB].measure(constraints)
        // Update content insets to account for Top Bar height
        onUpdateIntent(PaddingValues(top = topBarPlaceable.height.toDp()))
        return layout(width, height){
            // place the content at top
            contentPlaceable.placeRelative(0, 0)
            // place topBar at top the content
            topBarPlaceable.placeRelative(0, 0)
            // place fab according to fabPosition.
            val fabSpacingPx = FabSpacing.roundToPx()
            fabPlaceable.placeRelative(
                y = contentPlaceable.height - fabPlaceable.height - fabSpacingPx,
                x = when (fabPosition) {
                    FabPosition.End -> contentPlaceable.width - fabPlaceable.width - fabSpacingPx
                    FabPosition.Center -> (contentPlaceable.width - fabPlaceable.width) / 2
                    FabPosition.Start -> fabSpacingPx
                    else -> error("Invalid fab position")
                }
            )
            if (isDetailsAbsent) return@layout
            // overlay content with scrim
            scrimPlaceable.placeRelative(0, 0)
            // after the gap place the details at the bottom of the content
            detailsPlaceable.placeRelative(
                contentPlaceable.width + gapWidthPx / 2,
                0
            )
        }
    }
}

/**
 * A [MeasurePolicy] That places details stacked on top of content.
 */
private data class StackedMeasurePolicy(
    private val strategy: StackedTwoPaneStrategy,
    private val fabPosition: FabPosition,
    private val onUpdateIntent: (PaddingValues) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        c: Constraints
    ): MeasureResult {
        val width = c.maxWidth;
        val height = c.maxHeight
        // measure content with original coordinates.
        // Loose constraints for initial measurements
        val contentPlaceable = measurables[INDEX_CONTENT].measure(c)
        val scrimPlaceable = measurables[INDEX_SCRIM].measure(c)
        val constraints = c.copy(0, minHeight = 0)
        val topBarPlaceable = measurables[INDEX_TOP_BAR].measure(constraints)
        val fabPlaceable = measurables[INDEX_FAB].measure(constraints)
        val detailsPlaceable = measurables[INDEX_DETAILS].measure(constraints)
        // Check if details pane is effectively absent (null or zero height)
        val isDetailsAbsent = detailsPlaceable.height == 0
        // Update content insets to account for Top Bar height
        onUpdateIntent(PaddingValues(top = topBarPlaceable.height.toDp()))
        // since details is stacked on top of content; hence it will not affect anyone
        // here strategy is only used to find where details need to be placed
        // Update content insets to account for Top Bar height
        return layout(width, height){
            // place the content at top
            contentPlaceable.placeRelative(0, 0)
            // place topBar at top the content
            topBarPlaceable.placeRelative(0, 0)
            // place fab according to fabPosition.
            val fabSpacingPx = FabSpacing.roundToPx()
            fabPlaceable.placeRelative(
                y = contentPlaceable.height - fabPlaceable.height - fabSpacingPx,
                x = when (fabPosition) {
                    FabPosition.End -> contentPlaceable.width - fabPlaceable.width - fabSpacingPx
                    FabPosition.Center -> (contentPlaceable.width - fabPlaceable.width) / 2
                    FabPosition.Start -> fabSpacingPx
                    else -> error("Invalid fab position")
                }
            )

            if (isDetailsAbsent) return@layout
            // overlay content with scrim
            scrimPlaceable.placeRelative(0, 0)
            // after the gap place the details at the bottom of the content
            val y = strategy.calculate(IntSize(width, height)) - detailsPlaceable.height / 2
            detailsPlaceable.placeRelative(
                width / 2 - detailsPlaceable.width / 2,
                y.coerceIn(0..height - detailsPlaceable.height)
            )
        }
    }
}