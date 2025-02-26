/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 11-10-2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.zs.core_ui

import androidx.annotation.RawRes
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottiePainter
import com.primex.core.drawHorizontalDivider
import com.primex.core.thenIf
import com.primex.material2.Label
import kotlin.math.roundToLong

private const val TAG = "Delegates"

/**
 * A composable function that delegates to [LottieAnimation] painter and behaves like [AndroidVectorDrawable].
 * This overload animates between the start and end frames of the animation based on the value of `atEnd`.
 *
 * @param id The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param atEnd A boolean parameter that determines whether to display the end-frame or the start
 *              frame of the animation. The change in value causes animation.
 * @param duration The duration of the animation in milliseconds. The default value is -1, which
 *                 means the animation will use the duration specified in the
 *                 [LottieCompositionSpec] object. If a positive value is given, it will override
 *                 the duration from the [LottieCompositionSpec] object.
 * @param progressRange A range of float values that specifies the start and end frames of the
 *                      animation. The default range is 0f..1f, which means the animation will
 *                      start from the first frame and end at the last frame. Some [Lottie]
 *                      animation files may have different start/end frames, and this parameter
 *                      can be used to adjust them accordingly.
 * @param easing The easing function to use for the animation.
 */
@Composable
fun lottieAnimationPainter(
    @RawRes id: Int,
    atEnd: Boolean,
    duration: Int = -1,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    easing: Easing = FastOutSlowInEasing
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToLong() ?: AnimationConstants.DefaultDurationMillis
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2.toInt() else duration, easing = easing)
    )
    return rememberLottiePainter(
        composition = composition,
        progress = progress,


        )
}

/**
 * A composable function that delegates to [LottieAnimation] painter and behaves like [AndroidVectorDrawable].
 * This overload animates the Lottie composition according to the given parameters.
 *
 * @param id The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param speed The speed at which the animation plays.
 * @param repeatMode The repeat mode for the animation.
 * @param iterations The number of iterations to play the animation.
 * @see lottieAnimationPainter
 */
@Composable
fun lottieAnimationPainter(
    @RawRes id: Int,
    speed: Float = 1f,
    repeatMode: RepeatMode = RepeatMode.Restart,
    iterations: Int = Int.MAX_VALUE,
    dynamicProperties: LottieDynamicProperties? = null
): Painter {
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        reverseOnRepeat = repeatMode == RepeatMode.Reverse,
    )
    return rememberLottiePainter(
        composition = composition,
        progress = progress,
        dynamicProperties = dynamicProperties
    )
}

/**
 * A composable function that delegates to [LottieAnimation] and behaves like [AndroidVectorDrawable].
 *
 * @param atEnd: A boolean parameter that determines whether to display the end-frame or the start
 *              frame of the animation. The change in value causes animation.
 * @param id: The resource identifier of the [LottieCompositionSpec.RawRes] type.
 * @param scale: A float parameter that adjusts the size of the animation. The default size is
 *               24.dp, and the scale can be used to increase or decrease it.
 * @param progressRange: A range of float values that specifies the start and end frames of the
 *                       animation. The default range is 0f..1f, which means the animation will
 *                       start from the first frame and end at the last frame. Some [Lottie]
 *                       animation files may have different start/end frames, and this parameter
 *                       can be used to adjust them accordingly.
 * @param duration: The duration of the animation in milliseconds. The default value is -1, which
 *                  means the animation will use the duration specified in the
 *                  [LottieCompositionSpec] object. If a positive value is given, it will override
 *                  the duration from the [LottieCompositionSpec] object.
 */
@Composable
inline fun LottieAnimatedIcon(
    @RawRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    scale: Float = 1f,
    progressRange: ClosedFloatingPointRange<Float> = 0f..1f,
    duration: Int = -1,
    easing: Easing = FastOutSlowInEasing
) {
    /*Icon(
        painter = lottieAnimationPainter(id = id, atEnd = atEnd, duration, progressRange, easing),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .then(modifier),
    )*/
    val composition by rememberLottieComposition(spec = LottieCompositionSpec.RawRes(id))
    val duration2 = composition?.duration?.roundToLong() ?: AnimationConstants.DefaultDurationMillis.toLong()
    val progress by animateFloatAsState(
        targetValue = if (atEnd) progressRange.start else progressRange.endInclusive,
        label = "Lottie $id",
        animationSpec = tween(if (duration == -1) duration2.toInt() else duration, easing = easing)
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .size(24.dp * scale)
            .then(modifier),
    )
}




/**
 * Creates a header with an optional action.
 *
 * @param text The text to display in the header. max 2 lines one for title and other subtitle
 * @param modifier The [Modifier] to be applied to the header.
 * @param style The [TextStyle] to be applied to the header text.
 * @param contentPadding The padding to be applied around the header content.
 * @param action An optional composable function to display an action within the header.*/
@Composable
inline fun Header(
    text: CharSequence,
    crossinline leading: @Composable (() -> Unit) = {},
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.headlineSmall,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    color: Color = LocalContentColor.current,
    drawDivider: Boolean = false,
    crossinline action: @Composable () -> Unit
) = Row(
    modifier = modifier
        .padding(contentPadding)
        .fillMaxWidth()
        .thenIf(drawDivider){
            drawHorizontalDivider(color = color)
                .padding(bottom = ContentPadding.medium)
        },
    // horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium),
    verticalAlignment = Alignment.CenterVertically,
    content = {
        CompositionLocalProvider(LocalContentColor provides color) {
            // leading
            leading()
            // Title
            Label(
                style = style,
                text = text,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            // action.
            action()
        }
    }
)


/**
 * @see Header
 */
@Composable
inline fun Header(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.headlineSmall,
    color: Color = LocalContentColor.current,
    drawDivider: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) = Label(
    text = text,
    modifier = modifier
        .padding(contentPadding)
        .fillMaxWidth()
        .thenIf(drawDivider){
            drawHorizontalDivider(color = color)
                .padding(bottom = ContentPadding.medium)
        },
    style = style,
    maxLines = 2,
    color = color
)