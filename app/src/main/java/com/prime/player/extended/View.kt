package com.prime.player.extended


import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.flaviofaria.kenburnsview.KenBurnsView
import com.flaviofaria.kenburnsview.TransitionGenerator
import com.hanks.htextview.rainbow.RainbowTextView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView

private const val TAG = "View"

@Composable
fun Ticker(
    modifier: Modifier = Modifier,
    charList: String = TickerUtils.provideNumberList(),
    color: Color = LocalContentColor.current,
    duration: Long = 500,
    prefScrollingDirection: TickerView.ScrollingDirection = TickerView.ScrollingDirection.ANY,
    size: TextUnit = 16.sp,
    text: String,
    font: Typeface = Typeface.DEFAULT
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TickerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }) {
        it.setCharacterLists(charList)
        it.textColor = color.toArgb()
        it.textSize = sizePx
        it.animationDuration = duration
        it.setPreferredScrollingDirection(prefScrollingDirection)
        it.textSize = sizePx
        it.typeface = font
        it.text = text
    }
}

/**
 *
 * This composable will load, deserialize, and display an After Effects animation exported with
 * bodymovin (https://github.com/bodymovin/bodymovin).
 *
 * @param speed: Sets the playback speed. If speed < 0, the animation will play backwards.
 * @param repeatX: Sets how many times the animation should be repeated. If the repeat count is 0,
 *                 the animation is never repeated. If the repeat count is greater than 0 or [LottieDrawable.INFINITE],
 *                 the repeat mode will be taken into account. The repeat count is 0 by default
 * @param repeatM: Defines what this animation should do when it reaches the end. This setting is
 *                 applied only when the repeat count is either greater than 0 or
 *                 [LottieDrawable.INFINITE]. Defaults to [LottieDrawable.RESTART].
 */

@SuppressLint("Range")
@Composable
fun Lottie(
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    repeatX: Int = 0,
    repeatM: Int = LottieDrawable.RESTART,
    @FloatRange(from = 0.0, to = 1.0) progress: Float = -1f,
    speed: Float = 1f,
    scale: Float = 1f,
    play: Boolean = false,
    @RawRes res: Int,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LottieAnimationView(context).apply {
                if (autoPlay)
                    playAnimation()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    ) {
        if (progress != -1f)
            it.progress = progress
        if (play)
            it.playAnimation()
        it.setAnimation(res)
        it.repeatCount = repeatX
        it.speed = speed
        it.scale = scale
        it.repeatMode = repeatM
    }
}

@Composable
fun KenBurns(
    modifier: Modifier = Modifier,
    generator: TransitionGenerator? = null,
    contentDescription: String? = null,
    view: KenBurnsView.() -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            KenBurnsView(context).apply {
                if ((generator != null))
                    setTransitionGenerator(generator)
                setContentDescription(contentDescription)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = view
    )
}

@Composable
fun Seekbar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    thumb: Drawable? = null,
    progress: Float,
    onValueChange: (Float) -> Unit,
) {
    var isDragging by androidx.compose.runtime.remember {
        mutableStateOf(false)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SeekBar(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                max = 1000
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser)
                            onValueChange(progress.toFloat() / 1000)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isDragging = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        isDragging = false
                    }
                })

            }
        }
    ) {
        if (!isDragging)
            it.progress = ((progress * 1000).toInt())
        if (thumb != null)
            it.thumb = thumb
        // it.setBackgroundColor(color.toArgb())
        it.progressTintList = ColorStateList.valueOf(color.toArgb())
        it.progressDrawable?.let {
            val layer = it as LayerDrawable
            layer.getDrawable(0).setTint(color.copy(0.45f).toArgb())
        }
    }
}

@Composable
fun RainbowText(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    colorSpeed: Dp = 5.dp,
    text: String,
    textSize: TextUnit = 16.sp,
    typeface: Typeface = Typeface.DEFAULT,
) {
    val speexPX = with(LocalDensity.current) { colorSpeed.toPx() }
    val sizePX = with(LocalDensity.current) { textSize.toPx() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RainbowTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    ) {
        with(it) {
            colors?.map { it.toArgb() }?.toIntArray()?.let {
                setColors(*it)
            }
            this.text = text
            this.textSize = sizePX
            this.typeface = typeface
            this.colorSpeed = speexPX
        }
    }
}


@Composable
fun AnalogController(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
    colorOuterCircle: Color = Color.SignalWhite,
    colorInnerCircle: Color = Color.White,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    fun scale(value: Float, min: Float, max: Float, toMin: Float, toMax: Float): Float {
        val range = max - min
        if (range == 0f) return 0f
        val toRange = toMax - toMin
        Log.d(TAG, "AnalogController: $value, $min, $max, $toMin, $toMax")
        return (((value - min) * toRange) / range) + toMin
    }

    val scaled = scale(value, valueRange.start, valueRange.endInclusive, 0f, 19f)
    val colorLabel = (if (isLight()) Color.Black else Color.White).copy(0.7f)
    AndroidView(
        factory = { context ->
            com.prime.player.extended.views.AnalogController(context).apply {
                setOnProgressChangedListener { value ->
                    onValueChange(
                        scale(value.toFloat(), 0f, 19f, valueRange.start, valueRange.endInclusive)
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setLabel(label)
                progress = scaled.toInt()
                setAccent(accent.toArgb())
                setCircleColors(colorOuterCircle.toArgb(), colorInnerCircle.toArgb())
                isEnabled = enabled
                setLabelColor(colorLabel.toArgb())
            }
        },
        modifier = modifier
    ) {
        with(it) {
            // progress = scaled.toInt()
            setAccent(accent.toArgb())
            setCircleColors(colorOuterCircle.toArgb(), colorInnerCircle.toArgb())
            isEnabled = enabled
            setLabel(label)
            setLabelColor(colorLabel.toArgb())
        }
    }
}
