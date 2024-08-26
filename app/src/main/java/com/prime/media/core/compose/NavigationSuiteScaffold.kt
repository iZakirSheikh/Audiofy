package com.prime.media.core.compose

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.dp

private const val TAG = "NavigationSuitScaffold"

private val EmptyInsets = PaddingValues(0.dp)

/**
 * The content insets for the screen under current [NavigationSuitScaffold]
 */
private val LocalContentInsets =
    compositionLocalOf { EmptyInsets }

/**
 * Provides the insets for the current content within the [Scaffold].
 */
val WindowInsets.Companion.contentInsets
    @ReadOnlyComposable @Composable get() = LocalContentInsets.current

/**
 * The standard spacing (in dp) applied between two consecutive items,
 * such as the pixel and snack bar, within a vertical layout.
 */
private val STANDARD_SPACING = 8.dp

/**
 * Checks if this [Placeable] has zero width or zero height.
 */
private val Placeable.isZeroSized get() = width == 0 || height == 0

private const val INDEX_CONTENT = 0
private const val INDEX_NAV_BAR = 1
private const val INDEX_SNACK_BAR = 2
private const val INDEX_PIXEL = 3
private const val INDEX_PROGRESS_BAR = 4

/**
 * The size of the pixel when in collapsed mode.
 */
private val MINI_PIXEL_SIZE = 60.dp

/**
 * A flexible Scaffold that provides a structured layout for displaying content along with a navigation bar,
 * pixel element, snack bar, and progress bar. It supports both vertical and horizontal layouts.
 *
 * **Vertical Layout:**
 * - Navigation bar is displayed at the bottom of the screen.
 * - Pixel element, snack bar, and progress bar are also positioned at the bottom.
 *
 * **Horizontal Layout:**
 * - A navigation rail (or sidebar) is used instead of a bottom navigation bar.
 * - Pixel element and snack bar are positioned at the end of the screen.
 *
 * **Key Features:**
 * - Automatic window padding management for the pixel element in both layouts.
 * - Customizable background and content colors.
 * - Support for displaying snack bar messages using a [Channel].
 * - Ability to show a determinate or indeterminate progress bar.
 * - Option to force hide the navigation bar.
 *
 * **Usage Recommendations:**
 * - For snack bars, a maximum length of 360 dp is suggested.
 *
 * @param vertical Determines the layout orientation (true for vertical, false for horizontal).
 * @param content The main content to be displayed within the Scaffold.
 * @param modifier Optional [Modifier] to be applied to the Scaffold.
 * @param pixel A composable representing the dynamic "pixel" element, similar to iPhone's Dynamic Island.
 * @param hideNavigationBar Set to true to force hide the navigation bar.
 * @param background The background color of the Scaffold.
 * @param contentColor The color of the content within the Scaffold.
 * @param channel A [Channel] for handling snackbar messages.
 * @param progress The progress value for the linear progress bar (Float.NaN to hide, -1 for indeterminate, 0-1 for determinate).
 * @param navBar A composable function that provides the navigation bar (for vertical layout) or navigation rail (for horizontal layout).
 */
@Composable
fun NavigationSuiteScaffold(
    vertical: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pixel: @Composable () -> Unit = {},
    hideNavigationBar: Boolean = false,
    shape: Shape = RectangleShape,
    background: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor = background),
    channel: Channel = remember(::Channel),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    navBar: @Composable () -> Unit,
) {
    val (insets, onNewInsets) = remember { mutableStateOf(EmptyInsets) }
    // Compose all the individual elements of the Scaffold into a single composable
    val composed = @Composable {
        // Provide the content color for the main content
        Box(modifier = Modifier.clip(shape)) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalContentInsets provides insets,
                content = content
            )
        }

        // Conditionally display the navigation bar based on
        // 'hideNavigationBar'
        // Display the navigation bar (either bottom bar or navigation rail)
        // Don't show anything.
        when (hideNavigationBar) {
            true -> Spacer(modifier = Modifier)
            else -> navBar()
        }
        // Display the SnackBar using the provided channel
        SnackbarProvider(channel)
        // Display the pixel element
        Box { pixel() }
        // Conditionally display the progress bar based on the 'progress' value
        // Show an indeterminate progress bar when progress is -1
        // Show a determinate progress bar when progress is between 0 and 1
        when {
            progress == -1f -> LinearProgressIndicator()
            !progress.isNaN() -> LinearProgressIndicator(progress = progress)
            else -> Spacer(modifier = Modifier)
        }
    }
    // Apply background color and fill the available space with the Scaffold
    val finalModifier = modifier
        .background(background)
        .fillMaxSize()
    // Choose the layout based on 'vertical' flag
    when (vertical) {
        true -> Vertical(content = composed, onNewInsets, modifier = finalModifier)
        else -> Horizontal(content = composed, onNewInsets, modifier = finalModifier)
    }
}

@Composable
private inline fun Vertical(
    content: @UiComposable @Composable () -> Unit,
    crossinline onNewInsets: (PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Insets for the system navigation bar. This will be used to adjust
    // the position of elements when the navigation bar is hidden.
    val systemNavBarInsets = WindowInsets.navigationBars
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, c ->
        val width = c.maxWidth;
        val height = c.maxHeight
        // Measure the size requirements of each child element, allowing
        // them to use the full width
        var constraints = c.copy(minHeight = 0)
        val snackBarPlaceable = measurables[INDEX_SNACK_BAR].measure(constraints)
        val progressBarPlaceable = measurables[INDEX_PROGRESS_BAR].measure(constraints)
        val navBarPlaceable = measurables[INDEX_NAV_BAR].measure(constraints)
        // Allow the pixel element to have a custom size by not constraining its minimum height
        constraints = c.copy(0, minHeight = 0)
        val pixelPlaceable = measurables[INDEX_PIXEL].measure(constraints)
        constraints = c
        // Measure content against original constraints.
        // the content must fill the entire screen.
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        // Calculate the insets for the content.
        // and report through onNewIntent
        onNewInsets(PaddingValues(bottom = navBarPlaceable.height.toDp()))
        layout(width, height) {
            // Place the main content at the top, filling the space up to the navigation bar
            contentPlaceable.placeRelative(0, 0)
            // Place navbar at the bottom of the screen.
            navBarPlaceable.placeRelative(0, height - navBarPlaceable.height)
            // Place progress bar at the very bottom of the screen, ignoring system insets
            // (it might overlap system bars if they are not colored)
            var x = width / 2 - progressBarPlaceable.width / 2
            var y = (height - progressBarPlaceable.height)
            progressBarPlaceable.placeRelative(x, y)
            // Add insets to pixel only if nav bar is hidden.
            val isNavBarHidden = navBarPlaceable.isZeroSized
            val insets =
                if (isNavBarHidden) systemNavBarInsets.getBottom(density = this@Layout) else 0
            // Place Toast at the centre bottom of the screen
            // remove nav bar offset from it.
            x = width / 2 - snackBarPlaceable.width / 2   // centre
            y =
                (height - navBarPlaceable.height - snackBarPlaceable.height - STANDARD_SPACING.roundToPx() - insets)
            // the snack-bar must be top of every composable.
            snackBarPlaceable.placeRelative(x, y, 1f)
            // Don't draw from here if the pixel anchor is missing.
            if (isNavBarHidden) return@layout
            // Determine the positioning of the pixel element based on its expanded state and the
            // presence of the navigation bar.
            // Calculate a threshold size to determine the pixel is considered "expanded".
            val against = MINI_PIXEL_SIZE.roundToPx()
            val isExpanded = pixelPlaceable.height > against && pixelPlaceable.width > against
            // Calculate the x-coordinate for the pixel:
            // - If expanded, center it horizontally on the screen.
            // - Otherwise, position it at the start (left edge) of the navigation bar.
            x = if (isExpanded) width / 2 - pixelPlaceable.width / 2 else 0
            // Calculate the y-coordinate for the pixel:
            // - If expanded, place it above the navigation bar with standard spacing and insets considered.
            // - Otherwise, position it at the start of the navigation bar with a slight offset (16.dp).
            // - If the navigation bar is not present (navBarPlaceable is null), the pixel will not be drawn.
            y =
                if (isExpanded) height - navBarPlaceable.height - pixelPlaceable.height - STANDARD_SPACING.roundToPx() - insets
                // start centre of nav_bar
                else height - navBarPlaceable.height + 16.dp.roundToPx()
            pixelPlaceable.placeRelative(x, y)
        }
    }
}

@Composable
private inline fun Horizontal(
    content: @UiComposable @Composable () -> Unit,
    crossinline onNewInsets: (PaddingValues) -> Unit,
    modifier: Modifier = Modifier
) {
    // Insets for the system navigation bar. This will be used to adjust
    // the position of elements when the navigation bar is hidden.
    val systemNavBarInsets = WindowInsets.navigationBars
    // TODO - Maybe use this to update the insets?
    onNewInsets(EmptyInsets)
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, c ->
        val width = c.maxWidth;
        val height = c.maxHeight
        // Measure the size requirements of each child element
        // Allow the elements to have a custom size by not constraining them.
        var constraints = c.copy(minHeight = 0)
        val progressBarPlaceable = measurables[INDEX_PROGRESS_BAR].measure(constraints)
        constraints = c.copy(minHeight = 0, minWidth = 0)
        val snackBarPlaceable = measurables[INDEX_SNACK_BAR].measure(constraints)
        val navBarPlaceable = measurables[INDEX_NAV_BAR].measure(constraints)
        val pixelPlaceable = measurables[INDEX_PIXEL].measure(constraints)
        // Calculate the width available for the main content, excluding the navigation bar
        val contentWidth = width - navBarPlaceable.width
        constraints = c.copy(minWidth = contentWidth, maxWidth = contentWidth)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        layout(width, height) {
            var x = 0;
            var y = 0
            // place nav_bar from top at the start of the screen
            navBarPlaceable.placeRelative(x, y)
            x = navBarPlaceable.width
            // Place the main content at the top, after nav_bar width
            contentPlaceable.placeRelative(x, y)
            // Place progress bar at the very bottom of the screen, ignoring system insets
            // (it might overlap system bars if they are not colored)
            x = width / 2 - progressBarPlaceable.width / 2
            y = (height - progressBarPlaceable.height)
            progressBarPlaceable.placeRelative(x, y)
            // Place pixel above the system navigationBar at the centre of the screen.
            val insetBottom = systemNavBarInsets.getBottom(density = this@Layout)
            // Place SnackBar at the centre bottom of the screen
            // remove nav bar offset from it.
            x = width / 2 - snackBarPlaceable.width / 2   // centre
            // full height - toaster height - navbar - 16dp padding + navbar offset.
            y = (height - snackBarPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom)
            snackBarPlaceable.placeRelative(x, y)
            // Don't draw from here if the pixel anchor is missing.
            if (navBarPlaceable.isZeroSized) return@layout
            val against = MINI_PIXEL_SIZE.roundToPx()
            val isExpanded = pixelPlaceable.height > against && pixelPlaceable.width > against
            // Position the element strategically based on the expanded state:
            // - Expanded: Anchor it to the bottom center of the entire screen.
            //- Collapsed: Align it to the bottom center of the navigation rail.
            // Additionally, suppress rendering if the navigation rail is not present.
            x = if (isExpanded) width / 2 - pixelPlaceable.width / 2 else navBarPlaceable.width / 2 - pixelPlaceable.width / 2
            y = height - pixelPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom
            pixelPlaceable.placeRelative(x, y)
        }
    }
}

