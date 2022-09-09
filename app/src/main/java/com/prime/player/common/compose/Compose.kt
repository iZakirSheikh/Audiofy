package com.prime.player.common.compose

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.SemanticsProperties.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction.Companion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.primex.ui.ColoredOutlineButton
import com.primex.ui.Label
import com.primex.ui.Placeholder

/**
 * Composes placeholder with lottie icon.
 */
@Composable
fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: String? = null,
    action: @Composable (() -> Unit)? = null
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
                composition = composition,
                iterations = Int.MAX_VALUE
            )
        },
        action = action,
    )
}


@ExperimentalAnimationApi
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    initiallyVisible: Boolean,
    content: @Composable () -> Unit
) = AnimatedVisibility(
    visibleState = remember { MutableTransitionState(initiallyVisible) }
        .apply { targetState = visible },
    modifier = modifier,
    enter = enter,
    exit = exit
) {
    content()
}

@ExperimentalAnimationGraphicsApi
@Composable
fun rememberAnimatedVectorPainter(@DrawableRes id: Int, atEnd: Boolean) =
    androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = id),
        atEnd = atEnd
    )


/**
 * A single line [Label] that is animated using the [AnimatedContent]
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedLabel(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    transitionSpec: AnimatedContentScope<AnnotatedString>.() -> ContentTransform = {
        slideInVertically { height -> height } + fadeIn() with
                slideOutVertically { height -> -height } + fadeOut()
    }
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = transitionSpec,
        modifier = modifier,
        content = {
            Label(
                text = it,
                style = style,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign
            )
        }
    )
}




@OptIn(ExperimentalAnimationApi::class)
@NonRestartableComposable
@Composable
fun AnimatedLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    transitionSpec: AnimatedContentScope<AnnotatedString>.() -> ContentTransform = {
        slideInVertically { height -> height } + fadeIn() with
                slideOutVertically { height -> -height } + fadeOut()
    }
){
   AnimatedLabel(
       text = AnnotatedString(text),
       modifier = modifier,
       style = style,
       color = color,
       fontSize = fontSize,
       fontWeight = fontWeight,
       textAlign = textAlign,
       transitionSpec = transitionSpec
   )
}