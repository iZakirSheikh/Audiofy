@file:Suppress("NOTHING_TO_INLINE")

package com.prime.player.common

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.player.R
import com.primex.core.Result
import com.primex.ui.Label
import com.primex.ui.Placeholder


//This file holds the simple extension, utility methods of compose.
/**
 * Composes placeholder with lottie icon.
 */
@Composable
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: String? = null,
    noinline action: @Composable (() -> Unit)? = null
) {
    Placeholder(
        modifier = modifier,
        vertical = vertical,
        message = { if (message != null) Text(text = message) },
        title = { Label(text = title.ifEmpty { " " }, maxLines = 2) },

        icon = {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(
                    iconResId
                )
            )
            LottieAnimation(
                composition = composition, iterations = Int.MAX_VALUE
            )
        },
        action = action,
    )
}


@ExperimentalAnimationApi
@Composable
@Deprecated("Doesn't required.")
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    initiallyVisible: Boolean,
    content: @Composable () -> Unit
) = AnimatedVisibility(visibleState = remember { MutableTransitionState(initiallyVisible) }.apply {
    targetState = visible
}, modifier = modifier, enter = enter, exit = exit
) {
    content()
}

@ExperimentalAnimationGraphicsApi
@Composable
inline fun rememberAnimatedVectorResource(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id), atEnd = atEnd
    )


@Composable
inline fun <T> Placeholder(
    value: Result<T>,
    modifier: Modifier = Modifier,
    crossinline success: @Composable (data: T) -> Unit,
) {
    val (state, data) = value
    Crossfade(
        targetState = state,
        animationSpec = tween(Anim.ActivityLongDurationMills),
        modifier = modifier
    ) {
        when (it) {
            Result.State.Loading -> Placeholder(
                iconResId = R.raw.lt_loading_dots_blue,
                title = "Loading",
            )
            is Result.State.Processing -> Placeholder(
                iconResId = R.raw.lt_loading_hand, title = "Processing."
            )
            is Result.State.Error -> Placeholder(
                iconResId = R.raw.lt_error, title = "Error"
            )
            Result.State.Empty -> Placeholder(
                iconResId = R.raw.lt_empty_box, title = "Oops Empty!!"
            )
            Result.State.Success -> success(data)
        }
    }
}


@Composable
inline fun RowScope.BottomNavigationItem(
    selected: Boolean,
    noinline onClick: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "",
    alwaysShowLabel: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium)
) {
    BottomNavigationItem(selected = selected,
        onClick = onClick,
        icon = {
            Icon(painter = icon, contentDescription = "Bottom Nav Icon")
        },
        modifier = modifier,
        enabled = enabled,
        alwaysShowLabel = alwaysShowLabel,
        interactionSource = interactionSource,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        label = { Label(text = label) })
}
