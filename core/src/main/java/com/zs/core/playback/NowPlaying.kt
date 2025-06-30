/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

import android.net.Uri

/**
 * Represents the current state of the [Playback]
 */
class NowPlaying(
    val title: String,
    val subtitle: String,
    val artwork: Uri?,
    val speed: Float,
    val duration: Long,
    val position: Long,
    val favourite: Boolean,
    val playing: Boolean,
    val mimeType: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NowPlaying

        if (speed != other.speed) return false
        if (duration != other.duration) return false
        if (position != other.position) return false
        if (favourite != other.favourite) return false
        if (playing != other.playing) return false
        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (artwork != other.artwork) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = speed.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + favourite.hashCode()
        result = 31 * result + playing.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + (artwork?.hashCode() ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "NowPlaying(title='$title', subtitle='$subtitle', artwork=$artwork, speed=$speed, duration=$duration, position=$position, favourite=$favourite, playing=$playing, mimeType=$mimeType)"
    }
}