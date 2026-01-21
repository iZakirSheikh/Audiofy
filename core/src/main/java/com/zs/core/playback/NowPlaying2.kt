/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 21 of Jan 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 21 of Jan 2026
 *
 */

package com.zs.core.playback

import android.net.Uri
import androidx.media3.common.Player

/**
 * Represents the current state of the [Playback]
 *
 * @property neighbours An integer indicating the availability of neighboring media items:
 *  - `0`: No neighboring items are available.
 *  - `1`: Only the next item is available.
 *  - `-1`: Only the previous item is available.
 *  - `2`: Both next and previous items are available.
 */
class NowPlaying2(
    val title: String?,
    val subtitle: String?,
    val artwork: Uri? = null,
    val speed: Float = 1.0f,
    val shuffle: Boolean = false,
    val duration: Long = PlaybackController.TIME_UNSET,
    val position: Long = PlaybackController.TIME_UNSET,
    val favourite: Boolean = false,
    val playWhenReady: Boolean = false,
    val mimeType: String? = null,
    val state: Int = Player.STATE_IDLE,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val error: String? = null,
    val videoSize: VideoSize = VideoSize(),
    val data: Uri? = null,
    private val neighbours: Int = 0,
    val sleepAt: Long = PlaybackController.TIME_UNSET,
) {
    val  isNextAvailable get() = neighbours == 1 || neighbours == 2
    val  isPrevAvailable get() = neighbours == -1 || neighbours == 2
    // This time when this was generated.
    val timeStamp = System.currentTimeMillis()
    val isVideo = mimeType?.startsWith("video") == true

    val playing = playWhenReady && state != Player.STATE_ENDED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NowPlaying2

        if (speed != other.speed) return false
        if (shuffle != other.shuffle) return false
        if (duration != other.duration) return false
        if (position != other.position) return false
        if (favourite != other.favourite) return false
        if (playWhenReady != other.playWhenReady) return false
        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (artwork != other.artwork) return false
        if (mimeType != other.mimeType) return false
        if (timeStamp != other.timeStamp) return false
        if (state != other.state) return false
        if (repeatMode != other.repeatMode) return false
        if (error != other.error) return false
        if (videoSize != other.videoSize) return false
        if (data != other.data) return false
        if (neighbours != other.neighbours) return false
        if (sleepAt != other.sleepAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = speed.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + favourite.hashCode()
        result = 31 * result + playWhenReady.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + shuffle.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + (artwork?.hashCode() ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + state
        result = 31 * result + repeatMode
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + videoSize.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + neighbours.hashCode()
        result = 31 * result + sleepAt.hashCode()

        return result
    }

    override fun toString(): String {
        return "NowPlaying(" +
                "title=$title, " +
                "subtitle=$subtitle," +
                " artwork=$artwork," +
                " speed=$speed," +
                " shuffle=$shuffle," +
                " duration=$duration," +
                " position=$position," +
                " favourite=$favourite," +
                " playWhenReady=$playWhenReady," +
                " mimeType=$mimeType," +
                " state=$state," +
                " repeatMode=$repeatMode," +
                " error=$error," +
                " videoSize=$videoSize," +
                " timeStamp=$timeStamp," +
                " isVideo=$isVideo," +
                " playing=$playing" +
                " data=$data" +
                " neighbours=$neighbours" +
                " sleepAt=$sleepAt)"
    }
}