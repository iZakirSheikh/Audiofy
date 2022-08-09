package com.prime.player.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.common.asComposeState
import com.primex.core.Text
import com.primex.preferences.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@Immutable
data class Preference<out P>(
    val value: P,
    @JvmField
    val title: Text,
    val vector: ImageVector? = null,
    @JvmField
    val summery: Text? = null,
)


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: Preferences
) : ViewModel() {

    val darkUiMode =
        with(preferences) {
            preferences[GlobalKeys.NIGHT_MODE].map {
                Preference(
                    value = when (it) {
                        NightMode.YES -> true
                        else -> false
                    },
                    title = Text("Dark Mode"),
                    summery = Text("Click to change the app night/light mode."),
                    vector = Icons.Outlined.Lightbulb
                )
            }.asComposeState()
        }

    val font =
        with(preferences) {
            preferences[GlobalKeys.FONT_FAMILY].map {
                Preference(
                    vector = Icons.Default.TextFields,
                    title = Text("Font"),
                    summery = Text("Choose font to better reflect your desires."),
                    value = it
                )
            }.asComposeState()
        }

    val colorStatusBar =
        with(preferences) {
            preferences[GlobalKeys.COLOR_STATUS_BAR]
                .map {
                    Preference(
                        vector = null,
                        title = Text("Color Status Bar"),
                        summery = Text("Force color status bar."),
                        value = it
                    )
                }
                .asComposeState()
        }

    val hideStatusBar =
        with(preferences) {
            preferences[GlobalKeys.HIDE_STATUS_BAR]
                .map {
                    Preference(
                        value = it,
                        title = Text("Hide Status Bar"),
                        summery = Text("hide status bar for immersive view"),
                        vector = Icons.Outlined.HideImage
                    )
                }
                .asComposeState()
        }

    val forceAccent =
        with(preferences) {
            preferences[GlobalKeys.FORCE_COLORIZE]
                .map {
                    Preference(
                        value = it,
                        title = Text("Force Accent Color"),
                        summery = Text("Normally the app follows the rule of using 10% accent color. But if this setting is toggled it can make it use  more than 30%")
                    )
                }
                .asComposeState()
        }


    val fontScale =
        with(preferences) {
            preferences[GlobalKeys.FONT_SCALE]
                .map {
                    Preference(
                        value = it,
                        title = Text("Font Scale"),
                        summery = Text("Zoom in or out the text shown on the screen."),
                        vector = Icons.Outlined.ZoomIn
                    )
                }
                .asComposeState()
        }

    val showProgressInMini =
        with(preferences) {
            preferences[GlobalKeys.SHOW_MINI_PROGRESS_BAR]
                .map {
                    Preference(
                        value = it,
                        title = Text("MiniPlayer Progress Bar"),
                        summery = Text("Show/Hide progress bar in MiniPlayer."),
                        vector = Icons.Outlined.Report
                    )
                }
                .asComposeState()
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

context (Preferences, ViewModel)
        private fun <T> Flow<T>.asComposeState(): State<T> =
    asComposeState(
        obtain()
    )