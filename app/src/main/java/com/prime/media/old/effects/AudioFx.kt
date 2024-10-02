@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class)

package com.prime.media.old.effects

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.prime.media.old.common.LocalNavController
import com.primex.core.rotateTransform
import com.primex.core.textResource
import com.primex.material2.Label
import com.primex.material2.TextButton
import com.zs.core_ui.AppTheme

private const val TAG = "AudioFx"

@OptIn(ExperimentalTextApi::class)
@Composable
@NonRestartableComposable
private fun TopBar(
    enabled: Boolean,
    onToggleState: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material.TopAppBar(
        title = {
            Label(
                text = textResource(R.string.equalizer),
                style = AppTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        backgroundColor = AppTheme.colors.background(1.dp),
        contentColor = AppTheme.colors.onBackground,
        modifier = modifier,
        elevation = 0.dp,
        actions = {
            Switch(checked = enabled, onCheckedChange = onToggleState)
        }
    )
}

@Composable
@NonRestartableComposable
private fun BottomBar(
    modifier: Modifier = Modifier,
    onDismissRequest: (apply: Boolean) -> Unit
) {
    // Buttons
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding.normal)
    ) {
        // In case it is running; it will stop it.
        TextButton(
            label = stringResource(id = R.string.dismiss),
            onClick = { onDismissRequest(false) }
        )

        // start the timer.
        TextButton(
            label = stringResource(id = R.string.apply),
            onClick = { onDismissRequest(true) })
    }
}

@Composable
private fun Equalizer(
    fx: AudioFx,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .sizeIn(maxHeight = 220.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        CompositionLocalProvider(value = LocalContentColor provides  AppTheme.colors.onBackground) {
            // y - axis
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                CompositionLocalProvider(LocalTextStyle provides AppTheme.typography.caption) {
                    Label(
                        text = stringResource(
                            id = R.string.audio_fx_scr_abbr_db_suffix_d,
                            fx.eqBandLevelRange.endInclusive / 1000
                        )
                    )
                    Label(text = stringResource(id = R.string.audio_fx_scr_abbr_db_suffix_d, 0))
                    Label(
                        text = stringResource(
                            id = R.string.audio_fx_scr_abbr_db_suffix_d,
                            fx.eqBandLevelRange.start / 1000
                        )
                    )
                }
            }

            // bars
            repeat(fx.eqNumberOfBands) { band ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Slider(
                        value = fx.eqBandLevels[band],
                        onValueChange = { fx.setBandLevel(band, it) },
                        modifier = Modifier
                            .padding(bottom = ContentPadding.medium)
                            .weight(1f)
                            .rotateTransform(false),
                        valueRange = fx.eqBandLevelRange
                    )

                    Label(
                        text = stringResource(
                            id = R.string.audio_fx_scr_abbr_hz_suffix_d,
                            fx.getBandCenterFreq(band) / 1000
                        ),
                        style = AppTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun Preset(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val color = ChipDefaults.outlinedFilterChipColors(backgroundColor = Color.Transparent)
    FilterChip(
        onClick = onClick,
        colors = color,
        selected = selected,
        enabled = enabled,
        border =
        BorderStroke(1.dp, AppTheme.colors.accent.copy(ChipDefaults.OutlinedBorderOpacity)),
        modifier = modifier.padding(ContentPadding.small)
    ) {
        Label(
            text = label,
            modifier = Modifier.padding(end = ContentPadding.small),
            style = AppTheme.typography.caption
        )

        if (icon == null)
            return@FilterChip
        Icon(
            imageVector = icon,
            contentDescription = label.toString(),
            modifier = Modifier.size(16.dp)
        )
    }
}


private inline val AudioFx.isEqualizerReady
    get() = stateOfEqualizer != AudioFx.EFFECT_STATUS_NOT_READY || stateOfEqualizer != AudioFx.EFFECT_STATUS_NOT_SUPPORTED

@Composable
@NonRestartableComposable
fun AudioFx(
    state: AudioFx
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.compact)
            .background(AppTheme.colors.background(1.dp))
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(Unit) {}
    ) {
        TopBar(state.isEqualizerEnabled, onToggleState = { state.isEqualizerEnabled = it })
        if (state.isEqualizerReady) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ContentPadding.normal, vertical = ContentPadding.medium)
            ) {
                state.eqPresets.forEachIndexed { index, s ->
                    val current = state.eqCurrentPreset
                    Preset(
                        label = s,
                        onClick = { state.eqCurrentPreset = index },
                        selected = current == index
                    )
                }
            }

            Equalizer(
                fx = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ContentPadding.normal, vertical = ContentPadding.medium)
            )
        }
        val controller = LocalNavController.current
        BottomBar(
            onDismissRequest = {
                if (it)
                    state.apply()
                controller.navigateUp()
            }
        )
    }
}
