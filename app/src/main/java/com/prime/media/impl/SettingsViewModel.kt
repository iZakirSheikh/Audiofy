package com.prime.media.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.media.core.util.asComposeState
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

context (Preferences, ViewModel)
private fun <T> Flow<T>.asComposeState(): State<T> = asComposeState(runBlocking { first() })

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

    override fun <S, O> set(key: Key<S, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }
}