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

package com.prime.media.impl

import android.app.Activity
import android.net.Uri
import com.prime.media.console.ConsoleViewState
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying2
import com.zs.core.playback.PlaybackController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ConsoleViewModel2(val controller: PlaybackController) : KoinViewModel(), ConsoleViewState {
    override fun clear() {
        TODO("Not yet implemented")
    }

    override var playbackSpeed: Float
        get() = TODO("Not yet implemented")
        set(value) {}
    override val visibility: Int
        get() = TODO("Not yet implemented")

    override fun emit(newVisibility: Int, delayed: Boolean) {
        TODO("Not yet implemented")
    }

    override fun skipToNext() {
        TODO("Not yet implemented")
    }

    override fun skipToPrev() {
        TODO("Not yet implemented")
    }

    override fun togglePlay() {
        TODO("Not yet implemented")
    }

    override fun seekTo(pct: Float) {
        TODO("Not yet implemented")
    }

    override fun seekBy(mills: Long) {
        TODO("Not yet implemented")
    }

    override fun sleepAt(mills: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaybackState(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getBufferedPct(): Float {
        TODO("Not yet implemented")
    }

    override val state: StateFlow<NowPlaying2?> = controller.state
    override val queue: Flow<List<MediaFile>?> = controller.queue

    override fun skipTo(key: Uri) {
        TODO("Not yet implemented")
    }

    override fun remove(key: Uri) {
        TODO("Not yet implemented")
    }

    override fun delete(key: Uri, resolver: Activity) {
        TODO("Not yet implemented")
    }

    override fun toggleLike(uri: Uri?) {
        TODO("Not yet implemented")
    }

    override fun cycleRepeatMode() {
        TODO("Not yet implemented")
    }

    override fun shuffle(enable: Boolean) {
        TODO("Not yet implemented")
    }
}