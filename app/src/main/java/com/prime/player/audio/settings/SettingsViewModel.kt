package com.prime.player.audio.settings

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.R
import com.prime.player.extended.Quad
import com.prime.player.preferences.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


typealias Preference<T> = Quad<ImageVector?, String, String?, T>

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(context: Application) : ViewModel() {

    private val preferences = Preferences.get(context = context)

    val nightModeStrategy: StateFlow<Preference<NightMode>> =
        preferences.getDefaultNightMode().transformLatest { mode ->
            val mapped = Preference(
                Icons.Default.ModeNight,
                context.getString(R.string.app_theme),
                null,
                forth = mode
            )
            emit(mapped)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            Preference(
                Icons.Default.ModeNight,
                context.getString(R.string.app_theme),
                null,
                forth = with(preferences) { getDefaultNightMode().collectBlocking() }
            )
        )

    val primaryColor: StateFlow<Preference<Color>> =
        preferences.getPrimaryColor().transformLatest { primaryColor ->
            val transformed = Preference(
                Icons.Default.ColorLens,
                context.getString(R.string.color_primary),
                context.getString(R.string.color_primary_desc),
                primaryColor
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            Preference(
                Icons.Default.ColorLens,
                context.getString(R.string.color_primary),
                context.getString(R.string.color_primary_desc),
                with(preferences) { getPrimaryColor().collectBlocking() }
            )
        )

    val secondaryColor: StateFlow<Preference<Color>> =
        preferences.getSecondaryColor().transformLatest { secondaryColor ->
            val transformed = Preference(
                Icons.Default.ColorLens,
                context.getString(R.string.color_secondary),
                context.getString(R.string.color_secondary_desc),
                secondaryColor
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            Preference(
                Icons.Default.ColorLens,
                context.getString(R.string.color_secondary),
                context.getString(R.string.color_secondary_desc),
                with(preferences) { getSecondaryColor().collectBlocking() }
            )
        )

    val requiresAccentThoroughly: StateFlow<Preference<Boolean>> =
        preferences.requiresAccentThoroughly().transformLatest { should ->
            val transformed = Preference(
                null,
                context.getString(R.string.force_use_accent_color),
                context.getString(R.string.force_use_accent_color_summery),
                should
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value = with(preferences) { requiresAccentThoroughly().collectBlocking() }
                Preference(
                    null,
                    context.getString(R.string.force_use_accent_color),
                    context.getString(R.string.force_use_accent_color_summery),
                    value
                )
            }
        )

    val requiresColoringStatusBar: StateFlow<Preference<Boolean>> =
        preferences.requiresColoringStatusBar().transformLatest { should ->
            val transformed = Preference(
                null,
                context.getString(R.string.color_status_bar),
                "Force color status bar with accent",
                should
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val should =
                    with(preferences) { requiresColoringStatusBar().collectBlocking() }
                Preference(
                    null,
                    context.getString(R.string.color_status_bar),
                    "Force color status bar with accent",
                    should
                )
            }
        )

    val font: StateFlow<Preference<Font>> =
        preferences.getDefaultFont().transformLatest { value ->
            val transformed = Preference(
                Icons.Default.TextFields,
                "Font",
                "Choose font to better reflect your desires.",
                value
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value = with(preferences) { getDefaultFont().collectBlocking() }
                Preference(
                    Icons.Default.TextFields,
                    "Font",
                    "Choose font to better reflect your desires.",
                    value
                )
            }
        )

    val showProgressInMini: StateFlow<Preference<Boolean>> =
        preferences.requiresProgressBarInMiniPlayer().transformLatest { should ->
            val transformed = Preference(
                null,
                context.getString(R.string.show_progress_in_mini_player),
                context.getString(R.string.show_progress_in_mini_player_desc),
                should
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value =
                    with(preferences) { requiresProgressBarInMiniPlayer().collectBlocking() }
                Preference(
                    Icons.Default.TextFields,
                    context.getString(R.string.show_progress_in_mini_player),
                    context.getString(R.string.show_progress_in_mini_player_desc),
                    value
                )
            }
        )

    val hideStatusBar: StateFlow<Preference<Boolean>> =
        preferences.hideStatusBar().transformLatest { should ->
            val transformed = Preference(
                null,
                context.getString(R.string.status_bar),
                "Hide status bar for immersive view",
                should
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value = with(preferences) { hideStatusBar().collectBlocking() }
                Preference(
                    Icons.Default.TextFields,
                    context.getString(R.string.status_bar),
                    "Hide status bar for immersive view",
                    value
                )
            }
        )

    val recentSize: StateFlow<Preference<Int>> =
        preferences.recentSize().transformLatest { value ->
            val transformed = Preference(
                Icons.Default.FormatListNumbered,
                "Recent list",
                "Enter a number between 20 and 100",
                value
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value = with(preferences) { recentSize().collectBlocking() }
                Preference(
                    Icons.Default.FormatListNumbered,
                    "Recent list",
                    "Enter a number between 20 and 100",
                    value
                )
            }
        )

    val showVisualizer: StateFlow<Preference<Boolean>> =
        preferences.showVisualizer().transformLatest { should ->
            val transformed = Preference(
                null,
                "Audio Visualizer",
                "Toggle audio visualizer. This requires the permission to use Microphone.",
                should
            )
            emit(transformed)
        }.stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            kotlin.run {
                val value = with(preferences) { showVisualizer().collectBlocking() }
                Preference(
                    null,
                    "Audio Visualizer",
                    "Toggle audio visualizer. This requires the permission to use Microphone.",
                    value
                )
            }
        )

    fun setDefaultNightMode(mode: NightMode) {
        viewModelScope.launch {
            preferences.setDefaultNightMode(mode = mode)
        }
    }

    fun setShowVisualizer(should: Boolean) {
        viewModelScope.launch {
            preferences.setShowVisualizer(should)
        }
    }

    fun setDefaultFont(mode: Font) {
        viewModelScope.launch {
            preferences.setDefaultFont(font = mode)
        }
    }

    fun setPrimaryColor(color: Color) {
        viewModelScope.launch {
            preferences.setPrimaryColor(color)
        }
    }

    fun setSecondaryColor(color: Color) {
        viewModelScope.launch {
            preferences.setSecondaryColor(color)
        }
    }

    fun useAccentThoroughly(should: Boolean) {
        viewModelScope.launch {
            preferences.useAccentThoroughly(should)
            if (should)
                colorStatusBar(true)
        }
    }

    fun colorStatusBar(should: Boolean) {
        viewModelScope.launch {
            preferences.colorStatusBar(should)
        }
    }

    fun setRecentSize(newSize: Int) {
        viewModelScope.launch {
            preferences.setRecentSize(newSize)
        }
    }

    fun showProgressInMiniPlayer(should: Boolean) {
        viewModelScope.launch {
            preferences.showProgressInMiniPlayer(should)
        }
    }

    fun setHideStatusBar(hide: Boolean) {
        viewModelScope.launch {
            preferences.setHideStatusBar(hide)
        }
    }
}