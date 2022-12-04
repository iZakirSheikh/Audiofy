package com.prime.player.core


import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.prime.player.Audiofy
import com.prime.player.common.FileUtils
import com.prime.player.common.getAlbumArt
import com.prime.player.common.parent
import com.primex.core.runCatching
import com.primex.preferences.Preferences
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.value
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject

private const val TAG = "SyncWorker"

private val AUDIO_PROJECTION
    get() = arrayOf(
        MediaStore.MediaColumns._ID, //0
        MediaStore.MediaColumns.TITLE, // 1
        MediaStore.Audio.AudioColumns.ARTIST, // 2
        MediaStore.Audio.AudioColumns.ALBUM, // 3
        MediaStore.Audio.AudioColumns.ALBUM_ID, // 4
        MediaStore.Audio.AudioColumns.DATE_ADDED,  //5
        MediaStore.Audio.AudioColumns.COMPOSER, // , // 6
        MediaStore.Audio.AudioColumns.YEAR, // 7
        MediaStore.Audio.AudioColumns.DATA, // 8
        MediaStore.Audio.AudioColumns.DURATION, // 9
        MediaStore.Audio.AudioColumns.MIME_TYPE, // 10
        MediaStore.Audio.AudioColumns.TRACK, // 11
        MediaStore.MediaColumns.SIZE, //12
        MediaStore.Audio.AudioColumns.DATE_MODIFIED, // 14
    )


private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

private val MAX_ID_PROJECTION = arrayOf("MAX(${MediaStore.MediaColumns.DATE_MODIFIED})")
private val ContentResolver.lastModified
    get() = query(MEDIA_URI, MAX_ID_PROJECTION, null, null, null)
        ?.use {
            it.moveToFirst()
            it.getLong(0)
        }

// maybe use GROUP_CONCAT for keys below version pie.
private val KEYS_PROJECTION = arrayOf(MediaStore.MediaColumns._ID)
private val ContentResolver.keys: String?
    get() = query(MEDIA_URI, KEYS_PROJECTION, null, null, null)
        ?.use { cursor ->
            val buffer = StringBuilder()
            while (cursor.moveToNext()) {
                if (!cursor.isFirst) buffer.append(",")
                val element = cursor.getLong(0)
                buffer.append("'$element'")
            }
            buffer.toString()
        }


fun Audio(cursor: Cursor, retriever: MediaMetadataRetriever): Audio {
    val path = cursor.getString(8)
    val genre = runCatching(TAG) {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
    }
    return with(cursor) {
        Audio(
            id = getLong(0),
            title = getString(1) ?: MediaStore.UNKNOWN_STRING,
            albumId = getLong(4),
            path = path,
            parent = FileUtils.parent(path),
            album = getString(3) ?: MediaStore.UNKNOWN_STRING,
            artist = getString(2) ?: MediaStore.UNKNOWN_STRING,
            composer = getString(6) ?: MediaStore.UNKNOWN_STRING,
            mimeType = getString(10),
            track = getInt(11),
            dateAdded = getLong(5) * 1000,
            dateModified = getLong(13) * 1000,
            duration = getInt(9),
            size = getLong(12),
            year = getInt(7),
            genre = genre ?: MediaStore.UNKNOWN_STRING
        )
    }
}

private const val IMMEDIATE_UPDATE_WORKER = "immediate_update_worker"
private const val TRIGGER_UPDATE_WORKER = "trigger_update_worker"

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params = params) {

    companion object {

        /**
         * Runs Immediate Works.
         */
        fun run(context: Context) {
            // run on app start up for first time
            val workManager = WorkManager.getInstance(context.applicationContext)
            val work = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().build())
                .build()
            workManager.enqueueUniqueWork(
                IMMEDIATE_UPDATE_WORKER,
                ExistingWorkPolicy.REPLACE,
                work
            )
        }

        /**
         * Schedule works on content Uri change.
         */
        @RequiresApi(Build.VERSION_CODES.N)
        fun schedule(context: Context) {
            // run on app start up for first time
            val workManager = WorkManager.getInstance(context.applicationContext)
            val work = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true)
                        // .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(
                TRIGGER_UPDATE_WORKER,
                ExistingWorkPolicy.REPLACE,
                work
            )
        }


        /**
         * The name of the poster file. Must be unique
         */
        private const val AUDIO_POSTER_UNIQUE_TAG = TAG + "_audio_poster"

        /**
         * The time duration of the audio poster.
         */
        val KEY_AUDIO_POSTER_MILLS = longPreferenceKey(AUDIO_POSTER_UNIQUE_TAG)

        /**
         * The Max number of artworks used in the poster.
         */
        const val MAX_POSTER_ALBUMS = 30

        /**
         * The size of the individual artwork image used in the poster.
         */
        const val POSTER_ARTWORK_SIZE = 256

        /**
         * The duration in mills the individual artwork is played by the lib.
         */
        const val BITMAP_DURATION = 2_500L //ms
    }

    @Inject
    lateinit var audioDb: Audios

    @Inject
    lateinit var preferences: Preferences


    /**
     * Compute and save the art work.
     */
    private suspend fun poster() {
        val albums = audioDb.getRecentAlbums(MAX_POSTER_ALBUMS)
        // don't proceed if the list is empty.
        if (albums.isEmpty())
            return

        // collect bitmaps of albums.
        val bitmaps = ArrayList<Bitmap>()
        albums.forEach { album ->
            // compute uri using the album id
            val uri = Audiofy.toAlbumArtUri(album.id)
            //fetch the artwork
            val artwork = context.getAlbumArt(uri = uri, POSTER_ARTWORK_SIZE)?.toBitmap()
            //add to list
            if (artwork != null) bitmaps.add(artwork)
        }

        // only proceed if the bitmaps is not empty.
        if (bitmaps.isEmpty())
            return
        // The width and height of the resultant image.
        val width = bitmaps.size * POSTER_ARTWORK_SIZE
        val height = POSTER_ARTWORK_SIZE

        // paste bitmaps on the Canvas
        val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background)

        var x = 0f
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, x, 0f, null)
            x += POSTER_ARTWORK_SIZE
        }

        val key = KEY_AUDIO_POSTER_MILLS

        //save using key name and increent the version number.
        context.openFileOutput(key.name, Context.MODE_PRIVATE).use { fos ->
            background.compress(Bitmap.CompressFormat.JPEG, 60, fos)
            //increment by 1/ms if the new is equal to old.
            // so the observer will be notified.
            val oldMills = preferences.value(key)
            var new = bitmaps.size * BITMAP_DURATION
            if (new == oldMills)
                new += 1
            preferences[key] = new
        }
    }

    override suspend fun doWork(): Result {
        val result =
            kotlin.runCatching {
                val resolver = context.contentResolver
                val db = audioDb

                // To sync local cache with source.
                // 3 cases needs to be dealt with:
                // case 1: if the file/files in source has been updated.
                // case 2: if the file/files in source has been deleted.
                // case 3: if the files have been added to source.

                // case 2 can't be handled using lastModified, because lastModified returns the max value in table.
                // suppose some file before the max has been deleted; the lastModified will
                // still return the same value and hence can't determined weather a file has been deleted.

                // so handle the case for delete using teh below steps.
                // if keys is null either some error has occurred or every single track from MediaStore
                // has been deleted by the user.
                val keys = resolver.keys ?: ""
                // delete all the files of dest which are not
                // part of the source keys.
                db.delete(keys)

                // The last modified of source
                val sLastModified = resolver.lastModified ?: 0L
                // The last modified of dest.
                // because it is in mills
                val dLastModified = (db.lastModified() ?: 0L) / 1000

                // may be use cursor to get the genre.
                val retriever = MediaMetadataRetriever()

                when {
                    // after running the case of deletion; I guess this case should be ok to run.
                    sLastModified == dLastModified -> return@runCatching Result.success()

                    // source is greater.
                    sLastModified > dLastModified -> {
                        // means the source contains some files
                        // that are either updated or newly added
                        // case for deleting has already been dealt with
                        // Get all the files that are above the date of dLastModified.
                        // get all available in content resolver
                        val projection = AUDIO_PROJECTION
                        // get all those changed or newly added files.
                        // update or insert them to local images.
                        val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} > $dLastModified"

                        resolver.query(MEDIA_URI, projection, selection, null, null)
                            ?.use { cursor ->
                                while (cursor.moveToNext()) {
                                    val id = cursor.getLong(0)
                                    // insert not exist ones only
                                    val audio = Audio(cursor, retriever)
                                    // add if not exists else update
                                    if (!db.exists(id)) db.insert(audio) else db.update(audio)
                                }
                            }
                        // maybe reconstruct poster
                        poster()

                        Result.success()
                    }
                    // this will never occur.
                    else -> Result.success()
                }
            }
        return result.getOrNull() ?: Result.failure()
    }
}