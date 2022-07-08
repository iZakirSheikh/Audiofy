package com.prime.player.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primex.preferences.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Preference<out P>(
    val value: P,
    val title: String,
    val vector: ImageVector? = null,
    val summery: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {

    val darkUiMode =
        with(preferences) {
            preferences[GlobalKeys.NIGHT_MODE].map {
                Preference(
                    value = when (it) {
                        NightMode.YES -> true
                        else -> false
                    },
                    title = "Dark Mode",
                    summery = "Click to change the app night/light mode.",
                    vector = Icons.Outlined.Lightbulb
                )
            }.toComposeState()
        }

    val font =
        with(preferences) {
            preferences[GlobalKeys.FONT_FAMILY].map {
                Preference(
                    vector = Icons.Default.TextFields,
                    title = "Font",
                    summery = "Choose font to better reflect your desires.",
                    value = it
                )
            }.toComposeState()
        }

    val colorStatusBar =
        with(preferences) {
            preferences[GlobalKeys.COLOR_STATUS_BAR]
                .map {
                    Preference(
                        vector = null,
                        title = "Color Status Bar",
                        summery = "Force color status bar.",
                        value = it
                    )
                }
                .toComposeState()
        }

    val hideStatusBar =
        with(preferences) {
            preferences[GlobalKeys.HIDE_STATUS_BAR]
                .map {
                    Preference(
                        value = it,
                        title = "Hide Status Bar",
                        summery = "hide status bar for immersive view",
                        vector = Icons.Outlined.HideImage
                    )
                }
                .toComposeState()
        }

    val forceAccent =
        with(preferences) {
            preferences[GlobalKeys.FORCE_COLORIZE]
                .map {
                    Preference(
                        value = it,
                        title = "Force Accent Color",
                        summery = "Normally the app follows the rule of using 10% accent color. But if this setting is toggled it can make it use  more than 30%"
                    )
                }
                .toComposeState()
        }


    val fontScale =
        with(preferences) {
            preferences[GlobalKeys.FONT_SCALE]
                .map {
                    Preference(
                        value = it,
                        title = "Font Scale",
                        summery = "Zoom in or out the text shown on the screen.",
                        vector = Icons.Outlined.ZoomIn
                    )
                }
                .toComposeState()
        }

    val showProgressInMini =
        with(preferences) {
            preferences[GlobalKeys.SHOW_MINI_PROGRESS_BAR]
                .map {
                    Preference(
                        value = it,
                        title = "MiniPlayer Progress Bar",
                        summery = "Show/Hide progress bar in MiniPlayer.",
                        vector = Icons.Outlined.Report
                    )
                }
                .toComposeState()
        }


    fun <T> set(key: Key<T>, value: T) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }

    fun <T> set(key: Key1<T>, value: T) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }

    fun <T, O> set(key: Key2<T, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }

    fun <T, O> set(key: Key3<T, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }
}


context (Preferences, ViewModel) private fun <T> Flow<T>.toComposeState(): State<T> {
    val state = mutableStateOf(obtain())
    onEach { state.value = it }
        .launchIn(viewModelScope)
    return state
}
