package com.prime.player.core

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

/**
 * Height/Width-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each
 * window size class breakpoint represents a majority case for typical device scenarios so your
 * layouts will work well on most devices and configurations.
 *
 * For more details see <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes" class="external" target="_blank">Window size classes documentation</a>.
 */
enum class WindowSize {
    /**
     * **Width** - Represents the majority of phones in portrait.
     *
     * **Height** - Represents the majority of phones in landscape
     */
    COMPACT,

    /**
     * **Width** - Represents the majority of tablets in portrait and large unfolded inner displays in portrait.
     * **Height** - Represents the majority of tablets in landscape and majority of phones in portrait
     */
    MEDIUM,

    /**
     * **Width** - Represents the majority of tablets in landscape and large unfolded inner displays in landscape.
     * **Height** - Represents the majority of tablets in portrait
     */
    EXPANDED;
}



/** Calculates the [WindowWidthSizeClass] for a given [width] */
private fun fromWidth(width: Dp): WindowSize {
    require(width >= 0.dp) { "Width must not be negative" }
    return when {
        width < 600.dp -> WindowSize.COMPACT
        width < 840.dp -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

/** Calculates the [WindowHeightSizeClass] for a given [height] */
private fun fromHeight(height: Dp): WindowSize {
    require(height >= 0.dp) { "Height must not be negative" }
    return when {
        height < 480.dp -> WindowSize.COMPACT
        height < 900.dp -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

/**
 * Window size classes are a set of opinionated viewport breakpoints to design, develop, and test
 * responsive application layouts against.
 * For more details check <a href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes" class="external" target="_blank">Support different screen sizes</a> documentation.
 *
 * WindowSizeClass contains a [WindowWidthSizeClass] and [WindowHeightSizeClass], representing the
 * window size classes for this window's width and height respectively.
 *
 * See [calculateWindowSizeClass] to calculate the WindowSizeClass for an Activity's current window
 *
 * @property widthSizeClass width-based window size class ([WindowWidthSizeClass])
 * @property heightSizeClass height-based window size class ([WindowHeightSizeClass])
 */
@Immutable
class WindowSizeClass private constructor(
    val widthSizeClass: WindowSize,
    val heightSizeClass: WindowSize
) {

    companion object {
        /**
         * Calculates [WindowSizeClass] for a given [size]. Should be used for testing purposes only
         * - to calculate a [WindowSizeClass] for the Activity's current window see
         * [calculateWindowSizeClass].
         *
         * @param size of the window
         * @return [WindowSizeClass] corresponding to the given width and height
         */
        fun calculateFromSize(size: DpSize): WindowSizeClass {
            val windowWidthSizeClass = fromWidth(size.width)
            val windowHeightSizeClass = fromHeight(size.height)
            return WindowSizeClass(windowWidthSizeClass, windowHeightSizeClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WindowSizeClass

        if (widthSizeClass != other.widthSizeClass) return false
        if (heightSizeClass != other.heightSizeClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = widthSizeClass.hashCode()
        result = 31 * result + heightSizeClass.hashCode()
        return result
    }

    operator fun component1(): WindowSize = widthSizeClass

    operator fun component2(): WindowSize = heightSizeClass

    override fun toString() = "WindowSizeClass($widthSizeClass, $heightSizeClass)"
}

/**
 * Calculates the window's [WindowSizeClass] for the provided [activity].
 *
 * A new [WindowSizeClass] will be returned whenever a configuration change causes the width or
 * height of the window to cross a breakpoint, such as when the device is rotated or the window
 * is resized.
 *
 * @sample androidx.compose.material3.windowsizeclass.samples.AndroidWindowSizeClassSample
 */
@Composable
fun calculateWindowSizeClass(activity: Activity): WindowSizeClass {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val density = LocalDensity.current
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val size = with(density) { metrics.bounds.toComposeRect().size.toDpSize() }
    return WindowSizeClass.calculateFromSize(size)
}


val LocalWindowSizeClass =
    staticCompositionLocalOf<WindowSizeClass> {
        error("No Window size available")
    }