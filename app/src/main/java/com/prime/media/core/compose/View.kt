package com.prime.media.core.compose


import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.flaviofaria.kenburnsview.KenBurnsView
import com.flaviofaria.kenburnsview.TransitionGenerator
import com.zs.core_ui.AppTheme

private const val TAG = "View"

@Composable
@Deprecated("find better alternative")
fun KenBurns(
    modifier: Modifier = Modifier,
    generator: TransitionGenerator? = null,
    contentDescription: String? = null,
    view: KenBurnsView.() -> Unit,
) {
    AndroidView(
        modifier = modifier, factory = { context ->
            KenBurnsView(context).apply {
                if ((generator != null)) setTransitionGenerator(generator)
                setContentDescription(contentDescription)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipToOutline = true
            }
        }, update = view
    )
}

@Deprecated(message = "Don't use it!!", level = DeprecationLevel.HIDDEN)
@Composable
fun Seekbar(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    thumb: Drawable? = null,
    progress: Float,
    onValueChange: (Float) -> Unit,
) {
    var isDragging by androidx.compose.runtime.remember {
        mutableStateOf(false)
    }
    AndroidView(modifier = modifier, factory = { context ->
        SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    if (fromUser) onValueChange(progress.toFloat() / 1000)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isDragging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isDragging = false
                }
            })

        }
    }) {
        if (!isDragging) it.progress = ((progress * 1000).toInt())
        if (thumb != null) it.thumb = thumb
        // it.setBackgroundColor(color.toArgb())
        it.progressTintList = ColorStateList.valueOf(color.toArgb())
        it.progressDrawable?.let {
            val layer = it as LayerDrawable
            layer.getDrawable(0).setTint(color.copy(0.45f).toArgb())
        }
    }
}


/**
 * A wrapper around Media3 [PlayerView]
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun PlayerView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                hideController()
                useController = false
                this.player = player
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipToOutline = true
                // Set the Background Color of the player as Solid Black Color.
                setBackgroundColor(Color.Black.toArgb())
                keepScreenOn = true
            }
        },
        update = { it.resizeMode = resizeMode; it.player = player;  it.keepScreenOn = true },
        onRelease = {it.player = null; it.keepScreenOn = false}
    )
}