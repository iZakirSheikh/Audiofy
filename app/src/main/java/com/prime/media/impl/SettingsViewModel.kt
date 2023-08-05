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
                title = Text("App Theme"),
                summery = Text("Select the app theme: dark, light, or follow the system settings."),
                vector = Icons.Outlined.Lightbulb
            )
        }.asComposeState()
    }


    override val colorStatusBar by with(preferences) {
        preferences[Settings.COLOR_STATUS_BAR].map {
            Preference(
                vector = null,
                title = Text("Color Status Bar"),
                summery = Text("Make the status bar colorful."),
                value = it
            )
        }.asComposeState()
    }

    override val hideStatusBar by with(preferences) {
        preferences[Settings.HIDE_STATUS_BAR].map {
            Preference(
                value = it,
                title = Text("Hide Status Bar"),
                summery = Text("Enjoy a more immersive view without the status bar."),
                vector = Icons.Outlined.HideImage
            )
        }.asComposeState()
    }

    override val forceAccent by with(preferences) {
        preferences[Settings.FORCE_COLORIZE].map {
            Preference(
                value = it,
                title = Text("Force Accent Color"),
                summery = Text("Increase or decrease the accent color usage in the app.")
            )
        }.asComposeState()
    }

    override val minTrackLength: Preference<Int> by with(preferences){
        preferences[Settings.MIN_TRACK_LENGTH_SECS].map {
            Preference(
                title = Text("Minimum track length"),
                summery = Text("Only tracks longer than this duration (in seconds) will be added to the library."),
                value = it
            )
        }.asComposeState()
    }

    override val recentPlaylistLimit: Preference<Int> by with(preferences){
        preferences[Settings.RECENT_PLAYLIST_LIMIT].map {
            Preference(
                title = Text("Recent playlist size"),
                summery = Text("Set the maximum number of tracks that should be stored in the playlist history. "),
                value = it
            )
        }.asComposeState()
    }

    override val fetchArtworkFromMS: Preference<Boolean> by with(preferences){
        preferences[Settings.USE_LEGACY_ARTWORK_METHOD].map {
            Preference(
                title = Text("Fetch artwork from MediaStore"),
                summery = Text("Faster artwork loading time, but can show incorrect artwork"),
                value = it
            )
        }.asComposeState()
    }

    override val enableTrashCan: Preference<Boolean> by with(preferences){
        preferences[Settings.TRASH_CAN_ENABLED].map {
            Preference(
                title = Text("Enable trash can"),
                summery = Text("When enabled, deleted tracks will be moved to a trash can folder instead of being permanently deleted. "),
                value = it
            )
        }.asComposeState()
    }
    override val excludedFiles: Preference<Set<String>?> by with(preferences){
        preferences[Settings.BLACKLISTED_FILES].map {
            Preference(
                title = Text("Blacklist"),
                summery = Text("Tracks from the blacklist folders will be hidden from your library."),
                value = it
            )
        }.asComposeState()
    }
    override val gaplessPlayback: Preference<Boolean> by with(preferences){
        preferences[Settings.TRASH_CAN_ENABLED].map {
            Preference(
                title = Text("Enable gapless playback"),
                summery = Text("When enabled, there will be no gaps or pauses between tracks during playback."),
                value = it
            )
        }.asComposeState()
    }
    override val crossfadeTime: Preference<Int> by with(preferences){
        preferences[Settings.RECENT_PLAYLIST_LIMIT].map {
            Preference(
                title = Text("Crossfade time"),
                summery = Text("The duration (in seconds) of cross fading effect between tracks during playback."),
                value = it
            )
        }.asComposeState()
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }
}