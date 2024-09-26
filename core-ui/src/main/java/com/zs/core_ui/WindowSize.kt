package com.zs.core_ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

private const val TAG = "WindowSize"

// TODO: Maybe add the special window sizes to this like Small, Mobile, Large.

/**
 * Represents different reach categories based on screen width or height.
 */
enum class Range {
    /**
     * Indicates a compact screen size, typically for smaller devices like phones.
     *
     * Breakpoint: 320-500 dp
     */
    Compact,

    /**
     * Indicates a medium screen size, typically for tablets or small laptops.
     *
     * Breakpoint: 501-700 dp
     */
    Medium,

    /**
     * Indicates a large screen size, typically for laptops or desktops.
     *
     * Breakpoint: 701-900 dp
     */
    Large,

    /**
     * Indicates a very large screen size, typically for external monitors.
     *
     * Breakpoint: 900 dp and above
     */
    xLarge
}

/**
 * Calculates the reach category for a given width.
 *
 * @param width The width of the window in Dp.
 * @return The corresponding reach category.
 */
private fun fromWidth(width: Dp): Range {
    require(width >= 0.dp) { "Width must not be negative" }
    return when {
        width <= 500.dp -> Range.Compact
        width <= 700.dp -> Range.Medium
        width <= 900.dp -> Range.Large
        else -> Range.xLarge
    }
}

/**
 * Calculates the reach category for a given height.
 *
 * @param height The height of the window in Dp.
 * @return The corresponding reach category.
 */
private fun fromHeight(height: Dp): Range {
    require(height >= 0.dp) { "Height must not be negative" }
    return when {
        height <= 500.dp -> Range.Compact
        height <= 700.dp -> Range.Medium
        height <= 900.dp -> Range.Large
        else -> Range.xLarge
    }
}

/**
 * Represents the size of a window in terms of reach categories (Compact, Medium, Large, xLarge).
 *
 * @param value The size of the window in DpSize.
 */
@Immutable
@JvmInline
value class WindowSize(val value: DpSize) {

    /**
     * @see Range
     */
    val widthRange: Range get() = fromWidth(value.width)
    /**
     * @see Range
     */
    val heightRange: Range get() = fromHeight(value.height)

    operator fun component1() = widthRange
    operator fun component2() = heightRange

    /**
     * Consumes a specified amount of width and/or height from the current [WindowSize] and returns a new [WindowSize] with the adjusted dimensions.
     *
     * @param amount The [DpSize] to consume, representing the amount to subtract from both width and height.
     * @return A new [WindowSize] with the consumed amount subtracted from the original [value].
     */
    fun consume(amount: DpSize): WindowSize = WindowSize(value - amount)

    /**
     * @see consume
     */
    fun consume(width: Dp = 0.dp, height: Dp = 0.dp) = consume(DpSize(width, height))

    override fun toString() = "WindowSize($widthRange, $heightRange)"
}

/**
 * Calculates the window's [WindowSize] for the provided [activity].
 *
 * A new [WindowSize] will be returned whenever a configuration change causes the width or
 * height of the window to cross a breakpoint, such as when the device is rotated or the window
 * is resized.
 *
 * @sample androidx.compose.material3.windowsizeclass.samples.AndroidWindowSizeClassSample
 * @param activity The [Activity] for which the window size is calculated.
 * @return The [WindowSize] corresponding to the current width and height.
 */
@Composable
@ReadOnlyComposable
fun calculateWindowSizeClass(activity: Activity): WindowSize {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
    return WindowSize(size)
}

/**
 * [CompositionLocal] containing the [WindowSize].
 *
 * This [CompositionLocal] is used to access the current [WindowSize] within a composition.
 * If no [WindowSize] is found in the composition hierarchy, an error will be thrown.
 *
 * Usage:
 * ```
 * val windowSize = LocalWindowSize.current
 * // Use the windowSize value within the composition
 * ```
 * @optIn ExperimentalMaterial3WindowSizeClassApi
 */
val LocalWindowSize = compositionLocalOf<WindowSize> {
    error("No Window size defined.")
}