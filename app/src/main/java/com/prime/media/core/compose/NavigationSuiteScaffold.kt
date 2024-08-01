package com.prime.media.core.compose

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

private const val TAG = "NavigationSuitScaffold"

/**
 * The content padding for the screen under current [NavGraph]
 */
@Deprecated("This is no use from now on.")
val LocalWindowPadding =
    compositionLocalOf { PaddingValues(0.dp) }

private const val INDEX_CONTENT = 0
private const val INDEX_NAV_BAR = 1
private const val INDEX_SNACK_BAR = 2
private const val INDEX_PIXEL = 3
private const val INDEX_PROGRESS_BAR = 4

/**
 * The standard spacing (in dp) applied between two consecutive items,
 * such as the pixel and snack bar, within a vertical layout.
 */
private val STANDARD_SPACING = 8.dp

@Composable
private inline fun Vertical(
    content: @UiComposable @Composable () -> Unit,
    modifier: Modifier = Modifier
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
        val pixelPlaceable = measurables[INDEX_PIXEL].measure(c.copy(0, minHeight = 0))
        // Calculate the height available for the main content, excluding the navigation bar
        val contentHeight = height - navBarPlaceable.height
        constraints = c.copy(minHeight = contentHeight, maxHeight = contentHeight)
        val contentPlaceable = measurables[INDEX_CONTENT].measure(constraints)
        // Place the contents;
        layout(width, height) {
            var x = 0;
            var y = 0
            // Place the main content at the top, filling the space up to the navigation bar
            contentPlaceable.placeRelative(x, y)
            // Place navbar below the content
            x = 0; y = contentHeight
            navBarPlaceable.placeRelative(x, y)
            // Place progress bar at the very bottom of the screen, ignoring system insets
            // (it might overlap system bars if they are not colored)
            x = width / 2 - progressBarPlaceable.width / 2
            y = (height - progressBarPlaceable.height)
            progressBarPlaceable.placeRelative(x, y)
            // Place pixel above the navbar, adjusting its position if the navbar
            // is hidden
            // we only need bottom insets since we are placing above the navBar
            val insetBottom =
                if (navBarPlaceable.height == 0) systemNavBarInsets.getBottom(density = this@Layout) else 0
            x = width / 2 - pixelPlaceable.width / 2;
            y = contentHeight - pixelPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom
            pixelPlaceable.placeRelative(x, y)
            // Place Channel at the centre bottom of the screen
            // remove nav bar offset from it.
            x = width / 2 - snackBarPlaceable.width / 2   // centre
            // full height - toaster height - navbar - 16dp padding + navbar offset.
            y =
                (contentHeight - snackBarPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom)
            snackBarPlaceable.placeRelative(x, y)
        }
    }
}

@Composable
private inline fun Horizontal(
    content: @UiComposable @Composable () -> Unit,
    modifier: Modifier = Modifier
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
        // Measure the size requirements of each child element
        // Allow the elements to have a custom size by not constraining them.
        var constraints = c.copy(minHeight = 0, minWidth = 0)
        val snackBarPlaceable = measurables[INDEX_SNACK_BAR].measure(constraints)
        val progressBarPlaceable = measurables[INDEX_PROGRESS_BAR].measure(constraints)
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
            x = width / 2 - pixelPlaceable.width / 2;
            y = height - pixelPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom
            pixelPlaceable.placeRelative(x, y)
            // Place SnackBar at the centre bottom of the screen
            // remove nav bar offset from it.
            x = width / 2 - snackBarPlaceable.width / 2   // centre
            // full height - toaster height - navbar - 16dp padding + navbar offset.
            y = (height - snackBarPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom)
            snackBarPlaceable.placeRelative(x, y)
        }
    }
}

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
 * - If no content is available, pass a [Spacer] to avoid an empty composable.
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
    background: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor = background),
    channel: Channel = remember(::Channel),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    navBar: @Composable () -> Unit,
) {
    // Compose all the individual elements of the Scaffold into a single composable
    val composed = @Composable {
        // Provide the content color for the main content
        CompositionLocalProvider(
            value = LocalContentColor provides contentColor,
            content = content
        )
        // Conditionally display the navigation bar based on
        // 'hideNavigationBar'
        // Display the navigation bar (either bottom bar or navigation rail)
        when {
            // Don't show anything.
            hideNavigationBar -> Spacer(modifier = Modifier)
            else -> navBar()
        }
        // Display the Snackbar using the provided channel
        SnackbarProvider(channel)
        // Display the pixel element
        pixel()
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
    when(vertical) {
        true -> Vertical(content = composed, modifier = finalModifier)
        else -> Horizontal(content = composed, modifier = finalModifier)
    }
}