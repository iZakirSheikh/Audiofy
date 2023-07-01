@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.core.compose

import androidx.annotation.RawRes
import androidx.compose.animation.*
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.media.R
import com.primex.material2.*

@Composable
inline fun Image(
    data: Any?,
    modifier: Modifier = Modifier,
    fallback: Painter = painterResource(id = R.drawable.default_art),
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fadeMills: Int = AnimationConstants.DefaultDurationMillis,
) {
    val context = LocalContext.current
    val request = remember(data) {
        ImageRequest.Builder(context).data(data).crossfade(fadeMills).build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        error = fallback,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

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
@Deprecated("Doesn't required.", level = DeprecationLevel.HIDDEN)
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


