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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.FabPosition
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
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
import com.zs.core_ui.AppTheme
import kotlin.contracts.ExperimentalContracts
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.runtime.CompositionLocalProvider as Provider
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "TwoPane"

/**
 * The empty top-padding
 */
private val PaddingNone = CP.None
private val DEFAULT_SPACING = 0.dp

// The indexes of the content slots
private const val INDEX_CONTENT = 0
private const val INDEX_TOP_BAR = 1
private const val INDEX_FAB = 2
private const val INDEX_DETAILS = 3
private const val INDEX_DIALOG = 4

// FAB spacing above the bottom bar / bottom of the Scaffold
private val FabSpacing = 16.dp

/**
 * A simple slot for holding content within the TwoPane layout.
 *
 * @param content The composable content to be displayed within the slot.
 */
@Composable
private inline fun Slot(content: @Composable () -> Unit) = Box(content = { content() })

/**
 * A composable function that represents a layout for organizing screen components.
 *
 * This layout provides a flexible structure for displaying primary content,
 * secondary content (optional), dialogs (optional), a top app bar (optional),
 * and a floating action button (optional). It adapts to different screen sizes
 * and orientations using the provided `TwoPaneStrategy`.
 *
 * @param primary The primary content to be displayed within the layout.
 * @param modifier The modifier to be applied to the layout. Defaults to `Modifier`.
 * @param spacing The spacing between layout elements. Defaults to `DEFAULT_SPACING`.
 * @param background The background color of the layout. Defaults to `AppTheme.colors.background`.
 * @param onColor The color of content displayed on the background. Defaults to `AppTheme.colors.onBackground`.
 * @param strategy The strategy to use for displaying content in different panes. Defaults to `SinglePaneStrategy`.
 * @param secondary The secondary content to be displayed, if applicable. Defaults to an empty composable function.
 * @param dialog An optional composable function representing a dialog to be displayed. Defaults to `null`.
 * @param topBar An optional composable function representing the top app bar. Defaults to an empty composable function.
 * @param floatingActionButton An optional composable function representing the floating action button. Defaults to an empty composable function.
 * @param fabPosition The position of the floating action button. Defaults to `FabPosition.End`.
 */
@Composable
fun TwoPane(
    primary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = DEFAULT_SPACING,
    background: Color = AppTheme.colors.background,
    onColor: Color = AppTheme.colors.onBackground,
    strategy: TwoPaneStrategy = SinglePaneStrategy,
    secondary: @Composable () -> Unit = { },
    dialog: @Composable (() -> Unit)? = null,
    topBar: @Composable () -> Unit = { },
    floatingActionButton: @Composable () -> Unit = { },
    fabPosition: FabPosition = FabPosition.End,
) {
    // The indent propagated through window.contentIndent
    // The removes its old value; which means child has access to only topBar indent.
    val (indent, onIndentUpdated) = remember { mutableStateOf(CP.None) }
    //
    Layout(
        modifier = modifier.background(background).fillMaxSize(),
        measurePolicy = remember(strategy) {
            when (strategy) {
                is SinglePaneStrategy -> OnePaneMeasurePolicy(fabPosition, onIndentUpdated)
                is VerticalTwoPaneStrategy -> TwoPaneVerticalMeasurePolicy(strategy, fabPosition, spacing, onIndentUpdated)
                is HorizontalTwoPaneStrategy -> TwoPaneHorizontalMeasurePolicy(strategy, fabPosition, spacing, onIndentUpdated)
                else -> TODO("Add more strategies")
            }
        },
        content = {
            // Content (index 0)
            Slot {
                Provider(
                    LocalContentColor provides onColor,
                    LocalContentInsets provides indent,
                    content = primary
                )
            }
            // Top Bar (index 1)
            Slot(topBar)
            // Floating Action Button (index 2)
            Slot(floatingActionButton)
            if (strategy == SinglePaneStrategy) {
                // dialog at 3 - If using SinglePaneStrategy, display the dialog here
                dialog?.invoke()
                return@Layout
            }
            // Details (index 3) -  The details pane (for multi-pane layouts)
            Slot(secondary)
            // Dialog (index 4) - The dialog (for multi-pane layouts)
            dialog?.invoke()
        }
    )
}


/**
 * A [MeasurePolicy] that places only primary pane; without secondary pane.
 */
private data class OnePaneMeasurePolicy(
    private val fabPosition: FabPosition,
    private val onUpdateIntent: (Padding) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, c: Constraints): MeasureResult {
        val width = c.maxWidth; val height = c.maxHeight
        // measure content with original coordinates.
        // Loose constraints for initial measurements
        val contentPlaceable = measurables[INDEX_CONTENT].measure(c)
        val constraints = c.copy(0, minHeight = 0)
        val topBarPlaceable = measurables[INDEX_TOP_BAR].measure(constraints)
        val fabPlaceable = measurables[INDEX_FAB].measure(constraints)
        // Update content insets to account for Top Bar height
        onUpdateIntent(Padding(top = topBarPlaceable.height.toDp()))
        // measure dialog with original coordinates.
        val dialogPlaceable  = measurables.getOrNull(3)?.measure(c)
        // since details is absent no need to further complicate things.
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
            // place dialog over every-one
            dialogPlaceable?.placeRelative(0, 0)
        }
    }
}

/**
 * A [MeasurePolicy] That places details below content if available
 */
private data class TwoPaneVerticalMeasurePolicy(
    private val strategy: VerticalTwoPaneStrategy,
    private val fabPosition: FabPosition,
    private val spacing: Dp,
    private val onUpdateIntent: (Padding) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, c: Constraints): MeasureResult {
        val width = c.maxWidth; val height = c.maxHeight
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
        // Measure Content, dynamically allocating space based on the size of the details pane.
        // If details are absent, content gets the full available space.
        // If details are present, content height is limited to either the space above the split
        // or the actual height of the details pane, whichever is greater.
        // This ensures content fills the available space efficiently, even if the details pane
        // doesn't fully utilize its allocated area.
        val contentAllocatedWidth = splitAtY - gapWidthPx / 2
        val detailsMeasured = detailsPlaceable.height + gapWidthPx / 2
        val remaining = maxOf(contentAllocatedWidth, height - detailsMeasured)
        constraints = c.copy(minHeight = remaining, maxHeight = remaining)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        // measure dialog
        val dialogPlaceable = measurables.getOrNull(INDEX_DIALOG)?.measure(c)
        // Update content insets to account for Top Bar height
        onUpdateIntent(Padding(top = topBarPlaceable.height.toDp()))
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
            // after the gap place the details at the bottom of the content
            detailsPlaceable.placeRelative(
                width / 2 - detailsPlaceable.width / 2,
                height - detailsPlaceable.height
            )
            // place dialog
            dialogPlaceable?.placeRelative(0, 0)
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
    private val onUpdateIntent: (Padding) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, c: Constraints): MeasureResult {
        val width = c.maxWidth; val height = c.maxHeight
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
        // measure others
        // Others are restricted to the size of the remaining space.
        val contentAllocatedWidth = splitAtX - gapWidthPx / 2
        // if details has shorter width than suggested; then content will take that space.
        val remaining = maxOf(contentAllocatedWidth, width - detailsPlaceable.measuredWidth - gapWidthPx)
        constraints = c.copy(maxWidth = remaining, minWidth = remaining)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        val dialogPlaceable = measurables.getOrNull(INDEX_DIALOG)?.measure(constraints)
        constraints = constraints.copy(minHeight = 0, minWidth = 0)
        val topBarPlaceable = measurables[INDEX_TOP_BAR].measure(constraints)
        val fabPlaceable = measurables[INDEX_FAB].measure(constraints)
        // Update content insets to account for Top Bar height
        onUpdateIntent(Padding(top = topBarPlaceable.height.toDp()))
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
            // after the gap place the details at the bottom of the content
            detailsPlaceable.placeRelative(
                contentPlaceable.width + gapWidthPx / 2,
                0
            )
            // overlay content with scrim
            dialogPlaceable?.placeRelative(0, 0)
        }
    }
}