package com.prime.media.impl

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.core.util.PathUtils
import com.prime.media.core.util.asComposeState
import com.prime.media.settings.ColorizationStrategy
import com.prime.media.settings.Preference
import com.prime.media.settings.Settings
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.value
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
                title = Text(R.string.pref_app_theme),
                summery = Text(R.string.pref_app_theme_summery),
                vector = Icons.Outlined.Lightbulb
            )
        }.asComposeState()
    }


    override val translucentSystemBars by with(preferences) {
        preferences[Settings.TRANSLUCENT_SYSTEM_BARS].map {
            Preference(
                vector = Icons.Outlined.Fullscreen,
                title = Text(R.string.pref_translucent_system_bars),
                summery = Text(R.string.pref_translucent_system_bars_summery),
                value = it
            )
        }.asComposeState()
    }

    override val immersiveView by with(preferences) {
        preferences[Settings.IMMERSIVE_VIEW].map {
            Preference(
                value = it,
                title = Text(R.string.pref_immersive_view),
                summery = Text(R.string.pref_immersive_view_summery),
                vector = Icons.Outlined.VisibilityOff
            )
        }.asComposeState()
    }



    override val minTrackLength: Preference<Int> by with(preferences) {
        preferences[Settings.MIN_TRACK_LENGTH_SECS].map {
            Preference(
                title = Text(R.string.pref_minimum_track_length),
                summery = Text(R.string.pref_minimum_track_length_summery),
                value = it
            )
        }.asComposeState()
    }

    override val recentPlaylistLimit: Preference<Int> by with(preferences) {
        preferences[Settings.RECENT_PLAYLIST_LIMIT].map {
            Preference(
                title = Text(R.string.pref_recent_playlist_size),
                summery = Text(R.string.pref_recent_playlist_size_summery),
                value = it
            )
        }.asComposeState()
    }

    override val fetchArtworkFromMS: Preference<Boolean> by with(preferences) {
        preferences[Settings.USE_LEGACY_ARTWORK_METHOD].map {
            Preference(
                title = Text(R.string.pref_fetch_artwork_from_media_store),
                summery = Text(R.string.pref_fetch_artwork_from_media_store_summery),
                value = it
            )
        }.asComposeState()
    }

    override val gridItemSizeMultiplier: Preference<Float> by with(preferences) {
        preferences[Settings.GRID_ITEM_SIZE_MULTIPLIER].map {
            Preference(
                title = Text(R.string.pref_grid_item_size_multiplier),
                summery = Text(R.string.pref_grid_item_size_multiplier_summery),
                value = it,
                vector = Icons.Outlined.GridView
            )
        }.asComposeState()
    }
    override val colorizationStrategy: Preference<ColorizationStrategy> by with(preferences) {
        preferences[Settings.COLORIZATION_STRATEGY].map {
            Preference(
                title = Text(R.string.pref_colorization_strategy),
                summery = Text(R.string.pref_colorization_strategy_summery),
                value = it
            )
        }.asComposeState()
    }

    override val enableTrashCan: Preference<Boolean> by with(preferences) {
        preferences[Settings.TRASH_CAN_ENABLED].map {
            Preference(
                title = Text(R.string.pref_enable_trash_can),
                summery = Text(R.string.pref_enable_trash_can_summery, isHtml = true),
                value = it
            )
        }.asComposeState()
    }
    override val excludedFiles: Preference<Set<String>?> by with(preferences) {
        preferences[Settings.BLACKLISTED_FILES].map {
            Preference(
                title = Text(R.string.pref_blacklist),
                summery = Text(R.string.pref_blacklist_summery),
                value = it
            )
        }.asComposeState()
    }
    override val gaplessPlayback: Preference<Boolean> by with(preferences) {
        preferences[Settings.TRASH_CAN_ENABLED].map {
            Preference(
                title = Text(R.string.pref_enable_gapless_playback),
                summery = Text(R.string.pref_enable_gapless_playback_summery),
                value = it
            )
        }.asComposeState()
    }
    override val crossfadeTime: Preference<Int> by with(preferences) {
        preferences[Settings.RECENT_PLAYLIST_LIMIT].map {
            Preference(
                title = Text(R.string.pref_crossfade_time),
                summery = Text(R.string.pref_crossfade_time_summery),
                value = it
            )
        }.asComposeState()
    }
    override val closePlaybackWhenTaskRemoved by with(preferences) {
        preferences[Settings.CLOSE_WHEN_TASK_REMOVED].map {
            Preference(
                title = Text(R.string.pref_stop_playback_when_task_removed),
                summery = Text(R.string.pref_stop_playback_when_task_removed_summery),
                value = it
            )
        }.asComposeState()
    }
    override val useInbuiltAudioFx by with(preferences) {
        preferences[Settings.USE_IN_BUILT_AUDIO_FX].map {
            Preference(
                title = Text(R.string.pref_use_inbuilt_audio_effects),
                summery = Text(R.string.pref_use_inbuilt_audio_effects_summery),
                value = it
            )
        }.asComposeState()
    }

    override val fontScale by with(preferences) {
        preferences[Settings.FONT_SCALE].map {
            Preference(
                title = Text(R.string.pref_font_scale),
                summery = Text(R.string.pref_font_scale_summery),
                value = it,
                vector = Icons.Outlined.TextFields
            )
        }.asComposeState()
    }
    override fun <S, O> set(key: Key<S, O>, value: O) {
        viewModelScope.launch {
            preferences[key] = value
        }
    }

    override val values: Set<String>? by derivedStateOf {
        excludedFiles.value
    }

    override fun unblock(path: String, context: Context) {
        viewModelScope.launch {
            val blacklist = preferences.value(Settings.BLACKLISTED_FILES)?.toMutableSet() ?: return@launch
            val res = blacklist.remove(path)
            val name = PathUtils.name(path)
            if (res)
                Toast.makeText(context, context.getString(R.string.msg_blacklist_item_remove_success_s, name), Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            preferences[Settings.BLACKLISTED_FILES] = blacklist
        }
    }
}

suspend fun Preferences.block(vararg path: String): Int {
    val preferences = this
    val list = preferences.value(Settings.BLACKLISTED_FILES)
    if (path.isEmpty()) return  0
    // it will automatically remove duplicates.
    if (list == null) {
        val items = path.toSet()
        preferences[Settings.BLACKLISTED_FILES] = path.toSet()
        return items.size
    }
    val items = list + path
    preferences[Settings.BLACKLISTED_FILES] = items
    return items.size
}