package com.prime.player.extended

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val TAG = "Modifiers"


fun Modifier.animate() = animateContentSize(animationSpec = tween(Anim.DURATION_MEDIUM))

fun Modifier.gradientTint(
    colors: List<Color>,
    blendMode: BlendMode,
    brushProvider: (List<Color>, Size) -> Brush
) = composed {
    var size by androidx.compose.runtime.remember { mutableStateOf(Size.Zero) }
    val gradient = remember(colors, size) { brushProvider(colors, size) }
    drawWithContent {
        drawContent()
        size = this.size
        drawRect(
            brush = gradient,
            blendMode = blendMode
        )
    }
}

fun Modifier.gradient(
    colors: List<Color>,
    brushProvider: (List<Color>, Size) -> Brush
): Modifier = composed {
    var size by androidx.compose.runtime.remember { mutableStateOf(Size.Zero) }
    val gradient = remember(colors, size) { brushProvider(colors, size) }
    drawWithContent {
        size = this.size
        drawContent()
        drawRect(brush = gradient)
    }
}

fun Modifier.verticalGradient(
    colors: List<Color> = listOf(
        Color.Transparent,
        Color.Black,
    )
) = gradient(colors) { gradientColors, size ->
    Brush.verticalGradient(
        colors = gradientColors,
        startY = 0f,
        endY = size.height
    )
}

fun Modifier.horizontalGradient(
    colors: List<Color> = listOf(
        Color.Black,
        Color.Transparent
    )
) = gradient(colors) { gradientColors, size ->
    Brush.horizontalGradient(
        colors = gradientColors,
        startX = 0f,
        endX = size.width
    )
}

fun Modifier.radialGradient(
    colors: List<Color> = listOf(
        Color.Transparent,
        Color.Black
    ),
    center: Offset = Offset.Unspecified,
    radius: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp
): Modifier = gradient(colors) { gradientColors, size ->
    Brush.radialGradient(
        colors = gradientColors,
        center = center,
        radius = radius,
        tileMode = tileMode
    )
}

fun Modifier.acquireFocusOnInteraction(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null
): Modifier = composed {
    val interaction = interactionSource ?: androidx.compose.runtime.remember {
        MutableInteractionSource()
    }
    val requester = androidx.compose.runtime.remember {
        FocusRequester()
    }
    val isFocused by interaction.collectIsFocusedAsState()
    Modifier
        .focusRequester(requester)
        .focusable(true, interactionSource = interaction)
        .clickable(
            enabled = !isFocused,
            indication = indication,
            onClick = { requester.requestFocus() },
            interactionSource = androidx.compose.runtime.remember {
                MutableInteractionSource()
            }
        )
        .then(this)
}

private val DEFAULT_FADING_EDGE_LENGTH = 10.dp

fun Modifier.fadeEdge(
    color: Color? = null,
    state: ScrollableState,
    horizontal: Boolean = true,
    length: Dp = DEFAULT_FADING_EDGE_LENGTH,
) = composed(
    debugInspectorInfo {
        name = "length"
        value = length
    }
) {
    val color = color ?: (if (isLight()) Color.White else Color.Black)
    Modifier.drawWithContent {
        val lengthPx = length.toPx()

        val currPosFromStart: Int
        val currPosFromEnd: Int

        when (state) {
            is LazyListState -> {
                val info = state.layoutInfo
                val firstVisibleElementIndex = state.firstVisibleItemIndex
                // the avg item height.
                val elementHeight = this.size.height / info.totalItemsCount
                currPosFromStart = (firstVisibleElementIndex * elementHeight).toInt()
                currPosFromEnd = (this.size.height - currPosFromStart).toInt()
            }
            is ScrollState -> {
                currPosFromStart = state.value
                currPosFromEnd = state.maxValue - currPosFromStart
            }
            else -> error("The $state is not supported.")
        }

        val from: Float = lengthPx * (currPosFromStart / lengthPx).coerceAtMost(1f)
        val to: Float = lengthPx * (currPosFromEnd / lengthPx).coerceAtMost(1f)


        drawContent()

        when (horizontal) {
            true -> {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color,
                            Color.Transparent,
                        ),
                        startX = 0f,
                        endX = from,
                    ),
                    size = Size(
                        from,
                        this.size.height,
                    ),
                )

                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color,
                        ),
                        startX = size.width - to,
                        endX = size.width,
                    ),
                    topLeft = Offset(x = size.width - to, y = 0f),
                )
            }
            false -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color,
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = from,
                    ),
                    size = Size(
                        this.size.width,
                        from
                    ),
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color,
                        ),
                        startY = size.height - to,
                        endY = size.height,
                    ),
                    topLeft = Offset(x = 0f, y = size.height - to),
                )
            }
        }
    }
}


