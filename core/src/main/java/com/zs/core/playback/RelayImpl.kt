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

import android.media.audiofx.Equalizer
import android.net.Uri
import kotlinx.coroutines.flow.Flow

class RelayImpl : Relay {

    override val state: Flow<NowPlaying>
        get() = TODO("Not yet implemented")
    override val queue: Flow<List<MediaFile>>
        get() = TODO("Not yet implemented")

    override suspend fun connect() {
        TODO("Not yet implemented")
    }

    override suspend fun set(values: List<MediaFile>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun add(
        values: List<MediaFile>,
        index: Int
    ): Int {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: Uri): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }

    override suspend fun play(playWhenReady: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun toggleShuffle(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun skipToNext() {
        TODO("Not yet implemented")
    }

    override suspend fun skipToPrev() {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(position: Int, mills: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(uri: Uri, mills: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun cycleRepeatMode(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun move(from: Int, to: Int): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setSleepTimeAt(mills: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun setEqualizer(eq: Equalizer?) {
        TODO("Not yet implemented")
    }

    override suspend fun getEqualizer(priority: Int): Equalizer {
        TODO("Not yet implemented")
    }
}