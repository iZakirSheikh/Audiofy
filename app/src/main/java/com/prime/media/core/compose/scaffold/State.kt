package com.prime.media.core.compose.scaffold

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.rememberSaveable

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 500)
private const val TAG = "Player"

/**
 * Possible values of [ScaffoldState2].
 */
enum class SheetState {

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
class ScaffoldState2(
    initial: SheetState
) {
    /**
     * Maps between [SheetState] and screen.
     */
    private fun map(value: SheetState): Float = when (value) {
        SheetState.COLLAPSED -> 0f
        SheetState.EXPANDED -> 1f
    }

    /**
     * Maps between [SheetState] and screen.
     */
    private fun map(progress: Float): SheetState = when (progress) {
        0f -> SheetState.COLLAPSED
        else -> SheetState.EXPANDED
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
     * The current state of the [SheetState]
     */
    val current
        get() = map(progress.value)

    /**
     * Whether the drawer is closed.
     */
    inline val isCollapsed: Boolean
        get() = current == SheetState.COLLAPSED

    /**
     * Whether the drawer is expanded.
     */
    inline val isExpanded: Boolean
        get() = current == SheetState.EXPANDED


    /**
     * Set the state to the target value by starting an animation.
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterialApi
    private suspend fun animateTo(
        targetValue: SheetState, anim: AnimationSpec<Float> = AnimationSpec
    ) {
        animatable.animateTo(map(targetValue), animationSpec = anim)
    }

    /**
     * @see [Animatable.snapTo]
     */
    suspend fun snapTo(@FloatRange(0.0, 1.0) targetValue: Float) {
        animatable.snapTo(targetValue)
    }

    suspend fun snapTo(targetValue: SheetState) {
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
        animateTo(SheetState.COLLAPSED)
    }

    /**
     * Expand the drawer with animation and suspend until it if fully expanded or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     *
     */
    suspend fun expand() = animateTo(SheetState.EXPANDED)

    companion object {
        /**
         * The default [Saver] implementation for [ScaffoldState2].
         */
        fun Saver() =
            androidx.compose.runtime.saveable.Saver<ScaffoldState2, SheetState>(
                save = { it.current },
                restore = { ScaffoldState2(it) })
    }
}

/**
 * Create and [remember] a [ScaffoldState2].
 *
 * @param initial The initial value of the state.
 * @param open: How much percent is considered open
 */
@Composable
fun rememberScaffoldState2(
    initial: SheetState
): ScaffoldState2 {
    return rememberSaveable(saver = ScaffoldState2.Saver()) {
        ScaffoldState2(initial)
    }
}