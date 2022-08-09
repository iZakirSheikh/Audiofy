package com.prime.player.common.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator


/**
 * Window size classes are a set of opinionated viewport breakpoints for you to design, develop, and
 * test resizable application layouts against. They have been chosen specifically to balance layout
 * simplicity with the flexibility to optimize your app for unique cases.
 *
 * ![Window Sizes: Image](https://developer.android.com/images/guide/topics/large-screens/window_size_classes_width.png)
 *
 * Window size classes partition the raw window size available to your app into more manageable and
 * meaningful buckets. There are three buckets: compact, medium, and expanded. The available width
 * and height are partitioned individually, so at any point in time, your app has two size classes
 * associated with it: a width window size class, and a height window size class.
 *
 * While window size classes are specified for both width and height, the available width is often
 * more important than available height due to the ubiquity of vertical scrolling. Therefore, the
 * width window size class will likely be more relevant to your app’s UI.
 * @sample
 * @see https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#compose
 */
enum class WindowSize {

    // Small phones
    // An window size of 480p width max
    COMPACT,

    // medium phones
    // An Window Size of 600dp width max
    MEDIUM,

    // tablets 7 inch
    // An Window size of 800p width max
    LARGE,

    // large tablets and beyond.
    // Beyond 800p
    X_LARGE
}


/**
 * Remembers the [WindowSize] class for the window corresponding to the current window metrics.
 */
@Composable
fun Activity.rememberWindowSizeClass(): WindowSize {
    val configuration = LocalConfiguration.current
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
    }
    val windowDpSize = with(LocalDensity.current) {
        windowMetrics.bounds.toComposeRect().size.toDpSize()
    }
    return when {
        windowDpSize.width < 480.dp -> WindowSize.COMPACT
        windowDpSize.width < 600.dp -> WindowSize.MEDIUM
        windowDpSize.width < 840.dp -> WindowSize.LARGE
        else -> WindowSize.X_LARGE
    }

    //INFO: This seems not required.

    /* val heightWindowSizeClass = when {
         windowDpSize.height < 480.dp -> WindowSize.COMPACT
         windowDpSize.height < 900.dp -> WindowSize.MEDIUM
         else -> WindowSize.EXPANDED
     }*/

    // Use widthWindowSizeClass and heightWindowSizeClass
}


val LocalWindowSizeClass = compositionLocalOf<WindowSize> {
    error("No Window size available")
}