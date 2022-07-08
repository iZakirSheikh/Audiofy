package com.prime.player.common.compose

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
import com.prime.player.Material
import com.prime.player.Padding
import com.primex.ui.Label

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    imageVector: ImageVector,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}


@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    bitmap: ImageBitmap,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(bitmap = bitmap, contentDescription = contentDescription)
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    painter: Painter,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(painter = painter, contentDescription = contentDescription)
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
                modifier = Modifier.padding(end = Padding.Normal)
            )
            Text(text = action!!, style = Material.typography.button)
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

fun checkAndEmit(
    condition: Boolean,
    elze: @Composable (() -> Unit)? = null,
    value: @Composable (() -> Unit)
) = if (condition)
    value
else
    elze

private const val DividerAlpha = 0.12f

fun Modifier.drawVerticalDivider(
    color: Color,
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp,
    bottomIndent: Dp = 0.dp
) =
    drawWithContent {

        val thicknessPx = thickness.toPx()
        val startIndentPx = startIndent.toPx()
        val topIndentPx = bottomIndent.toPx()

        val padding = PaddingValues(12.dp)


        val (width, height) = size

        val start = Offset(
            startIndentPx,
            height - topIndentPx
        )

        val end = Offset(
            width,
            height - topIndentPx
        )

        drawContent()
        drawLine(
            color.copy(DividerAlpha),
            strokeWidth = thicknessPx,
            start = start,
            end = end
        )
    }





@ExperimentalAnimationApi
@Composable
fun AnimateVisibility(
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