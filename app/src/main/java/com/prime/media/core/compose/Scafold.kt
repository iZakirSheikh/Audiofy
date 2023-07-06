package com.prime.media.core.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.airbnb.lottie.utils.MiscUtils.lerp
import kotlin.math.roundToInt

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 500)

private const val TAG = "Player"


/**
 * Possible values of [ScaffoldState].
 */
enum class SheetValue {

    /**
     * The state of the bottom drawer is collapsed.
     */
    COLLAPSED,

    /**
     * The state of the bottom drawer when it is expanded (i.e. at 100% height).
     */
    EXPANDED
}

/**
 * State of the [Scaffold2] composable.
 * @param initial The initial value of the state.
 * @param open: The percentage of screen that is considered open.
 */
@OptIn(ExperimentalMaterialApi::class)
@Stable
class ScaffoldState(
    initial: SheetValue
) {
    /**
     * Maps between [SheetValue] and screen.
     */
    private fun map(value: SheetValue): Float = when (value) {
        SheetValue.COLLAPSED -> 0f
        SheetValue.EXPANDED -> 1f
    }

    /**
     * Maps between [SheetValue] and screen.
     */
    private fun map(progress: Float): SheetValue = when (progress) {
        0f -> SheetValue.COLLAPSED
        else -> SheetValue.EXPANDED
    }

    private val animatable = Animatable(
        map(initial),
        Float.VectorConverter,
        visibilityThreshold = 0.0001f,
    )

    /**
     * Represents a value between 0 and 1.
     * O implies [CurtainValue.CLOSED].
     * 1 implies [CurtainValue.OPEN]
     */
    val progress = animatable.asState()

    /**
     * The current state of the [SheetValue]
     */
    val current
        get() = map(progress.value)

    /**
     * Whether the drawer is closed.
     */
    inline val isCollapsed: Boolean
        get() = current == SheetValue.COLLAPSED

    /**
     * Whether the drawer is expanded.
     */
    inline val isExpanded: Boolean
        get() = current == SheetValue.EXPANDED


    /**
     * Set the state to the target value by starting an animation.
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterialApi
    private suspend fun animateTo(
        targetValue: SheetValue, anim: AnimationSpec<Float> = AnimationSpec
    ) {
        animatable.animateTo(map(targetValue), animationSpec = anim)
    }

    /**
     * @see [Animatable.snapTo]
     */
    suspend fun snapTo(@FloatRange(0.0, 1.0) targetValue: Float) {
        animatable.snapTo(targetValue)
    }

    suspend fun snapTo(targetValue: SheetValue) {
        animatable.snapTo(map(targetValue))
    }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. If the content height is less than [BottomDrawerOpenFraction], the drawer state
     * will move to [BottomDrawerValue.Expanded] instead.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun collapse() {
        animateTo(SheetValue.COLLAPSED)
    }

    /**
     * @see collapse
     * @see expand
     */
    suspend fun toggle() =
        if (current == SheetValue.EXPANDED) collapse() else expand()

    /**
     * Expand the drawer with animation and suspend until it if fully expanded or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun expand() = animateTo(SheetValue.EXPANDED)

    companion object {
        /**
         * The default [Saver] implementation for [ScaffoldState].
         */
        fun Saver() =
            Saver<ScaffoldState, SheetValue>(save = { it.current }, restore = { ScaffoldState(it) })
    }
}

/**
 * Create and [remember] a [ScaffoldState].
 *
 * @param initial The initial value of the state.
 * @param open: How much percent is considered open
 */
@Composable
fun rememberScaffoldState2(
    initial: SheetValue
): ScaffoldState {
    return rememberSaveable(saver = ScaffoldState.Saver()) {
        ScaffoldState(initial)
    }
}
/**
 * This houses the logic to show [Toast]s, animates [sheet] and displays update progress.
 * @param progress progress for the linear progress bar. pass [Float.NaN] to hide and -1 to show
 * indeterminate and value between 0 and 1 to show progress
 */
@Composable
fun Scaffold2(
    sheet: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetPeekHeight: Dp = 56.dp,
    color: Color = MaterialTheme.colors.background,
    state: ScaffoldState = rememberScaffoldState2(initial = SheetValue.COLLAPSED),
    channel: Channel = remember(::Channel),
    vertical: Boolean = true,
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    content: @Composable () -> Unit
) {
    val realContent =
        @Composable {
            // stack each part over the player.
            CompositionLocalProvider(LocalWindowPadding provides PaddingValues(bottom = sheetPeekHeight)) {
                Surface(content = content)
            }
            Channel(state = channel)
            // don't draw sheet when closed.
            sheet()
            // don't draw progressBar.
            when {
                progress == -1f -> LinearProgressIndicator()
                !progress.isNaN() -> LinearProgressIndicator(progress = progress)
            }
        }
    // The scaffold will fill whole screen.
    // Set the background to color.
    val modifier = modifier
        .background(color = color)
        .fillMaxSize()
    val progress by state.progress
    val expanded = state.isExpanded
    when (vertical) {
        true -> Vertical(sheetPeekHeight, expanded, progress, modifier, realContent)
        false -> TODO("Not Implemented yet.")
    }
}

@Composable
private inline fun Vertical(
    sheetPeekHeight: Dp,
    expanded: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val imePaddingPx = WindowInsets.ime.getBottom(density)
    val navBarPaddingPx = WindowInsets.navigationBars.getBottom(density)
    // TODO: Maybe hide on scroll.
    Layout(
        content,
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        // The height of the layout.
        val lHeight = constraints.maxHeight
        val lWidth = constraints.maxWidth
        // Measure content
        val unrestrected = constraints.copy(minWidth = 0, minHeight = 0)
        val placeableContent = measurables[0].measure(unrestrected)
        val placeableChannel = measurables[1].measure(unrestrected)
        val placeableProgressBar = measurables.getOrNull(3)?.measure(unrestrected)
        // measure sheet
        // measure min height agains orgSheetPeekHeightPx
        val sheetPeekHeightPx = sheetPeekHeight.toPx()
        var width = lWidth
        var height = lerp(sheetPeekHeightPx, lHeight.toFloat(), progress).roundToInt()
        val placeableSheet = measurables[2].measure(
            constraints.copy(0, width, 0, height)
        )
        layout(lWidth, lHeight) {
            var x = 0
            var y = 0
            placeableContent.placeRelative(x, y)
            // Place sheet at top if sheetHeight == height
            // else place at the bottom.
            y = lHeight - placeableSheet.height - lerp(
                navBarPaddingPx.toFloat(),
                0f,
                progress
            ).roundToInt() //
            placeableSheet.placeRelative(x, y)
            // the diff to accomondate
            val diff = lerp(sheetPeekHeightPx, 0f, progress)
            x = lWidth / 2 - placeableChannel.width / 2
            y = lHeight - (placeableChannel.height + diff.roundToInt() + navBarPaddingPx + imePaddingPx)
            placeableChannel.placeRelative(x, y)
            if (placeableProgressBar == null)
                return@layout
            x = lWidth / 2 - placeableProgressBar.width / 2
            y = lHeight - placeableProgressBar.height - navBarPaddingPx
            placeableProgressBar.placeRelative(x, y)
        }
    }
}

/**
 * The content padding for the screen under current [NavGraph]
 */
val LocalWindowPadding =
    compositionLocalOf {
        PaddingValues(0.dp)
    }