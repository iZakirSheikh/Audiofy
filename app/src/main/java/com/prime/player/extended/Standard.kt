package com.prime.player.extended

import androidx.annotation.RawRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieDrawable

private const val TAG = "Standard"

@Composable
fun Label(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        color = color,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        fontSize = fontSize,
        textAlign = textAlign
    )
}

@Composable
fun Label(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Label(
    modifier = modifier,
    style = style,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = AnnotatedString(text),
    fontSize = fontSize,
    textAlign = textAlign
)

@Composable
fun Label1(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.body1,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = AnnotatedString(text),
    textAlign = textAlign
)

@Composable
fun Label1(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.body1,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = text,
    textAlign = textAlign
)

@Composable
fun Label2(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.body2,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = AnnotatedString(text),
    textAlign = textAlign
)

@Composable
fun Label2(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.body2,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = text,
    textAlign = textAlign
)

@Composable
fun Header(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.h6,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = style.fontSize,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) = Label(
    modifier = modifier,
    style = style,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = text,
    fontSize = fontSize,
    textAlign = textAlign
)

@Composable
fun Header(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.h6,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = style.fontSize,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Label(
    modifier = modifier,
    style = style,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = AnnotatedString(text),
    fontSize = fontSize,
    textAlign = textAlign
)

@Composable
fun Header5(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.h5,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = AnnotatedString(text),
    textAlign = textAlign
)

@Composable
fun Header5(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) = Label(
    modifier = modifier,
    style = MaterialTheme.typography.h5,
    color = color,
    maxLines = maxLines,
    fontWeight = fontWeight,
    text = text,
    textAlign = textAlign
)

@Composable
fun Caption(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: AnnotatedString,
) = Label(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.caption,
    color = color,
    fontWeight = fontWeight,
    textAlign = textAlign,
    maxLines = maxLines
)

@Composable
fun Caption(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    text: String,
) = Caption(
    text = AnnotatedString(text),
    modifier = modifier,
    color = color,
    fontWeight = fontWeight,
    textAlign = textAlign,
    maxLines = maxLines
)

@Composable
fun CircularProgressIndicator(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth,
    backgroundColor: Color = color.copy(alpha = ProgressIndicatorDefaults.IndicatorBackgroundOpacity),
    content: @Composable() (BoxScope.() -> Unit)? = null
) {
    Box(modifier = modifier) {


        val value by animateFloatAsState(targetValue = progress)

        val animatedColor by animateColorAsState(targetValue = color)

        androidx.compose.material.CircularProgressIndicator(
            progress = value,
            color = animatedColor,
            strokeWidth = strokeWidth,
            modifier = Modifier.fillMaxSize(),

        )

        if (content != null)
            content()

        //background
        androidx.compose.material.CircularProgressIndicator(
            progress = 1f,
            color = backgroundColor,
            strokeWidth = strokeWidth,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ProgressButton(
    modifier: Modifier = Modifier,
    progress: Float,
    text: String,
    borderStroke: BorderStroke? = null,
    color: Color = MaterialTheme.colors.primary,
    onClick: () -> Unit
) {
    val new by animateColorAsState(
        targetValue = color,
        animationSpec = tween(Anim.DURATION_MEDIUM)
    )

    val progressT by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(Anim.DURATION_MEDIUM)
    )

    TextButton(
        onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = suggestContentColorFor(new)
        ),
        contentPadding = PaddingValues(0.dp),
        border = borderStroke
    ) {

        Box(modifier = Modifier.fillMaxWidth()) {

            LinearProgressIndicator(
                progress = progressT,
                color = color,
                modifier = Modifier.fillMaxSize(),
            )

            Label(
                text = text,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun PlaceHolder(
    modifier: Modifier = Modifier,
    size: Dp = 156.dp,
    @RawRes lottieResource: Int = -1,
    message: String,
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Crossfade(targetState = lottieResource) {
            if (it != -1) {
                Lottie(
                    res = it,
                    autoPlay = true,
                    repeatX = LottieDrawable.INFINITE,
                    modifier = Modifier
                        //.padding(0.dp)
                        .requiredSize(size),
                )
            }
        }
        Label(
            text = message,
            maxLines = 2,
            style = MaterialTheme.typography.body1,
            color = LocalContentColor.current.copy(0.7f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    secondaryText: @Composable (() -> Unit)? = null,
    overlineText: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    text: @Composable () -> Unit
) {
    val typography = MaterialTheme.typography

    val styledText = applyTextStyle(
        typography.subtitle1,
        ContentAlpha.high,
        text
    )!!
    val styledSecondaryText = applyTextStyle(
        typography.body2,
        ContentAlpha.medium,
        secondaryText
    )
    val styledOverlineText = applyTextStyle(
        typography.overline,
        ContentAlpha.high,
        overlineText
    )
    val styledTrailing = applyTextStyle(typography.caption, ContentAlpha.high, trailing)


    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        icon?.invoke()
        Column(
            modifier = Modifier
                .padding(horizontal = Padding.MEDIUM)
                .weight(1f), verticalArrangement = Arrangement.Center
        ) {
            styledOverlineText?.invoke()
            styledText.invoke()
            styledSecondaryText?.invoke()
        }
        styledTrailing?.invoke()
    }
}

private fun applyTextStyle(
    textStyle: TextStyle,
    contentAlpha: Float,
    icon: @Composable (() -> Unit)?
): @Composable (() -> Unit)? {
    if (icon == null) return null
    return {
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            ProvideTextStyle(textStyle, icon)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Header(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight? = FontWeight.SemiBold,
    style: TextStyle = MaterialTheme.typography.h6,
    secondaryText: String? = null,
    text: String,
    backgroundColor: Color = Color.Transparent,
    maxLines: Int = 1,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val leading: (@Composable () -> Unit)? = leadingIcon?.let {
        @Composable {
            Icon(imageVector = it, contentDescription = null)
        }
    }

    val secondary: (@Composable () -> Unit)? = secondaryText?.let {
        @Composable {
            Caption(text = it)
        }
    }

    Surface(modifier = modifier, color = backgroundColor, elevation = 0.dp) {
        ListItem(
            icon = leading,
            text = {
                Crossfade(targetState = text) {
                    Label(
                        text = it,
                        style = style,
                        color = color,
                        fontWeight = fontWeight,
                        maxLines = maxLines
                    )
                }
            },
            secondaryText = secondary,
            trailing = trailing,
        )
    }
}

@Composable
fun HorizontalIndicator(
    modifier: Modifier = Modifier,
    activeColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    inactiveColor: Color = activeColor.copy(ContentAlpha.disabled),
    indicatorWidth: Dp = 8.dp,
    indicatorHeight: Dp = indicatorWidth,
    spacing: Dp = indicatorWidth,
    indicatorShape: Shape = CircleShape,
    count: Int,
    current: Int
) {

    val indicatorWidthPx = LocalDensity.current.run { indicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicatorModifier = Modifier
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(color = inactiveColor, shape = indicatorShape)

            repeat(count) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val scrollPosition = (current)
                        .coerceIn(
                            0,
                            (count - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                                .toInt()
                        )
                    IntOffset(
                        x = ((spacingPx + indicatorWidthPx) * scrollPosition),
                        y = 0
                    )
                }
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(
                    color = activeColor,
                    shape = indicatorShape,
                )
        )
    }
}

@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    simple: Boolean = true,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMsg: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = if (simple)
        TextFieldDefaults.textFieldColors()
    else
        TextFieldDefaults.outlinedTextFieldColors()
) {
    Column(modifier = modifier.animateContentSize()) {
        val leader: (@Composable () -> Unit)? = leadingIcon?.let {
            @Composable {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.labelColor(
                        enabled = enabled,
                        error = isError,
                        interactionSource = interactionSource
                    ).value
                )
            }
        }

        val labeler: (@Composable () -> Unit)? = label?.let {
            @Composable {
                Label(text = it)
            }
        }

        val holder: (@Composable () -> Unit)? = placeholder?.let {
            @Composable {
                Label(
                    text = it,
                    fontWeight = null,
                    style = textStyle,
                )
            }
        }

        when (simple) {
            true -> {
                TextField(
                    value = value,
                    singleLine = true,
                    readOnly = readOnly,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = textStyle,
                    shape = shape,
                    label = if (label == null) null else labeler,
                    placeholder = if (placeholder == null) null else holder,
                    isError = isError,
                    trailingIcon = trailingIcon,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    colors = colors,
                    leadingIcon = if (leadingIcon == null) null else leader
                )
            }
            else -> {
                OutlinedTextField(
                    value = value,
                    singleLine = true,
                    readOnly = readOnly,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = textStyle,
                    shape = shape,
                    label = if (label == null) null else labeler,
                    placeholder = if (placeholder == null) null else holder,
                    isError = isError,
                    leadingIcon = if (leadingIcon == null) null else leader,
                    trailingIcon = trailingIcon,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    colors = colors,
                )
            }
        }

        Crossfade(targetState = errorMsg) {
            if (it != null && enabled)
                Caption(text = it, color = MaterialTheme.colors.error)
        }
    }
}

@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    value: String,
    simple: Boolean = true,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMsg: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    interactionSource: MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = if (simple) TextFieldDefaults.textFieldColors() else TextFieldDefaults.outlinedTextFieldColors()
) {
    var textFieldValueState by androidx.compose.runtime.remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)

    TextInputField(
        modifier = modifier,
        value = textFieldValue,
        simple = simple,
        onValueChange = {
            textFieldValueState = it
            if (value != it.text) {
                onValueChange(it.text)
            }
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        errorMsg = errorMsg,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}



