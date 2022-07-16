package com.prime.player.common.compose

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.primex.ui.Label

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlinx.coroutines.delay

@Composable
fun IconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription, tint = tint)
    }
}


@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    contentDescription: String?,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(bitmap = bitmap, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(painter = painter, contentDescription = contentDescription, tint = tint)
    }
}


@Composable
fun ColoredOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = null,
    shape: Shape = RoundedCornerShape(50),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colors.primary,
        disabledContentColor = MaterialTheme.colors.primary.copy(ContentAlpha.disabled),
        backgroundColor = Color.Transparent
    ),
    border: BorderStroke? = BorderStroke(
        2.dp,
        color = colors.contentColor(enabled = enabled).value
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = elevation,
        shape = shape,
        border = border,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}


@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    elevation: Dp = 4.dp,
    color: Color = MaterialTheme.colors.surface,
    placeholder: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions(),
    trailingIcon: @Composable (() -> Unit)? = null,
    query: String,
    onQueryChanged: (query: String) -> Unit,
) {
    Surface(
        shape = shape,
        modifier = Modifier
            .scale(0.85f)
            .then(modifier),
        elevation = elevation,
        color = color,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                if (placeholder != null)
                    Text(text = placeholder)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = trailingIcon,
            keyboardActions = keyboardActions,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
        )
    }
}


/**
 * Composes placeholder with lottie icon.
 */
@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: String? = null,
    action: String? = null,
    onActionTriggered: (() -> Unit)? = null,
    title: String,
) {
    val icon: @Composable () -> Unit =
        @Composable {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(
                    iconResId
                )
            )
            LottieAnimation(
                composition = composition,
                iterations = Int.MAX_VALUE
            )
        }

    val titleLabel =
        @Composable {
            Label(
                text = title,
                maxLines = 2
            )
        }

    val messageText = checkAndEmit(message != null) {
        Text(text = message!!)
    }

    val actionButton = checkAndEmit(action != null) {
        ColoredOutlineButton(
            onClick = onActionTriggered ?: {},
            modifier = Modifier
                //.padding(top = Dp.pLarge)
                .size(width = 200.dp, height = 46.dp),
            elevation = null,
        ) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.padding(end = ContentPadding.normal)
            )
            Text(text = action!!, style = MaterialTheme.typography.button)
        }
    }

    Placeholder(
        modifier = modifier,
        vertical = vertical,
        icon = icon,
        title = titleLabel,
        message = messageText,
        action = actionButton,
    )
}

private fun checkAndEmit(
    condition: Boolean,
    elze: @Composable (() -> Unit)? = null,
    value: @Composable (() -> Unit)
) = if (condition)
    value
else
    elze

private const val DividerAlpha = 0.12f

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
 * Draws divider at the bottom of the composable.
 *
 * A divider is a thin line that groups content in lists and layouts.
 *
 * ![Dividers image](https://developer.android.com/images/reference/androidx/compose/material/dividers.png)
 *
 * @param color color of the divider line
 * @param thickness thickness of the divider line, 1 dp is used by default. Using [Dp.Hairline]
 * will produce a single pixel divider regardless of screen density.
 * @param indent offset of this line, no offset by default
 */
fun Modifier.drawHorizontalDivider(
    color: Color,
    thickness: Dp = 1.dp,
    indent: PaddingValues = PaddingValues(0.dp)
) = drawWithContent {

    // calculate the respective indents.
    val startIndentPx = indent.calculateStartPadding(layoutDirection).toPx()
    val endIndentPx = indent.calculateEndPadding(layoutDirection = layoutDirection).toPx()
    val topIndentPx = indent.calculateTopPadding().toPx()
    val bottomIndentPx = indent.calculateBottomPadding().toPx()

    // width and height of the composable UI element.
    val (width, height) = size

    // constructs offsets of the divider.
    val start = Offset(
        startIndentPx,

        // top will get added and bottom will get subtracted.
        height + topIndentPx - bottomIndentPx
    )

    val end = Offset(
        width - endIndentPx,
        height + topIndentPx - bottomIndentPx
    )

    val thicknessPx = thickness.toPx()

    drawContent()
    drawLine(
        color.copy(DividerAlpha),
        strokeWidth = thicknessPx,
        start = start,
        end = end
    )
}


/**
 * Draws vertical [Divider] at the end of the composable
 * @see drawHorizontalDivider
 */
fun Modifier.drawVerticalDivider(
    color: Color,
    thickness: Dp = 1.dp,
    indent: PaddingValues = PaddingValues(0.dp)
) = drawWithContent {

    // calculate the respective indents.
    val startIndentPx = indent.calculateStartPadding(layoutDirection).toPx()
    val endIndentPx = indent.calculateEndPadding(layoutDirection = layoutDirection).toPx()
    val topIndentPx = indent.calculateTopPadding().toPx()
    val bottomIndentPx = indent.calculateBottomPadding().toPx()

    // width and height of the composable UI element.
    val (width, height) = size

    // constructs offsets of the divider.
    val start = Offset(
        width + startIndentPx,

        // top will get added and bottom will get subtracted.
        topIndentPx
    )

    val end = Offset(
        width - endIndentPx,
        height - bottomIndentPx
    )

    val thicknessPx = thickness.toPx()

    drawContent()
    drawLine(
        color.copy(DividerAlpha),
        strokeWidth = thicknessPx,
        start = start,
        end = end
    )
}


/**
 * A [Modifier] that draws a border around elements that are recomposing. The border increases in
 * size and interpolates from red to green as more recompositions occur before a timeout.
 */
@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able to read/write the value without invalidating (which would cause infinite
        // recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++

        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(3000)
            totalCompositionsAtLastTimeout.value = totalCompositions[0]
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> Color.Green to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                Color.Red.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style
                )
            }
        }
    }