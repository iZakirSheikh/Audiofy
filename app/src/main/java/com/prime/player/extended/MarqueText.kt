package com.prime.player.extended

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "MarqueText"


@Composable
fun MarqueText(
    modifier: Modifier = Modifier,
    text: String,
    textSize: TextUnit = 16.sp,
    textColor: Color = LocalContentColor.current,
    fadeEdge: Boolean = true,
    fadingEdgeLength: Dp = 16.dp,
    marqueeRepeatLimit: Int = -1, //Infinitely
    typeface: Typeface = Typeface.DEFAULT,
){
    MarqueText(
        modifier = modifier,
        text = AnnotatedString(text = text),
        textSize = textSize,
        textColor = textColor,
        fadeEdge = fadeEdge,
        fadingEdgeLength = fadingEdgeLength,
        marqueeRepeatLimit = marqueeRepeatLimit,
        typeface = typeface,
    )
}


@Composable
fun MarqueText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    textSize: TextUnit = 16.sp,
    textColor: Color = LocalContentColor.current,
    fadeEdge: Boolean = true,
    fadingEdgeLength: Dp = 16.dp,
    marqueeRepeatLimit: Int = -1, //Infinitely
    typeface: Typeface = Typeface.DEFAULT,
) {
    val fadingEdgeLengthPx = with(LocalDensity.current) { fadingEdgeLength.toPx() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MarqueText(context).apply {
                this.textSize = textSize.value
                setTextColor(textColor.toArgb())
                ellipsize = TextUtils.TruncateAt.MARQUEE
                setFadingEdgeLength(fadingEdgeLengthPx.toInt())
                maxLines = 1
                isSingleLine = true
                setText(text)
                this.typeface = typeface
                this.marqueeRepeatLimit = marqueeRepeatLimit
                if (fadeEdge)
                    isHorizontalFadingEdgeEnabled = true
            }
        }) {
        it.setTextColor(textColor.toArgb())
        it.text = text
    }
}


private class MarqueText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        isSelected = true
        addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                val layoutParams = layoutParams
                layoutParams.height = bottom - top
                layoutParams.width = right - left
                removeOnLayoutChangeListener(this)
                setLayoutParams(layoutParams)
            }
        })
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        if (focused) super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (hasWindowFocus) super.onWindowFocusChanged(hasWindowFocus)
    }

    override fun hasWindowFocus(): Boolean {
        return true
    }

    override fun isFocused(): Boolean {
        return true
    }

    override fun hasFocus(): Boolean {
        return true
    }
}