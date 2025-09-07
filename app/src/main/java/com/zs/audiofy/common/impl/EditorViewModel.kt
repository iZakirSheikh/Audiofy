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

package com.zs.audiofy.common.impl


import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mpatric.mp3agic.ID3v1Genres
import com.mpatric.mp3agic.Mp3File
import com.zs.audiofy.R
import com.zs.audiofy.editor.EditorViewState
import com.zs.audiofy.editor.RouteEditor
import com.zs.audiofy.editor.get
import com.zs.compose.foundation.findActivity
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.snackbar.SnackbarResult
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private const val TAG = "TagEditorViewModel"


/**
 * Gets or sets the title of the MP3 file.
 * If the ID3v1 tag is present, it uses the title from ID3v1;
 * otherwise, it uses the title from ID3v2.
 */
private var Mp3File.title
    get() = id3v2Tag?.title ?: id3v1Tag?.title
    set(value) {
        if (title == value) return
        id3v1Tag?.title = value
        id3v2Tag?.title = value
    }

/**
 * Gets or sets the artist of the MP3 file.
 * If the ID3v1 tag is present, it uses the artist from ID3v1;
 * otherwise, it uses the artist from ID3v2.
 */
private var Mp3File.artist
    get() = id3v2Tag?.artist ?: id3v1Tag?.artist
    set(value) {
        if (artist == value) return
        id3v1Tag?.artist = value
        id3v2Tag?.artist = value
    }

/**
 * Gets or sets the album of the MP3 file.
 * If the ID3v1 tag is present, it uses the album from ID3v1;
 * otherwise, it uses the album from ID3v2.
 */
private var Mp3File.album
    get() = id3v2Tag?.album ?: id3v1Tag?.album
    set(value) {
        if (album == value) return
        id3v1Tag?.album = value
        id3v2Tag?.album = value
    }

/**
 * Gets or sets the composer of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.composer
    get() = id3v2Tag?.composer
    set(value) {
        if (composer == value) return
        id3v2Tag?.composer = value
    }

/**
 * Gets or sets the album artist of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.albumArtist
    get() = id3v2Tag?.albumArtist
    set(value) {
        if (albumArtist == value) return
        id3v2Tag?.albumArtist = value
    }

/**
 * Gets or sets the genre of the MP3 file.
 * If the ID3v1 tag is present, it uses the genre description from ID3v1;
 * otherwise, it uses the genre description from ID3v2.
 */
private var Mp3File.genre
    get() = id3v2Tag?.genreDescription ?: id3v1Tag?.genreDescription
    set(value) {
        // old value is new value or genre is not from pre-defiend values just return
        if (genre == value || ID3v1Genres.matchGenreDescription(value) < 0) return
        //id3v1Tag?.genreDescription = value
        id3v2Tag?.genreDescription = value
    }

/**
 * Gets or sets the lyrics of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.lyrics
    get() = id3v2Tag?.lyrics
    set(value) {
        if (lyrics == value) return
        id3v2Tag?.lyrics = value
    }

/**
 * Gets or sets the comment of the MP3 file.
 * If the ID3v1 tag is present, it uses the comment from ID3v1;
 * otherwise, it uses the comment from ID3v2.
 */
private var Mp3File.comment
    get() = id3v2Tag.comment ?: id3v1Tag?.comment
    set(value) {
        if (comment == value) return
        id3v1Tag?.comment = value
        id3v2Tag?.comment = value
    }

/**
 * Gets or sets the copyright information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.copyright
    get() = id3v2Tag?.copyright
    set(value) {
        if (copyright == value) return
        id3v2Tag?.copyright = value
    }

/**
 * Gets or sets the URL of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.url
    get() = id3v2Tag?.url
    set(value) {
        if (url == value) return
        id3v2Tag?.url = value
    }

/**
 * Gets or sets the publisher information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.publisher
    get() = id3v2Tag?.publisher
    set(value) {
        if (publisher == value) return
        id3v2Tag?.publisher = value
    }

/**
 * Gets or sets the original artist information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.originalArtist
    get() = id3v2Tag?.originalArtist
    set(value) {
        if (originalArtist == value) return
        id3v2Tag?.originalArtist = value
    }

/**
 * Gets or sets the encoder information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.encoder
    get() = id3v2Tag?.encoder
    set(value) {
        if (encoder == value) return
        id3v2Tag?.encoder = value
    }

private val ALBUM_ART_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("png")

/**
 * Gets or sets the artwork (album cover) of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.artwork
    get() = id3v2Tag?.albumImage
    set(value) {
        // same
        if (artwork.contentEquals(value)) return
        id3v2Tag?.setAlbumImage(value, ALBUM_ART_MIME_TYPE)
    }

/**
 * Gets or sets the MIME type of the album artwork of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.albumArtMimeType
    get() = id3v2Tag?.albumImageMimeType
    set(value) {
        if (albumArtMimeType == value) return
        id3v2Tag?.setAlbumImage(id3v2Tag?.albumImage, value)
    }

/**
 * Gets or sets the year of the MP3 file. If available, it first checks the ID3v1 tag,
 * then the ID3v2 tag, and defaults to 0 if neither has a valid year.
 */
private var Mp3File.year
    get() = id3v2Tag?.year?.toIntOrNull() ?: id3v1Tag?.year?.toIntOrNull() ?: 0
    set(value) {
        if (year == value) return
        id3v1Tag?.year = "$value"
        id3v2Tag?.year = "$value"
    }

/**
 * Gets or sets the track number of the MP3 file. If available, it first checks the ID3v1 tag,
 * then the ID3v2 tag, and defaults to 0 if neither has a valid track number.
 */
private var Mp3File.trackNumber
    get() = id3v2Tag?.track?.toIntOrNull() ?: id3v1Tag?.track?.toIntOrNull() ?: 0
    set(value) {
        if (trackNumber == value) return
        id3v1Tag?.track = "$value"
        id3v2Tag?.track = "$value"
    }

/**
 * Gets or sets the number of disks for the MP3 file.
 * Note: This property is not currently implemented.
 */
private var Mp3File.disks: Int
    get() {
        // Get the TPOS frame
        val partOfSet = id3v2Tag?.partOfSet ?: return 0
        // Split the string by "/"
        val parts = partOfSet.split("/")
        if (parts.size >= 2) {
            // Return the second part as an integer
            return parts[1].toIntOrNull() ?: 0
        }
        return 0
    }
    set(value) {
        if (disks == value) return
        // Get the TPOS frame
        val partOfSet = id3v2Tag?.partOfSet ?: "0/0"
        val parts = partOfSet.split("/")
        id3v2Tag?.partOfSet = (parts.getOrNull(0) ?: "0") + "/" + "$value"
    }

/**
 * Gets or sets the disk number of the MP3 file.
 * Note: This property is not currently implemented.
 */
private var Mp3File.diskNumber: Int
    get() {
        // Get the TPOS frame
        val partOfSet = id3v2Tag?.partOfSet ?: return 0
        // Split the string by "/"
        val parts = partOfSet.split("/")
        if (parts.isEmpty()) {
            // Return the 1st part as an integer
            return 0
        }
        return parts[0].toIntOrNull() ?: 0
    }
    set(value) {
        if (diskNumber == value) return
        // Get the TPOS frame
        val partOfSet = id3v2Tag?.partOfSet ?: "0/0"
        val parts = partOfSet.split("/")
        id3v2Tag?.partOfSet = "$value" + "/" + (parts.getOrNull(0) ?: "0")
    }

/**
 * @see registerActivityResultLauncher
 */
private suspend fun <I, O> ComponentActivity.awaitActivityResult(
    contract: ActivityResultContract<I, O>,
    request: I,
): O = suspendCoroutine { continuation ->
    val key = UUID.randomUUID().toString()
    var launcher: ActivityResultLauncher<I>? = null
    val callback = ActivityResultCallback<O> { output ->
        continuation.resume(output)
        launcher?.unregister()
    }
    launcher = activityResultRegistry.register(key, contract, callback)
    launcher.launch(request)
}

class EditorViewModel(handle: SavedStateHandle) : KoinViewModel(), EditorViewState {
    val path = handle[RouteEditor]

    /**
     * The Mp3 File
     */
    lateinit var file: Mp3File

    override var extraInfo: CharSequence? by mutableStateOf(null)

    // first set all values to empty string
    override var title: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var artist: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var album: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var composer: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var albumArtist: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var genre: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var year: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var trackNumber: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var diskNumber: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var totalDisks: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var lyrics: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var comment: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var copyright: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var url: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var originalArtist: TextFieldValue by mutableStateOf(TextFieldValue(""))
    override var publisher: TextFieldValue by mutableStateOf(TextFieldValue(""))
    private var _artwork: ByteArray? = null
    override var artwork: ImageBitmap? by mutableStateOf(null)

    /**
     * Resets the value from a [File], discarding the previous value.
     * This function initializes or reinitializes the internal state based on the contents of the provided [File].
     */
    private fun initialize() {
        title = TextFieldValue(file.title ?: "")
        artist = TextFieldValue(file.artist ?: "")
        album = TextFieldValue(file.album ?: "")
        composer = TextFieldValue(file.composer ?: "")
        albumArtist = TextFieldValue(file.albumArtist ?: "")
        genre = TextFieldValue(file.genre ?: "")
        year = TextFieldValue("${file.year}")
        //year = TextFieldValue(file.)
        trackNumber = TextFieldValue("${file.trackNumber}")
        diskNumber = TextFieldValue("${file.diskNumber}")
        totalDisks = TextFieldValue("${file.disks}")
        lyrics = TextFieldValue(file.lyrics ?: "")
        comment = TextFieldValue(file.comment ?: "")
        copyright = TextFieldValue(file.copyright ?: "")
        url = TextFieldValue(file.url ?: "")
        originalArtist = TextFieldValue(file.originalArtist ?: "")
        publisher = TextFieldValue(file.publisher ?: "")
        val na = getText(R.string.abbr_not_available)
        // initialize the extra info.
        extraInfo = getText(
            R.string.scr_tag_editor_extra_info_ssss,
            file.channelMode ?: na,
            file.bitrate,
            DateUtils.formatElapsedTime(file.lengthInSeconds),
            file.sampleRate
        )
        _artwork = file.artwork
        val array = _artwork
        if (array != null)
            artwork = BitmapFactory.decodeByteArray(array, 0, array.size).asImageBitmap()
    }

    init {
        viewModelScope.launch {
            // requires new name
            //file = Mp3File(path ?: throw IllegalArgumentException("$path must not be null"))
            val result = runCatching {
                file = Mp3File(path ?: error("path must not be null"))
                initialize()
            }
            // show error message if there happened an error during initializing phase
            if (result.isFailure)
                showPlatformToast("${result.exceptionOrNull()?.message}")
        }
    }

    override fun reset() {
        viewModelScope.launch {
            val action =
                showSnackbar("Resetting the file will discard all the changes", action = "Proceed")
            if (action == SnackbarResult.Dismissed)
                return@launch
            // else reset/reinitialize
            initialize()
        }
    }

    override fun setArtwork(new: Uri?) {
        if (new == null) {
            // clear artwork
            _artwork = null
            artwork = null
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                // Create an input stream from the URI
                val inputStream = context.contentResolver.openInputStream(new)
                // Create a Bitmap from the input stream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                artwork = bitmap.asImageBitmap()
                // Create a ByteArrayOutputStream object
                val stream = ByteArrayOutputStream()
                // Compress the Bitmap into a PNG format
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
                // Get the ByteArray from the stream
                _artwork = stream.toByteArray()
            }
            if (result.isFailure)
                showPlatformToast("${result.exceptionOrNull()?.message}")
        }
    }

    /**
     * Applies the specified tags to the MP3 file. Remember to call save() to persist the changes to storage.
     */
    fun Mp3File.apply() {
        // apply tags from here to file.
        val file = this
        file.title = this@EditorViewModel.title.text
        file.artist = this@EditorViewModel.artist.text
        file.album = this@EditorViewModel.album.text
        file.composer = this@EditorViewModel.composer.text
        file.albumArtist = this@EditorViewModel.albumArtist.text
        file.genre = this@EditorViewModel.genre.text
        file.year = this@EditorViewModel.year.text.toIntOrNull() ?: 0
        file.trackNumber = this@EditorViewModel.trackNumber.text.toIntOrNull() ?: 0
        file.diskNumber = this@EditorViewModel.diskNumber.text.toIntOrNull() ?: 0
        file.disks = this@EditorViewModel.totalDisks.text.toIntOrNull() ?: 0
        file.lyrics = this@EditorViewModel.lyrics.text
        file.comment = this@EditorViewModel.comment.text
        file.copyright = this@EditorViewModel.copyright.text
        file.originalArtist = this@EditorViewModel.originalArtist.text
        file.publisher = this@EditorViewModel.publisher.text
        file.url = this@EditorViewModel.url.text
        file.artwork = _artwork
    }

    override fun save(ctx: Context) {
        runCatching {
            // Show a confirmation Snackbar to the user before proceeding with the save operation
            val action = showSnackbar(
                "Warning: Existing file will be replaced.",
                action = "Confirm",
                icon = Icons.Outlined.Save,
                duration = SnackbarDuration.Indefinite
            )
            // If the user dismisses the Snackbar, abort the save operation
            if (action == SnackbarResult.Dismissed)
                return@runCatching
            // Apply the edited tags to the Mp3File object
            file.apply()
            // Define the path for a temporary file in the cache directory.
            // Mp3File library doesn't support overwriting files directly, so a temp file is needed.
            val tmpFilePath = "${ctx.cacheDir.path}/tmp.edit"
            // Save the modified Mp3File to the temporary file
            file.save(tmpFilePath)
            // Get the ContentResolver to interact with the MediaStore
            val resolver = context.contentResolver
            // Query the MediaStore to find the URI of the original audio file
            val uri = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.DATA}=?",
                arrayOf(path),
                null
            ).use { // Use 'use' block to ensure the cursor is closed automatically
                it?.moveToFirst()
                // Get the ID of the audio file from the cursor
                val id =
                    it?.getLong(0) ?: throw IllegalStateException("File not found in media library.")
                // Construct the content URI for the audio file using its ID
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            }
            // Ask for write permission if above R
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android R and above, use MediaStore.createWriteRequest for scoped storage
                val request = MediaStore.createWriteRequest(resolver, listOf(uri)).let {
                    IntentSenderRequest.Builder(it).build()
                }
                // The context must be a ComponentActivity to launch the intent sender
                val activity = ctx.findActivity() as ComponentActivity
                // Launch the intent sender and wait for the result
                val result = activity.awaitActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult(),
                    request
                )
                // If the result is not OK, throw an error
                if (result.resultCode != Activity.RESULT_OK) {
                    error("Failed to save file.")
                }
            }
            // For versions below Android R, or after successful write request on R+, proceed with direct file stream copy.
            // This assumes write permission is already granted.
            val tmpFile = File(tmpFilePath)
            tmpFile.inputStream().use { inputStream -> // Open an input stream from the temporary file
                resolver.openOutputStream(uri)?.use { outputStream -> // Open an output stream to the original file's URI
                    inputStream.copyTo(outputStream) // Copy the contents of the temp file to the original file
                }
            }
            tmpFile.delete()
            showPlatformToast("Done! Your changes have been saved.")
        }
    }
}