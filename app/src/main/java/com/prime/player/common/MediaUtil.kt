@file:Suppress("FunctionName")

package com.prime.player.common

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

private const val ALBUM_ART_URI: String = "content://media/external/audio/albumart"


object MediaUtil {
    /**
     * This Composes the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] from the provided Album [id]
     */
    @JvmStatic
    fun composeAlbumArtUri(id: Long): Uri =
        ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), id)

    /**
     * This Composes the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] with the provided [Audio] [id]
     */
    @JvmStatic
    fun composeAudioTrackUri(id: Long) =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
}



