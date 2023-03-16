package com.prime.media.core.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.*
import com.airbnb.lottie.utils.MiscUtils.lerp
import kotlin.math.roundToInt

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 500)

private const val TAG = "Player"


/**
 * Possible values of [PlayerState].
 */
enum class PlayerValue {

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
 * State of the [Player] composable.
 * @param initial The initial value of the state.
 * @param open: The percentage of screen that is considered open.
 */
@OptIn(ExperimentalMaterialApi::class)
@Stable
class PlayerState(
    initial: PlayerValue
) {
    /**
     * Maps between [PlayerValue] and screen.
     */
    private fun map(value: PlayerValue): Float = when (value) {
        PlayerValue.COLLAPSED -> 0f
        PlayerValue.EXPANDED -> 1f
    }

    /**
     * Maps between [PlayerValue] and screen.
     */
    private fun map(progress: Float): PlayerValue = when (progress) {
        0f -> PlayerValue.COLLAPSED
        else -> PlayerValue.EXPANDED
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
     * The current state of the [PlayerValue]
     */
    val current
        get() = map(progress.value)

    /**
     * Whether the drawer is closed.
     */
    inline val isCollapsed: Boolean
        get() = current == PlayerValue.COLLAPSED

    /**
     * Whether the drawer is expanded.
     */
    inline val isExpanded: Boolean
        get() = current == PlayerValue.EXPANDED


    /**
     * Set the state to the target value by starting an animation.
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterialApi
    private suspend fun animateTo(
        targetValue: PlayerValue, anim: AnimationSpec<Float> = AnimationSpec
    ) {
        animatable.animateTo(map(targetValue), animationSpec = anim)
    }

    /**
     * @see [Animatable.snapTo]
     */
    suspend fun snapTo(@FloatRange(0.0, 1.0) targetValue: Float) {
        animatable.snapTo(targetValue)
    }

    suspend fun snapTo(targetValue: PlayerValue) {
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
        animateTo(PlayerValue.COLLAPSED)
    }

    /**
     * Expand the drawer with animation and suspend until it if fully expanded or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun expand() = animateTo(PlayerValue.EXPANDED)

    companion object {
        /**
         * The default [Saver] implementation for [PlayerState].
         */
        fun Saver() =
            Saver<PlayerState, PlayerValue>(save = { it.current }, restore = { PlayerState(it) })
    }
}

/**
 * Create and [remember] a [PlayerState].
 *
 * @param initial The initial value of the state.
 * @param open: How much percent is considered open
 */
@Composable
fun rememberPlayerState(
    initial: PlayerValue
): PlayerState {
    return rememberSaveable(saver = PlayerState.Saver()) {
        PlayerState(initial)
    }
}

/**
 * This houses the logic to show [Toast]s, animates [sheet] and displays update progress.
 * @param progress progress for the linear progress bar. pass [Float.NaN] to hide and -1 to show
 * indeterminate and value between 0 and 1 to show progress
 */
@Composable
fun Player(
    sheet: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetPeekHeight: Dp = 56.dp,
    state: PlayerState = rememberPlayerState(initial = PlayerValue.COLLAPSED),
    toast: ToastHostState = remember(::ToastHostState),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    content: @Composable () -> Unit
) {
    // How am I going to build it.
    // * Firstly the content occupies the whole of the screen.
    // * The toast shows below sheet if not expanded other wise over it. Keep in mind the animation.
    // * Third The progress bar can be null shows at the extreme bottom of the screen.
    // * Lastly if sheet is closed don't measure it.
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // stack each part over the player.
            content()
            ToastHost(state = toast)
            // don't draw sheet when closed.
            sheet()
            // don't draw progressBar.
            when {
                progress == -1f -> LinearProgressIndicator()
                !progress.isNaN() -> LinearProgressIndicator(progress = progress)
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        // create duplicate constants to measure the contents as per their wishes.
        val duplicate = constraints.copy(minWidth = 0, minHeight = 0)

        // measure original content with original constrains
        val contentPlaceable = measurables[0].measure(constraints)
        val toastPlaceable = measurables[1].measure(duplicate)
        val progressPlaceable = measurables.getOrNull(3)?.measure(duplicate)

        val progress by state.progress
        val sheetPeekHeightPx = sheetPeekHeight.toPx().roundToInt()
        // animate sheet with only upto open.
        val sheetH = lerp(sheetPeekHeightPx, height, progress)
        val sheetW = lerp(0, width, progress)
        val sheetPlaceable = measurables[2].measure(
            constraints.copy(0, width, 0, sheetH)
        )

        layout(width, height) {
            contentPlaceable.placeRelative(0, 0)
            // place at the bottom centre
            val sheetY = height - sheetH
            if (sheetY != height)  // draw only if visible
                sheetPlaceable.placeRelative(0, sheetY)
            //Log.d(TAG, "Player: ${height}")
            val adjusted = if (state.current == PlayerValue.COLLAPSED) sheetPeekHeightPx else 0
            // draw a bottom centre.
            toastPlaceable.placeRelative(
                width / 2 - toastPlaceable.width / 2, height - toastPlaceable.height - adjusted
            )

            progressPlaceable?.placeRelative(
                width / 2 - progressPlaceable.width / 2, height - progressPlaceable.height
            )
        }
    }
}
