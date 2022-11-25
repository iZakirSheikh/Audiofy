package com.prime.player.audio

import androidx.compose.ui.unit.dp
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.longPreferenceKey
import java.util.concurrent.TimeUnit

private const val TAG = "Tokens"

object Player {
    /**
     * A prefix char for private playlists.
     */
    const val PRIVATE_PLAYLIST_PREFIX = '_'

    /**
     * The name of the playlist contains the favourites.
     */
    val PLAYLIST_FAVOURITES = PRIVATE_PLAYLIST_PREFIX + "favourites"

    /**
     * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
     */
    val MINI_PLAYER_HEIGHT = 68.dp

    val SHOW_MINI_PROGRESS_BAR = booleanPreferenceKey(
        TAG + "_show_mini_progress_bar", false
    )


    private val defaultMinTrackLimit = TimeUnit.MINUTES.toMillis(1)

    /**
     * The length/duration of the track in mills considered above which to include
     */
    val EXCLUDE_TRACK_DURATION = longPreferenceKey(
        TAG + "_min_duration_limit_of_track", defaultMinTrackLimit
    )


    val MAX_RECENT_PLAYLIST_SIZE = intPreferenceKey(
        TAG + "_max_recent_size", defaultValue = 20
    )
}

