@file:OptIn(ExperimentalMaterialApi::class, ExperimentalSharedTransitionApi::class)

package com.prime.media.old.widget

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.zs.core_ui.Anim
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.MediumDurationMills
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LottieAnimButton
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.common.marque
import com.primex.core.thenIf
import com.prime.media.old.core.playback.artworkUri
import com.prime.media.old.core.playback.mediaUri
import com.prime.media.old.core.playback.subtitle
import com.prime.media.old.core.playback.title
import com.primex.core.SignalWhite
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private val DefaultArtworkSize = 84.dp
private val DefaultArtworkShape = RoundedCornerShape(20)
private val Shape = RoundedCornerShape(14)

/**
 * A mini-player inspired by i-phone
 */
@Composable
fun Iphone(
    item: MediaItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    duration: Long = C.TIME_UNSET,
    progress: Float = 0.0f,
    onSeek: (progress: Float) -> Unit = {},
    onAction: (action: String) -> Unit = {},
) {
    val accent = com.zs.core_ui.AppTheme.colors.accent
    ListTile(
        onColor = Color.SignalWhite,
        modifier = modifier
            .thenIf(
                item.mediaUri != Uri.EMPTY){sharedBounds(
                Glance.SHARED_BACKGROUND_ID,
                exit = fadeOut() + scaleOut(),
                enter = fadeIn() + scaleIn()
            )}
            .shadow(Glance.ELEVATION, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(Color.Black)
        ,
        overline = {
            Label(
                item.subtitle.toString(),
                style = com.zs.core_ui.AppTheme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },
        headline = { Label(item.title.toString(), modifier = Modifier.marque(Int.MAX_VALUE)) },
        leading = {
            Artwork(
                data = item.artworkUri,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .thenIf(
                        item.mediaUri != Uri.EMPTY){sharedElement(Glance.SHARED_ARTWORK_ID)}
                    .clip(DefaultArtworkShape),
            )
        },
        trailing = {
            // Expand to fill
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                //   tint = accent
                onClick = { onAction(Glance.ACTION_LAUCH_CONSOLE) },
                modifier = Modifier
                    .scale(0.9f)
                    .offset(x = 14.dp),
            )
        },
        subtitle = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    val color = LocalContentColor.current.copy(ContentAlpha.medium)
                    // SeekBackward
                    IconButton(
                        onClick = { onAction(Glance.ACTION_PREV_TRACK) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color
                    )

                    val properties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.STROKE_COLOR,
                            accent.toArgb(),
                            "**"
                        )
                    )
                    // Play Toggle
                    LottieAnimButton(
                        id = R.raw.lt_play_pause,
                        atEnd = !playing,
                        scale = 1.5f,
                        progressRange = 0.0f..0.29f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = { onAction(Glance.ACTION_PLAY) },
                        dynamicProperties = properties
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onAction(Glance.ACTION_NEXT_TRACK) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color
                    )
                }
            )
        },
        footer = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // show playing bars.
                    LottieAnimation(
                        id = R.raw.playback_indicator,
                        iterations = Int.MAX_VALUE,
                        dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.COLOR,
                                accent.toArgb(),
                                "**"
                            )
                        ),
                        modifier = Modifier
                            .thenIf(
                                item.mediaUri != Uri.EMPTY){sharedBounds(Glance.SHARED_PLAYING_BARS_ID)}
                            .requiredSize(24.dp),
                        isPlaying = playing,
                    )


                    // played duration
                    Label(
                        when (duration) {
                            C.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((duration / 1000 * progress).roundToLong())
                        },
                        style = com.zs.core_ui.AppTheme.typography.caption,
                        color = LocalContentColor.current.copy(ContentAlpha.medium)
                    )

                    // slider
                    WavySlider(
                        value = progress.fastCoerceIn(0f, 1f),
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f),
                        // idp because 0 dp is not supported.
                        waveLength = if (!playing) 0.dp else 20.dp,
                        waveHeight = if (!playing) 0.dp else 7.dp,
                        incremental = true,
                        colors = SliderDefaults.colors(
                            activeTrackColor = accent,
                            thumbColor = Color.Transparent
                        ),
                    )

                    // total duration
                    Label(
                        when (duration) {
                            C.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((duration / 1000))
                        },
                        style = com.zs.core_ui.AppTheme.typography.caption,
                        color = LocalContentColor.current.copy(ContentAlpha.medium)
                    )
                    // control centre
                    IconButton(
                        imageVector = Icons.Outlined.Tune,
                        onClick = { onAction(Glance.ACTION_LAUNCH_CONTROL_PANEL) }
                    )
                }
            )
        }
    )
}