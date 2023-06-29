package com.prime.media.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.media.Audiofy
import com.prime.media.core.compose.asComposeState
import com.prime.media.settings.Preference
import com.prime.media.settings.Settings
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: Preferences
) : ViewModel(), Settings {

    override val darkUiMode by with(preferences) {
        preferences[Settings.NIGHT_MODE].map {
            Preference(
                value = it,
                title = Text("Dark Mode"),
                summery = Text("Click to change the app night/light mode."),
                vector = Icons.Outlined.Lightbulb
            )
        }.asComposeState()
    }

    override val colorStatusBar by with(preferences) {
        preferences[Settings.COLOR_STATUS_BAR].map {
            Preference(
                vector = null,
                title = Text("Color Status Bar"),
                summery = Text("Force color status bar."),
                value = it
            )
        }.asComposeState()
    }

    override val hideStatusBar by with(preferences) {
        preferences[Settings.HIDE_STATUS_BAR].map {
            Preference(
                value = it,
                title = Text("Hide Status Bar"),
                summery = Text("hide status bar for immersive view"),
                vector = Icons.Outlined.HideImage
            )
        }.asComposeState()
    }

    override val forceAccent by with(preferences) {
        preferences[Settings.FORCE_COLORIZE].map {
            Preference(
                value = it,
                title = Text("Force Accent Color"),
                summery = Text("Normally the app follows the rule of using 10% accent color. But if this setting is toggled it can make it use  more than 30%")
            )
        }.asComposeState()
    }


    override val fontScale by with(preferences) {
        preferences[Settings.FONT_SCALE].map {
            Preference(
                value = it,
                title = Text("Font Scale"),
                summery = Text("Zoom in or out the text shown on the screen."),
                vector = Icons.Outlined.ZoomIn
            )
        }.asComposeState()
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }
}

context (Preferences, ViewModel)
        private fun <T> Flow<T>.asComposeState(): State<T> = asComposeState(runBlocking { first() })