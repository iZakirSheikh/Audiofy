@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media.core.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize
import androidx.compose.animation.SharedTransitionScope.PlaceHolderSize.Companion.contentSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val TAG = "Shared Transition"

@OptIn(ExperimentalSharedTransitionApi::class)
val MaterialTheme.sharedTransitionScope
    @Composable
    @ReadOnlyComposable
    get() = LocalSharedTransitionScope.current

/**
 * Provides a [CompositionLocal] to access the current [SharedTransitionScope].
 *
 * This CompositionLocal should be provided bya parent composable that manages shared transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> {
        error("CompositionLocal LocalSharedTransition not present")
    }

/**
 * Provides a[CompositionLocal] to access the current [AnimatedVisibilityScope].
 *
 * This CompositionLocal should be provided by a parent composable that manages animated visibility.
 */
val LocalAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope> { error("CompositionLocal LocalSharedTransition not present") }

private val DefaultSpring = spring(
    stiffness = StiffnessMediumLow,
    visibilityThreshold = Rect.VisibilityThreshold
)

@ExperimentalSharedTransitionApi
private val DefaultBoundsTransform = BoundsTransform { _, _ -> DefaultSpring }

@ExperimentalSharedTransitionApi
private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            state: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return state.parentSharedContentState?.clipPathInOverlay
        }
    }

/**
 * @see androidx.compose.animation.SharedTransitionScope.sharedElement
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElement(
    state: SharedContentState,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
) = composed {
    val navAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    with(sharedTransitionScope) {
        Modifier.sharedElement(
            state = state,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            animatedVisibilityScope = navAnimatedVisibilityScope,
            boundsTransform = boundsTransform,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}


/**
 * A shared bounds modifier that uses scope from [AppTheme]'s [AppTheme.sharedTransitionScope] and [AnimatedVisibilityScope] from [LocalAnimatedVisibilityScope]
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedBounds(
    sharedContentState: SharedContentState,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    resizeMode: ResizeMode = ScaleToBounds(ContentScale.FillWidth, Center),
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
) = composed {
    val navAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = navAnimatedVisibilityScope,
            enter = enter,
            exit = exit,
            boundsTransform = boundsTransform,
            resizeMode = resizeMode,
            placeHolderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}

/**
 * @return the state of shared contnet corresponding to [key].
 * @see androidx.compose.animation.SharedTransitionScope.rememberSharedContentState
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
inline fun rememberSharedContentState(key: Any): SharedContentState =
    with(MaterialTheme.sharedTransitionScope) {
        rememberSharedContentState(key = key)
    }


/**
 * @see androidx.compose.animation.SharedTransitionScope.sharedElement
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElement(
    key: Any,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
) = composed {
    sharedElement(
        state = rememberSharedContentState(key = key),
        boundsTransform,
        placeHolderSize,
        renderInOverlayDuringTransition,
        zIndexInOverlay,
        clipInOverlayDuringTransition
    )
}

/**
 * @see androidx.compose.animation.SharedTransitionScope.sharedBounds
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    resizeMode: ResizeMode = ScaleToBounds(ContentScale.FillWidth, Center),
    placeHolderSize: PlaceHolderSize = contentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip
) = composed {
    sharedBounds(
        sharedContentState = rememberSharedContentState(key = key),
        enter = enter,
        exit = exit,
        boundsTransform = boundsTransform,
        resizeMode = resizeMode,
        placeHolderSize = placeHolderSize,
        renderInOverlayDuringTransition = renderInOverlayDuringTransition,
        zIndexInOverlay = zIndexInOverlay,
        clipInOverlayDuringTransition = clipInOverlayDuringTransition
    )
}

private val DefaultClipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
    { _, _ -> null }

/**
 * @see androidx.compose.animation.SharedTransitionScope.renderInSharedTransitionScopeOverlay
 */
fun Modifier.renderInSharedTransitionScopeOverlay(
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? =
        DefaultClipInOverlayDuringTransition
) = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    with(sharedTransitionScope) {
        Modifier.renderInSharedTransitionScopeOverlay(
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
            )
    }
}
