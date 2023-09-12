@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.impl

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mpatric.mp3agic.Mp3File
import com.prime.media.R
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.findAudio
import com.prime.media.core.db.uri
import com.prime.media.core.util.getActivityResult
import com.prime.media.editor.TagEditor
import com.primex.core.activity
import com.primex.core.getText2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

private const val TAG = "TagEditorViewModel"


/**
 * Gets or sets the title of the MP3 file.
 * If the ID3v1 tag is present, it uses the title from ID3v1;
 * otherwise, it uses the title from ID3v2.
 */
private var Mp3File.title
    get() = id3v1Tag?.title ?: id3v2Tag?.title
    set(value) {
        id3v1Tag?.title = value
        id3v2Tag?.title = value
    }

/**
 * Gets or sets the artist of the MP3 file.
 * If the ID3v1 tag is present, it uses the artist from ID3v1;
 * otherwise, it uses the artist from ID3v2.
 */
private var Mp3File.artist
    get() = id3v1Tag?.artist ?: id3v2Tag?.artist
    set(value) {
        id3v1Tag?.artist = value
        id3v2Tag?.artist = value
    }

/**
 * Gets or sets the album of the MP3 file.
 * If the ID3v1 tag is present, it uses the album from ID3v1;
 * otherwise, it uses the album from ID3v2.
 */
private var Mp3File.album
    get() = id3v1Tag?.album ?: id3v2Tag?.album
    set(value) {
        id3v1Tag?.album = value
        id3v2Tag?.album = value
    }

/**
 * Gets or sets the composer of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.composer
    get() = id3v2Tag?.composer
    set(value) {
        id3v2Tag?.composer = value
    }

/**
 * Gets or sets the album artist of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.albumArtist
    get() = id3v2Tag?.albumArtist
    set(value) {
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
        //id3v1Tag?.genreDescription = value
        id3v2Tag?.genreDescription = value
    }

/**
 * Gets or sets the lyrics of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.lyrics
    get() = id3v2Tag?.lyrics
    set(value) {
        id3v2Tag?.lyrics = value
    }

/**
 * Gets or sets the comment of the MP3 file.
 * If the ID3v1 tag is present, it uses the comment from ID3v1;
 * otherwise, it uses the comment from ID3v2.
 */
private var Mp3File.comment
    get() = id3v1Tag?.comment ?: id3v2Tag.comment
    set(value) {
        id3v1Tag?.comment = value
        id3v2Tag?.comment = value
    }

/**
 * Gets or sets the copyright information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.copyright
    get() = id3v2Tag?.copyright
    set(value) {
        id3v2Tag?.copyright = value
    }

/**
 * Gets or sets the URL of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.url
    get() = id3v2Tag?.url
    set(value) {
        id3v2Tag?.url = value
    }

/**
 * Gets or sets the publisher information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.publisher
    get() = id3v2Tag?.publisher
    set(value) {
        id3v2Tag?.publisher = value
    }

/**
 * Gets or sets the original artist information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.originalArtist
    get() = id3v2Tag?.originalArtist
    set(value) {
        id3v2Tag?.originalArtist = value
    }

/**
 * Gets or sets the encoder information of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.encoder
    get() = id3v2Tag?.encoder
    set(value) {
        id3v2Tag?.encoder = value
    }

/**
 * Gets or sets the artwork (album cover) of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.artwork
    get() = id3v2Tag?.albumImage
    set(value) {
        id3v2Tag?.setAlbumImage(value, id3v2Tag.albumImageMimeType)
    }

/**
 * Gets or sets the MIME type of the album artwork of the MP3 file from the ID3v2 tag.
 */
private var Mp3File.albumArtMimeType
    get() = id3v2Tag?.albumImageMimeType
    set(value) {
        id3v2Tag?.setAlbumImage(id3v2Tag?.albumImage, value)
    }

/**
 * Gets or sets the year of the MP3 file. If available, it first checks the ID3v1 tag,
 * then the ID3v2 tag, and defaults to 0 if neither has a valid year.
 */
private var Mp3File.year
    get() = id3v1Tag?.year?.toIntOrNull() ?: id3v2Tag?.year?.toIntOrNull() ?: 0
    set(value) {
        id3v1Tag?.year = "$value"
        id3v2Tag?.year = "$value"
    }

/**
 * Gets or sets the track number of the MP3 file. If available, it first checks the ID3v1 tag,
 * then the ID3v2 tag, and defaults to 0 if neither has a valid track number.
 */
private var Mp3File.trackNumber
    get() = id3v1Tag?.track?.toIntOrNull() ?: id3v2Tag?.track?.toIntOrNull() ?: 0
    set(value) {
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
        // Get the TPOS frame
        val partOfSet = id3v2Tag?.partOfSet ?: "0/0"
        val parts = partOfSet.split("/")
        id3v2Tag?.partOfSet = "$value" + "/" + (parts.getOrNull(0) ?: "0")
    }

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val resources: Resources,
    private val resolver: ContentResolver,
    private val snackbar: Channel
) : ViewModel(), TagEditor {

    val path = handle.get<String>(TagEditor.KEY_PRAM_DATA)

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
        val na = resources.getText(R.string.not_available_abbv)
        // initialize the extra info.
        extraInfo = resources.getText2(
            R.string.tag_editor_scr_extra_immutable_info_ssss,
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
            file = Mp3File(path ?: throw IllegalArgumentException("$path must not be null"))
            val result = runCatching { initialize() }
            // show error message if there happened an error during initializing phase
            if (result.isFailure)
                snackbar.show(R.string.msg_unknown_error)
        }
    }

    override fun reset() {
        viewModelScope.launch {
            val action =
                snackbar.show(R.string.msg_tag_editor_reset_warning, action = R.string.reset)
            if (action == Channel.Result.Dismissed)
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
                val inputStream = resolver.openInputStream(new)
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
                snackbar.show(R.string.msg_unknown_error)
        }
    }

    /**
     * Applies the specified tags to the MP3 file. Remember to call save() to persist the changes to storage.
     */
    fun Mp3File.apply() {
        // apply tags from here to file.
        val file = this
        file.title = this@TagEditorViewModel.title.text
        file.artist = this@TagEditorViewModel.artist.text
        file.album = this@TagEditorViewModel.album.text
        file.composer = this@TagEditorViewModel.composer.text
        file.albumArtist = this@TagEditorViewModel.albumArtist.text
        file.genre = this@TagEditorViewModel.genre.text
        file.year = this@TagEditorViewModel.year.text.toIntOrNull() ?: 0
        file.trackNumber = this@TagEditorViewModel.trackNumber.text.toIntOrNull() ?: 0
        file.diskNumber = this@TagEditorViewModel.diskNumber.text.toIntOrNull() ?: 0
        file.disks = this@TagEditorViewModel.totalDisks.text.toIntOrNull() ?: 0
        file.lyrics = this@TagEditorViewModel.lyrics.text
        file.comment = this@TagEditorViewModel.comment.text
        file.copyright = this@TagEditorViewModel.copyright.text
        file.originalArtist = this@TagEditorViewModel.originalArtist.text
        file.publisher = this@TagEditorViewModel.publisher.text
        file.url = this@TagEditorViewModel.url.text
    }

    override fun save(ctx: Context) {
        viewModelScope.launch {
            val action = snackbar.show(
                R.string.msg_tag_editor_scr_overwrite_waring,
                action = R.string.overwrite
            )
            if (action == Channel.Result.Dismissed)
                return@launch
            val result = runCatching {
                val cacheDir = ctx.cacheDir.path
                // Apply tags to file
                file.apply()
                // save file in cache first; because Mp3File doesn't support overwriting file
                val tmpFilePath = "$cacheDir/tmp.edit"
                // save file as temp.
                file.save(tmpFilePath)
                // now obtain the usi of the audio file
                // TODO: rethink! How this should be obtained.
                val uri = resolver.findAudio(path ?: "")?.uri
                    ?: throw IllegalStateException("Mp3 file not found.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val request = MediaStore.createWriteRequest(resolver, listOf(uri)).let {
                        IntentSenderRequest.Builder(it).build()
                    }
                    // This must be Component activity
                    val activity = ctx.activity as ComponentActivity
                    val result = activity.getActivityResult(
                        ActivityResultContracts.StartIntentSenderForResult(),
                        request
                    )
                    if (result.resultCode != Activity.RESULT_OK) {
                        snackbar.show(R.string.msg_tag_editor_overwrite_permission_revoked)
                        return@launch
                    }
                }
                // in case below R proceed normally
                // because we already have write permission.
                val tmpFile = File(tmpFilePath)
                tmpFile.inputStream().use { ios ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        // Create a buffer of 4 KB
                        val buffer = ByteArray(4096)
                        // Read the data from the file into the buffer
                        var bytesRead = ios.read(buffer)
                        // Loop until the end of the file is reached
                        while (bytesRead != -1) {
                            // Write the buffer to the output stream
                            outputStream.write(buffer, 0, bytesRead)
                            // Read the next chunk of data into the buffer
                            bytesRead = ios.read(buffer)
                        }
                    }
                }
                tmpFile.delete()
                snackbar.show(R.string.msg_tag_editor_file_update_success)
            }
            if (result.isFailure)
                snackbar.show(resources.getText2(R.string.error, result.exceptionOrNull()?.message ?: ""))
        }
    }
}