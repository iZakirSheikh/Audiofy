/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 17-11-2024.
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

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.zs.core.await

private const val TAG = "PlaybackControllerImpl"

// TODO: currently a quickfix requirement. find better alternative.
private fun Context.browser(listener: MediaBrowser.Listener) =
    MediaBrowser
        .Builder(this, SessionToken(this, ComponentName(this, Playback::class.java)))
        .setListener(listener)
        .buildAsync()

internal class PlaybackControllerImpl(
    val context: Context
) : PlaybackController, MediaBrowser.Listener {

    // TODO: A quickfix, find better alternative of doing this.

    // The fBrowser variable is lazily initialized with context.browser(this).
    // Whenever fBrowser is accessed, the getter checks if the current value is cancelled.
    // If it is cancelled, it re-initializes fBrowser with a new context.browser(this).
    // Otherwise, it retains the current value.
    // The goal is to ensure that fBrowser always holds a valid browser context,
    // reinitializing it if the current one has been cancelled.
    private var fBrowser = context.browser(this)
        get() {
            field = if (field.isCancelled) context.browser(this) else field
            return field
        }


    override suspend fun setMediaFiles(values: List<MediaFile>): Int {
        val browser = fBrowser.await()
        // make sure the items are distinct.
        val list = values.distinctBy { it.mediaUri }
        // set the media items; this will automatically clear the old ones.
        browser.setMediaItems(list.map { it.value })
        // return how many have been added to the list.
        return list.size
    }

    override suspend fun play(playWhenReady: Boolean) {
        val browser = fBrowser.await()
        browser.playWhenReady = playWhenReady
        browser.play()
    }

    override suspend fun clear() {
        val browser = fBrowser.await()
        browser.clearMediaItems()
    }

    override suspend fun connect(){
        val browser = fBrowser.await()
        /*TODO: Do nothing*/
    }
}