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

package com.prime.media.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.preference
import com.primex.core.textArrayResource
import com.primex.core.textResource
import com.primex.material2.DropDownPreference
import com.primex.material2.Label
import com.primex.material2.SliderPreference
import com.primex.material2.SwitchPreference
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Header
import com.zs.core_ui.NightMode
import kotlin.math.roundToInt
import com.prime.media.settings.RouteSettings as RS

private const val CONTENT_TYPE_PREF = "preference"
private const val TAG = "Preferences"

context(_: RS)
fun LazyListScope.preferences(viewState: SettingsViewState) {
    // General
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(R.string.general),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }

    // Recycle Bin
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(R.string.pref_enable_trash_can),
            checked = viewState.trashCanEnabled,
            onCheckedChange = { viewState.trashCanEnabled = it },
            icon = Icons.Outlined.Recycling,
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape),
        )
    }

    // Legacy Artwork Method
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(R.string.pref_fetch_artwork_from_media_store),
            checked = viewState.preferCachedThumbnails,
            onCheckedChange = { viewState.preferCachedThumbnails = it },
            icon = Icons.Outlined.Camera,
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape),
        )
    }

    // Exclude Track Duration
    // The duration from which below tracks are excluded from the library.
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            text = textResource(R.string.pref_minimum_track_length),
            value = viewState.minTrackLengthSecs.toFloat(),
            onRequestChange = { viewState.minTrackLengthSecs = it.toInt() },
            valueRange = 0f..100f,
            steps = 9,
            icon = Icons.Outlined.Straighten,
            preview = {
                Label(
                    text = textResource(R.string.postfix_s_d, it.roundToInt()),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .wrapContentSize(Alignment.Center)
                )
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape),
        )
    }

    // Appearance
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(R.string.appearance),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }

    // Night Mode Strategy
    // The strategy to use for night mode.
    item(contentType = CONTENT_TYPE_PREF) {
        val strategy by preference(Settings.NIGHT_MODE)
        val entries = textArrayResource(R.array.pref_night_mode_entries)
        DropDownPreference(
            text = textResource(R.string.pref_app_theme_s, entries[strategy.ordinal]),
            value = strategy,
            icon = Icons.Default.LightMode,
            entries = entries,
            onRequestChange = { viewState.set(Settings.NIGHT_MODE, it) },
            values = NightMode.values(),
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape)
        )
    }

    // Accent nav
    item(contentType = CONTENT_TYPE_PREF) {
        val use by preference(Settings.USE_ACCENT_IN_NAV_BAR)
        SwitchPreference(
            checked = use,
            text = textResource(R.string.pref_accent_nav),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.USE_ACCENT_IN_NAV_BAR, should)
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Colorization Strategy
    item(contentType = CONTENT_TYPE_PREF) {
        val colorizationStrategy by preference(Settings.COLORIZATION_STRATEGY)
        SwitchPreference(
            checked = colorizationStrategy == ColorizationStrategy.Wallpaper,
            text = textResource(R.string.pref_colorization_strategy),
            onCheckedChange = { should: Boolean ->
                val strategy =
                    if (should) ColorizationStrategy.Wallpaper else ColorizationStrategy.Default
                viewState.set(Settings.COLORIZATION_STRATEGY, strategy)
            },
            icon = Icons.Default.ColorLens,
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // App font scale
    // The font scale to use for the app if -1 is used, the system font scale is used.
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            value = viewState.fontScale,
            text = textResource(R.string.pref_font_scale),
            valueRange = 0.7f..2f,
            steps = 12,   // steps=(max−min)stepSize−1/ (2.0 - 0.7) / 0.1 - 1 =  12 steps
            icon = Icons.Outlined.FormatSize,
            preview = {
                Label(
                    text = if (it < 0.76f) textResource(R.string.system) else textResource(
                        R.string.postfix_x_f,
                        it
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                val newValue = if (value < 0.76f) -1f else value
                viewState.fontScale = newValue
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Grid Item Multiplier
    // The multiplier increases/decreases the size of the grid item from 0.6 to 2f
    item(contentType = CONTENT_TYPE_PREF) {
        SliderPreference(
            value = viewState.gridItemSizeMultiplier,
            text = textResource(R.string.pref_grid_item_size_multiplier),
            valueRange = 0.6f..2f,
            steps = 13, // (2.0 - 0.7) / 0.1 = 13 steps
            icon = Icons.Outlined.Dashboard,
            preview = {
                Label(
                    text = textResource(R.string.postfix_x_f, it),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                viewState.gridItemSizeMultiplier = value
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.CentreTileShape)
        )
    }

    // Translucent System Bars
    // Whether System Bars are rendered as translucent or Transparent.
    item(contentType = CONTENT_TYPE_PREF) {
        val translucentSystemBars by preference(Settings.TRANSPARENT_SYSTEM_BARS)
        SwitchPreference(
            checked = translucentSystemBars,
            text = textResource(R.string.pref_translucent_system_bars),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.TRANSPARENT_SYSTEM_BARS, should)
            },
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape)
        )
    }

    // Playback
    item(contentType = RS.CONTENT_TYPE_HEADER) {
        Header(
            textResource(R.string.playback),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.accent,
            contentPadding = RS.HeaderPadding
        )
    }

    // Use Inbuilt Audio FX
    // Whether to use inbuilt audio effects or inApp.
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(R.string.pref_use_inbuilt_audio_effects),
            checked = viewState.inAppAudioEffectsEnabled,
            onCheckedChange = { viewState.inAppAudioEffectsEnabled = it},
            icon = Icons.Outlined.Tune,
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.TopTileShape)
        )
    }
    // FAB player long press behaviour
    item(contentType = CONTENT_TYPE_PREF) {
        SwitchPreference(
            text = textResource(R.string.pref_fab_player_tap_behaviour),
            checked = !viewState.fabLongPressLaunchConsole,
            onCheckedChange = { viewState.fabLongPressLaunchConsole = !it},
            icon = Icons.Outlined.TouchApp,
            modifier = Modifier.background(AppTheme.colors.background(1.dp), RS.BottomTileShape)
        )
    }
}