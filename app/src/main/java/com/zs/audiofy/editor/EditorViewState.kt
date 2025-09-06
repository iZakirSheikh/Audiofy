/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.common.Route

private const val PARAM_SOURCE = "param_source"

object RouteEditor: Route {

    override val route: String = "$domain/{${PARAM_SOURCE}}"

    /** Navigates to files of folder identified by [source] providing args*/
    operator fun invoke(path: String): String = "$domain/${Uri.encode(path)}"
}

operator fun SavedStateHandle.get(route: RouteEditor) = get<String>(PARAM_SOURCE)

interface EditorViewState {
    val extraInfo: CharSequence?
    val artwork: ImageBitmap?


    var title: TextFieldValue
    var artist: TextFieldValue
    var album: TextFieldValue
    var composer: TextFieldValue
    var albumArtist: TextFieldValue
    var genre: TextFieldValue
    var year: TextFieldValue
    var trackNumber: TextFieldValue
    var diskNumber: TextFieldValue
    var totalDisks: TextFieldValue
    var lyrics: TextFieldValue
    var comment: TextFieldValue
    var copyright: TextFieldValue
    var url: TextFieldValue
    var originalArtist: TextFieldValue
    var publisher: TextFieldValue

    /**
     * Sets the artwork of the media item to the given URI, or removes the artwork if the URI is null.
     * This function modifies the APIC (Attached picture) frame in the ID3v2 tag, or deletes it if the URI is null.
     * @param new the URI of the new artwork image, or null to remove the artwork
     */
    fun setArtwork(new: Uri?)

    /**
     * Saves the media item to the persistent memory, either replacing the original file or creating a new one depending on the user's strategy.
     * This function requires the context to be of ComponentActivity type, otherwise it will throw an exception.
     * @param ctx the context of the ComponentActivity that calls this function
     * @throws IllegalArgumentException if the context is not of ComponentActivity type
     */
    fun save(ctx: Context)

    /**
     * Resets the tags of the media item to their original value, discarding any changes made by the user.
     * This function restores the ID3v2 tag to its initial state when the media item was created or loaded.
     */
    fun reset()
}