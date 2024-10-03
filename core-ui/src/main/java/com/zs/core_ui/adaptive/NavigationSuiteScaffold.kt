package com.zs.core_ui.adaptive

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
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.zs.core_ui.AppTheme
import com.zs.core_ui.toast.ToastHost
import com.zs.core_ui.toast.ToastHostState

private const val TAG = "NavigationSuitScaffold"

private val EmptyInsets = AppTheme.emptyPadding

/**
 * The content insets for the screen under current [NavigationSuitScaffold]
 */
internal val LocalContentInsets =
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
 * A flexible Scaffold for displaying content with a navigation bar, a dynamic "pixel" element
 * (similar to iPhone's Dynamic Island), a toast, and a progress bar. Supports vertical and
 * horizontal layouts, automatically managing window insets to avoid overlap with system bars.
 *
 * **Vertical Layout:**
 * - Navigation bar at the bottom.
 * - "Widget", Toast, and ProgressBar also at the bottom.
 *
 * **Horizontal Layout:**
 * - Navigation rail (sidebar) instead of a bottom bar.
 * - "Window" and toast at the bottom center.
 *
 * @param vertical `true` for vertical layout, `false` for horizontal.
 * @param content The screen's main content.
 * @param modifier Modifier for the Scaffold.
 * @param widget Composable for the dynamic "pixel" element.
 * @param hideNavigationBar `true` to hide the navigation bar.
 * @param background Background color.
 * @param contentColor Content color.
 * @param toastHostState A [ToastHostState] for handling toast messages.
 * @param progress Progress value (`Float.NaN` to hide, `-1f` for indeterminate, `0f - 1f` for determinate).
 * @param navBar Composable for the navigation bar or rail.
 */
@Composable
fun NavigationSuiteScaffold(
    vertical: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    widget: @Composable () -> Unit = {},
    hideNavigationBar: Boolean = false,
    background: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor = background),
    toastHostState: ToastHostState = remember(::ToastHostState),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    navBar: @Composable () -> Unit,
) {
    val (insets, onNewInsets) =
        remember { mutableStateOf(EmptyInsets) }
    val navBarInsets = WindowInsets.navigationBars
    Layout(
        modifier = modifier
            .background(background)
            .fillMaxSize(),
        measurePolicy = remember(vertical, navBarInsets) {
            when {
                vertical -> VerticalMeasurePolicy(navBarInsets, onNewInsets)
                else -> HorizontalMeasurePolicy(navBarInsets, onNewInsets)
            }
        },
        content = {
            // Provide the content color for the main content
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalContentInsets provides insets,
                content = content
            )
            // Conditionally display the navigation bar based on
            // 'hideNavigationBar'
            // Display the navigation bar (either bottom bar or navigation rail)
            // Don't show anything.
            when (hideNavigationBar) {
                true -> Spacer(modifier = Modifier)
                else -> navBar()
            }
            // Display the SnackBar using the provided channel
            ToastHost(toastHostState)
            // Display the pixel element
            Box { widget() }
            // Conditionally display the progress bar based on the 'progress' value
            // Show an indeterminate progress bar when progress is -1
            // Show a determinate progress bar when progress is between 0 and 1
            when {
                progress == -1f -> LinearProgressIndicator()
                !progress.isNaN() -> LinearProgressIndicator(progress = progress)
                else -> Spacer(modifier = Modifier)
            }
        }
    )
}

/**
 * A [MeasurePolicy] for the [NavigationSuiteScaffold] that arranges the content, navigation bar,
 * floating widget, and toast in a vertical layout.
 *
 * This policy positions the navigation bar at the bottom of the screen. Content is displayed above the
 * navigation bar and consumes the remaining available space. A floating widget is positioned above the
 * center of the navigation bar. Toasts are displayed on top of all other elements, ensuring they are
 * always visible.
 *
 * @param insets The [WindowInsets] to be applied to the toast and floating widget to prevent them
 * from being obscured when the navigation bar is not visible.
 * @param onNewInsets A lambda function that receives the [PaddingValues] to be applied to the content,
 * ensuring it is not hidden behind the navigation bar.
 */
private class VerticalMeasurePolicy(
    private val insets: WindowInsets,
    private val onNewInsets: (PaddingValues) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        c: Constraints
    ): MeasureResult {
        val width = c.maxWidth
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
        return layout(width, height) {
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
                if (isNavBarHidden) insets.getBottom(density = this@measure) else 0
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
                else height - navBarPlaceable.height + 32.dp.roundToPx()
            pixelPlaceable.placeRelative(x, y)
        }
    }
}

/**
 * A [MeasurePolicy] for the [NavigationSuiteScaffold] that arranges the content, navigation bar,
 * floating widget, and toast in a horizontal layout.
 *
 * This policy positions the navigation bar to the side of the screen. The content is displayed next
 * to the navigation bar. The navigation bar can be a navigation rail or a wide navigation bar
 * depending on the user's configuration.
 *
 * In this layout, the [onNewInsets] lambda returns [EmptyInsets] because the width of the navigation
 * bar is already accounted for and deducted from the available space for the content.
 *
 * The floating widget and toast are positioned at the bottom center of the screen.
 *
 * @param insets The [WindowInsets] to be applied to the toast and floating widget to prevent them
 * from being obscured when the navigation bar is not visible.
 * @param onNewInsets A lambda function that provides [PaddingValues] to the content. In this case,
 * it returns [EmptyInsets] as the navigation bar width is already excluded from the content's available space.
 */
private class HorizontalMeasurePolicy(
    private val insets: WindowInsets,
    private val onNewInsets: (PaddingValues) -> Unit
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        c: Constraints
    ): MeasureResult {
        val width = c.maxWidth
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
        // reset new insets
        onNewInsets(EmptyInsets)
        return layout(width, height) {
            var x = 0
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
            val insetBottom = insets.getBottom(density = this@measure)
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
            x =
                if (isExpanded) width / 2 - pixelPlaceable.width / 2 else navBarPlaceable.width / 2 - pixelPlaceable.width / 2
            y = height - pixelPlaceable.height - STANDARD_SPACING.roundToPx() - insetBottom
            pixelPlaceable.placeRelative(x, y)
        }
    }
}