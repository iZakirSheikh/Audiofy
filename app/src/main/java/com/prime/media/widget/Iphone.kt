@file:OptIn(ExperimentalMaterialApi::class, ExperimentalSharedTransitionApi::class)

package com.prime.media.widget

import android.text.format.DateUtils
import android.util.Log
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.prime.media.common.chronometer
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LottieAnimButton
import com.prime.media.old.common.marque
import com.prime.media.old.console.Console
import com.primex.core.SignalWhite
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private const val TAG = "Iphone"

private val DefaultArtworkSize = 84.dp
private val DefaultArtworkShape = RoundedCornerShape(20)
private val Shape = RoundedCornerShape(14)

/**
 * A mini-player inspired by i-phone
 */
@Composable
fun Iphone(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val accent = AppTheme.colors.accent
    val navController = LocalNavController.current

    // Real Content
    ListTile(
        onColor = Color.SignalWhite,
        modifier = modifier
            .thenIf(
                !showcase
            ) {
                sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn()
                )
            }
            .shadow(Glance.ELEVATION, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(Color.Black),
        overline = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },
        headline = {
            Label(
                state.title ?: textResource(R.string.unknown),
                modifier = Modifier.marque(Int.MAX_VALUE)
            )
        },
        leading = {
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .clip(DefaultArtworkShape),
            )
        },
        trailing = {
            // Expand to fill
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                //   tint = accent
                onClick = { navController.navigate(Console.route); onDismissRequest() },
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
                    val ctx = LocalContext.current
                    val color = LocalContentColor.current.copy(ContentAlpha.medium)
                    // Skip to Prev
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color
                    )

                    // Play Toggle
                    LottieAnimButton(
                        id = R.raw.lt_play_pause,
                        atEnd = !state.playing,
                        scale = 1.5f,
                        progressRange = 0.0f..0.29f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                        dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.STROKE_COLOR,
                                accent.toArgb(),
                                "**"
                            )
                        )
                    )

                    // Skip to Next
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
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
                    val chronometer = state.chronometer
                    val position = chronometer.value
                    // Position
                    Label(
                        when (position) {
                            -1L-> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((chronometer.value / 1000 ))
                        },
                        style = AppTheme.typography.caption,
                        color = LocalContentColor.current.copy(ContentAlpha.medium)
                    )

                    // TimeBar
                    val ctx = LocalContext.current
                    val newProgress = if (chronometer.value != -1L) chronometer.value / state.duration.toFloat() else 0f
                    WavySlider(
                        newProgress,
                        onValueChange = {
                            if (position == -1L) return@WavySlider
                            Log.d(TAG, "Iphone: $it")
                            chronometer.value = ((it * state.duration).roundToLong())
                            NowPlaying.trySend(ctx, NowPlaying.ACTION_SEEK_TO){
                                putExtra(NowPlaying.EXTRA_SEEK_PCT, it)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        // idp because 0 dp is not supported.
                        waveLength = if (!state.playing) 0.dp else 20.dp,
                        waveHeight = if (!state.playing) 0.dp else 7.dp,
                        incremental = true,
                        colors = SliderDefaults.colors(
                            activeTrackColor = accent,
                            thumbColor = Color.Transparent
                        )
                    )

                    // Duration
                    Label(
                        when  {
                            position == -1L -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime(((state.duration) / 1000))
                        },
                        style = AppTheme.typography.caption,
                        color = LocalContentColor.current.copy(ContentAlpha.medium)
                    )
                }
            )
        }
    )
}