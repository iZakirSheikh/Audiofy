/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 30-09-2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core.playback

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "NowPlaying"

// extra
private const val EXTRA_TITLE = "com.prime.player.extra.MEDIA_TITLE"
private val EXTRA_SUBTITLE = "com.prime.player.extra.MEDIA_SUBTITLE"
private val EXTRA_ARTWORK = "com.prime.player.extra.MEDIA_ARTWORK"
private val EXTRA_DURATION = "com.prime.player.extra.MEDIA_DURATION"
private val EXTRA_POSITION = "com.prime.player.extra.MEDIA_PROGRESS"
private val EXTRA_STATE = "com.prime.player.extra.MEDIA_STATE"
private val EXTRAS_PLAY_WHEN_READY = "com.prime.player.extra.PLAY_WHEN_READY"
private val EXTRA_WHEN = "com.prime.player.extra.WHEN"
private val EXTRA_SPEED = "com.prime.player.extra.SPEED"

/**
 * @property timeStamp - The time in mills when this notification was generated.
 */
@JvmInline
value class NowPlaying(private val value: Intent) {
    companion object {
        // action
        @JvmStatic
        val ACTION_TOGGLE_PLAY = "com.prime.player.action.TOGGLE_PLAY"

        @JvmStatic
        val ACTION_SEEK_TO = "com.prime.player.action.SEEK_TO"

        @JvmStatic
        val ACTION_NEXT = "com.prime.player.action.NEXT"

        @JvmStatic
        val ACTION_PREVIOUS = "com.prime.player.action.PREVIOUS"

        @JvmStatic
        val EXTRA_SEEK_PCT = "com.prime.player.extra.SEEK_TO"

        @JvmStatic
        val EXTRA_REPEAT_MODE = "com.prime.player.action.REPEAT_MODE"

        /**
         * Constructs the widget update intent from a [Player].
         */
        internal fun from(ctx: Context, player: Player): Intent {
            return Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                `package` = "com.prime.player"
                val ids = AppWidgetManager.getInstance(ctx.applicationContext)
                    .getAppWidgetIds(ComponentName(ctx.applicationContext, "com.zs.widget.AppWidget"))

                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                val mediaItem = player.currentMediaItem ?: return@apply
                putExtra(EXTRA_TITLE, mediaItem.mediaMetadata.title.toString())
                putExtra(EXTRA_SUBTITLE, mediaItem.mediaMetadata.subtitle.toString())
                val uri = mediaItem.mediaMetadata.artworkUri
                putExtra(EXTRA_ARTWORK, uri)
                putExtra(EXTRA_DURATION, player.duration)
                putExtra(EXTRA_POSITION, player.currentPosition)
                putExtra(EXTRA_STATE, player.playbackState)
                putExtra(EXTRAS_PLAY_WHEN_READY, player.playWhenReady)
                putExtra(EXTRA_WHEN, System.currentTimeMillis())
                putExtra(EXTRA_SPEED, player.playbackParameters.speed)
                putExtra(EXTRA_REPEAT_MODE, player.repeatMode)
            }
        }

        /**
         * Represents the empty [NowPlaying] instance.
         */
        val EMPTY = NowPlaying(Intent())

        /**
         *
         */
        inline fun trySend(ctx: Context, action: String? = null, args: Intent.() -> Unit = {}) {
            try {
                val intent = Intent(action, null, ctx, Playback::class.java).apply(args)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startService(intent) else ctx.startService(intent)
            } catch (i: Exception) {
                Log.d("NowPlaying", "trySend: ${i.message}")
            }
        }
    }

    val timeStamp get() =
        value.getLongExtra(EXTRA_WHEN, -1L)
    val title
        get() = value.getStringExtra(EXTRA_TITLE)
    val subtitle
        get() = value.getStringExtra(EXTRA_SUBTITLE)
    val position
        get() = value.getLongExtra(EXTRA_POSITION, C.TIME_UNSET)
    val duration
        get() = value.getLongExtra(EXTRA_DURATION, C.TIME_UNSET)
    val artwork
        get() = value.getParcelableExtra<Uri>(EXTRA_ARTWORK)
    val state
        get() = value.getIntExtra(EXTRA_STATE, Player.STATE_IDLE)
    val playWhenReady
        get() = value.getBooleanExtra(EXTRAS_PLAY_WHEN_READY, false)
    val speed
        get() = value.getFloatExtra(EXTRA_SPEED, 1f)
    val repeatMode
        get() = value.getIntExtra(EXTRA_REPEAT_MODE, Player.REPEAT_MODE_ALL)

    val playing get() = playWhenReady && state != Player.STATE_ENDED && state != Player.STATE_IDLE
}