package com.prime.media.old.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.TextFieldValue

private const val TAG = "TagEditor"

interface TagEditor {

    companion object {
        const val KEY_PRAM_DATA = "_data"
        const val route = "tag_editor/{${KEY_PRAM_DATA}}"
        fun direction(path: String) =
            "tag_editor/${Uri.encode(path)}"
    }

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

