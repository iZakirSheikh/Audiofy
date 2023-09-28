package com.prime.media.core.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val TAG = "Scaffold2"

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 500)

/**
 * The content padding for the screen under current [NavGraph]
 */
@Deprecated("This is no use from now on.")
val LocalWindowPadding = compositionLocalOf {
    PaddingValues(0.dp)
}

private const val LAYOUT_ID_PROGRESS_BAR = "_layout_id_progress_bar"

/**
 * Scaffold implements the top-level visual layout structure.
 *
 * This component provides an API to assemble multiple components into a screen, ensuring proper
 * layout strategy and coordination between the components.
 *
 * @param vertical Determines the layout structure, allowing either vertical or horizontal
 *                 orientation. When set to true, a vertical layout is used, and a navRail is used
 *                 instead of a navbar in the horizontal layout.
 * @param content The main content of the screen to be displayed. The context is automatically
 *                boxed, so manual boxing is not required.
 * @param modifier Optional [Modifier] to be applied to the composable.
 * @param channel Optional [SnackbarHostState] object to handle displaying [Snack] messages.
 * @param progress Optional progress value to show a linear progress bar. Pass [Float.NaN] to hide
 *                 the progress bar, -1 to show an indeterminate progress bar, or a value between 0 and 1 to show a determinate progress bar.
 * @param tabs Optional [Composable] function to display a navigation bar or toolbar.
 * @param hideNavBar Optional value to force hiding the navigation bar.
 */
@Composable
fun Scaffold2(
    vertical: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    hideNavigationBar: Boolean = false,
    channel: Channel = remember(::Channel),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    navBar: @Composable () -> Unit,
) {
    val realContent =
        @Composable {
            // The main content. Autoboxed inside surface.
            Surface(content = content, color = Color.Transparent)
            // The SnackBar
            SnackbarProvider(channel)
            // ProgressBar
            // Don't draw when progress is Float.NaN
            when {
                // special value indicating that the progress is about to start.
                progress == -1f -> LinearProgressIndicator(
                    modifier = Modifier.layoutId(LAYOUT_ID_PROGRESS_BAR)
                )
                // draw the progress bar at the bottom of the screen when is not a NAN.
                !progress.isNaN() -> LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.layoutId(LAYOUT_ID_PROGRESS_BAR)
                )
                // draw nothing.
                else -> Unit
            }
            // Don't show the NavigationBar if hideNavigationBar
            // Show BottomBar when vertical else show NavigationRail.
            when {
                // Don't show anything.
                hideNavigationBar -> Unit
                // Show BottomAppBar
                // Push content to centre of the screen.
                else -> navBar()
            }
        }
    when (vertical) {
        true -> Vertical(content = realContent, modifier = modifier.fillMaxSize())
        else -> Horizontal(content = realContent, modifier = modifier.fillMaxSize())
    }
}

@Composable
private inline fun Vertical(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var orgNavBarHeightPx by remember { mutableFloatStateOf(Float.NaN) }
    val orgAnmNavBarHeight by animateFloatAsState(targetValue = if (orgNavBarHeightPx.isNaN()) 0f else orgNavBarHeightPx, tween(500))
    var navBarHeight by remember { mutableFloatStateOf(0f) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                // if not init return
                if (orgNavBarHeightPx.isNaN()) return Offset.Zero
                val newOffset = navBarHeight - delta.roundToInt()
                // calculate how much height should be reduced or increased.
                navBarHeight = newOffset.coerceIn(0f, orgNavBarHeightPx)
                // return nothing consumed.
                return Offset.Zero
            }
        }
    }
    Layout(
        content = content,
        modifier = modifier
            .nestedScroll(connection)
            .fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        // The content's length should be equal to height - navBar suggested length.
        val navBarOffsetY = if (orgAnmNavBarHeight == 0f) 0f else orgAnmNavBarHeight - navBarHeight
        var h = height - navBarOffsetY.roundToInt()
        val placeableContent = measurables[0].measure(
            constraints.copy(minHeight = h, maxHeight = h)
        )
        val channelPlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )
        val measurable = measurables.getOrNull(2)
        val placeable = measurable?.measure(constraints.copy(minHeight = 0))
        val placeableProgress =
            if (measurable?.layoutId == LAYOUT_ID_PROGRESS_BAR) placeable else null
        val placeableNavBar =
            if (measurable?.layoutId != LAYOUT_ID_PROGRESS_BAR) placeable else null

        // update the height etc.
        orgNavBarHeightPx = placeableNavBar?.height?.toFloat() ?: Float.NaN
        layout(width, height) {
            var x: Int = 0
            var y: Int = 0
            placeableContent.placeRelative(0, 0)
            // Place Channel at the centre bottom of the screen
            // remove nav bar offset from it.
            x = width / 2 - channelPlaceable.width / 2   // centre
            // full height - toaster height - navbar - 16dp padding + navbar offset.
            y = (height - channelPlaceable.height - navBarOffsetY).roundToInt()
            channelPlaceable.placeRelative(x, y)
            // NavBar
            x = width / 2 - (placeableNavBar?.width ?: 0) / 2
            y = (height - navBarOffsetY).roundToInt()
            placeableNavBar?.placeRelative(x, y)
            // the progress bar
            x = width / 2 - (placeableProgress?.width ?: 0) / 2
            y = (height - (placeableProgress?.height ?: 0) - navBarOffsetY).roundToInt()
            placeableProgress?.placeRelative(x, y)
        }
    }
}

@Composable
private inline fun Horizontal(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            // First thing first.
            // try to find the progress measurable.
            // The index of progress measurable is 2
            val measurable = when {
                measurables.size == 4 -> measurables[2]
                measurables.size == 3 && measurables[2].layoutId == LAYOUT_ID_PROGRESS_BAR -> measurables[2]
                else -> null
            }
            // obtain the nav rail placeable
            val placeableNavRail = measurables.getOrNull(if (measurable == null) 2 else 3)
                ?.measure(constraints.copy(minWidth = 0))
            var w = width - (placeableNavRail?.width ?: 0)
            val modified = constraints.copy(minWidth = w, maxWidth = w)
            val placeableProgressBar = measurable?.measure(modified)
            val placeableContent = measurables[0].measure(modified)
            val placeableChannel = measurables[1].measure(modified.copy(minWidth = 0))
            layout(width, height) {
                var x: Int = 0
                var y: Int = 0
                placeableContent.placeRelative(x, y)
                // Place toaster at the centre bottom of the screen
                // remove nav bar offset from it.
                x = (placeableNavRail?.width
                    ?: 0) + (placeableContent.width / 2) - placeableChannel.width / 2   // centre
                // full height - toaster height - navbar - 16dp padding + navbar offset.
                y = (height - placeableChannel.height)
                placeableChannel.placeRelative(x, y)
                // NavBar place at the start of the screen.
                x = width - (placeableNavRail?.width ?: 0)
                y = 0
                placeableNavRail?.placeRelative(x, y)
                // Place ProgressBar at the bottom of the screen.
                x = placeableContent.width / 2 - (placeableProgressBar?.width ?: 0) / 2
                y = height - (placeableProgressBar?.height ?: 0)
                placeableProgressBar?.placeRelative(x, y)
            }
        }
    )
}