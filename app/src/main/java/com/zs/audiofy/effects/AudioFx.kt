/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.effects

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.rotateTransform
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ChipDefaults
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.SelectableChip
import com.zs.compose.theme.Slider
import com.zs.compose.theme.Surface
import com.zs.compose.theme.Switch
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.text.Label
import com.zs.audiofy.common.compose.ContentPadding as CP

private val TitleBarHeight = Modifier.height(48.dp)
private val DialogSize = Modifier
    .widthIn(max = 400.dp)
    .padding(horizontal = CP.normal)

private val GridSize = Modifier.sizeIn(maxHeight = 150.dp)


private inline val AudioFxViewState.isEqualizerReady
    get() = stateOfEqualizer != RouteAudioFx.EFFECT_STATUS_NOT_READY || stateOfEqualizer != RouteAudioFx.EFFECT_STATUS_NOT_SUPPORTED

@Composable
fun AudioFx(viewState: AudioFxViewState) {

    val (width, height) = LocalWindowSize.current

    // Handles the logic for showing dialog at bottom/centre
    val view = LocalView.current
    SideEffect {
        val dialogWindowProvider = view.parent as? DialogWindowProvider ?: return@SideEffect
        val gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM
        val window = dialogWindowProvider.window
        window.setGravity(gravity)
    }

    val colors = AppTheme.colors
    val content: @Composable ColumnScope.() -> Unit = {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                stringResource(R.string.equalizer),
                color = colors.accent,
                style = AppTheme.typography.title3
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = viewState.isEqualizerEnabled,
                onCheckedChange = { viewState.isEqualizerEnabled = it }
            )
        }
        // Grid
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = GridSize,
            content = {
                // y - axis labels
                val range = viewState.eqBandLevelRange
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight(),
                    content = {
                        val start = range.start;
                        val end = range.endInclusive
                        Label(
                            text = stringResource(id = R.string.scr_effects_db_suffix_d, start / 1000),
                            style = AppTheme.typography.label3
                        )
                        Label(
                            text = stringResource(id = R.string.scr_effects_db_suffix_d, 0),
                            style = AppTheme.typography.label3
                        )
                        Label(
                            text = stringResource(id = R.string.scr_effects_db_suffix_d, end / 1000),
                            style = AppTheme.typography.label3
                        )
                    }
                )

                // bars
                val padding = Modifier.padding(bottom = CP.medium)
                repeat(viewState.eqNumberOfBands) { band ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val fx = viewState
                        Slider(
                            value = fx.eqBandLevels[band],
                            onValueChange = { fx.setBandLevel(band, it) },
                            modifier = padding
                                .weight(1f)
                                .rotateTransform(false),
                            valueRange = fx.eqBandLevelRange
                        )

                        Label(
                            text = stringResource(id = R.string.scr_effects_hz_suffix_d, fx.getBandCenterFreq(band) / 1000),
                            style = AppTheme.typography.label3
                        )
                    }
                }
            }
        )

        // Presets
        val colors = ChipDefaults.selectableChipColors(
            backgroundColor = colors.background(4.dp),
            contentColor = LocalContentColor.current
        )

        val state = rememberLazyListState()
        LazyRow(
            contentPadding = PaddingValues(horizontal = CP.normal, vertical = CP.medium),
            horizontalArrangement = CP.SmallArrangement,
            content = {
                val current = viewState.eqCurrentPreset
                viewState.eqPresets.forEachIndexed { index, label ->
                    item {
                        SelectableChip(
                            current == index,
                            onClick = { if (index != 0) viewState.eqCurrentPreset = index },
                            colors = colors,
                            content = { Label(label) }
                        )
                    }
                }
            },
            state = state
        )

        // Scroll to current selected.
        LaunchedEffect(Unit) {
            state.animateScrollToItem(viewState.eqCurrentPreset)
        }

    }
    val navController = LocalNavController.current
    val dialogShape = AppTheme.shapes.xLarge
    Surface(
        color = if (colors.isLight) colors.accent else colors.background(3.dp),
        shape = dialogShape,
        modifier = DialogSize,
        border = if (colors.isLight) null else colors.shine,
        content = {
            Column {

                // TitleBar
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            onClick = navController::navigateUp
                        )
                    },
                    title = {
                        Label(
                            textResource(R.string.scr_effects_title),
                            fontWeight = FontWeight.Light,
                            maxLines = 2
                        )
                    },
                    background = Background(Color.Transparent),
                    elevation = 0.dp,
                    contentColor = LocalContentColor.current,
                    modifier = TitleBarHeight,
                    actions = {
                        TonalIconButton(
                            icon = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.scale(0.80f).padding(end = CP.small),
                            onClick = {
                                viewState.apply()
                                navController.navigateUp()
                            }
                        )
                    }
                )

                // Content
                Surface(
                    color = colors.background(1.dp),
                    shape = dialogShape,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    content = {
                        //
                        if (!viewState.isEqualizerReady) return@Surface
                        //
                        Column(
                            verticalArrangement = CP.SmallArrangement,
                            modifier = Modifier.padding(
                                horizontal = CP.medium,
                                vertical = CP.normal
                            ),
                            content = content
                        )
                    }
                )
            }
        }
    )
}