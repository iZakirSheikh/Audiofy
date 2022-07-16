package com.prime.player.core


import android.content.Context
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.prime.player.common.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private const val AUDIO_COLUMN_ID = "audio_id"

// data classes.
@Entity(tableName = "tbl_audios")
data class Audio(
    @PrimaryKey
    @ColumnInfo(name = AUDIO_COLUMN_ID) @JvmField val id: Long,
    @JvmField val title: String,
    @ColumnInfo(name = "album_id") @JvmField val albumId: Long,
    @JvmField val path: String,
    @JvmField @ColumnInfo(name = "parent_path") val parent: String,
    @JvmField val album: String,
    @JvmField val artist: String,
    @JvmField val composer: String,
    @JvmField val genre: String,
    @ColumnInfo(name = "mime_type") @JvmField val mimeType: String,
    @JvmField val track: Int,
    @ColumnInfo(name = "date_added") @JvmField val dateAdded: Long,
    @ColumnInfo(name = "date_modified") @JvmField val dateModified: Long,
    @JvmField val duration: Int,
    @ColumnInfo(name = "file_size") @JvmField val size: Long,
    @JvmField val year: Int,
) {

    @DatabaseView(
        viewName = "vw_audio_info",
        value = "SELECT tbl_audios.*, COUNT(*) AS cardinality, MAX(date_modified) AS bucket_date_modified, " +
                "SUM(file_size) AS info_size, SUM(duration) AS info_duration FROM tbl_audios ORDER BY date_modified DESC",
    )
    data class Info(
        @Embedded @JvmField val value: Audio,
        @JvmField val cardinality: Int,
        @ColumnInfo(name = "bucket_date_modified") @JvmField val dateModified: Long,
        @ColumnInfo(name = "info_size") @JvmField val size: Int,
        @ColumnInfo(name = "info_duration") @JvmField val duration: Int
    )

    @DatabaseView(
        value = "SELECT tbl_audios.*, parent_path AS bucket_path, COUNT(*) AS cardinality, " +
                "MAX(date_modified) AS bucket_date_modified, SUM(file_size) AS bucket_size FROM tbl_audios " +
                "GROUP BY parent_path ORDER BY date_modified DESC",
        viewName = "vw_audio_bucket"
    )
    data class Bucket(
        @Embedded @JvmField val value: Audio,
        @JvmField @ColumnInfo(name = "bucket_path") val path: String,
        @JvmField val cardinality: Int,
        @JvmField @ColumnInfo(name = "bucket_size") val size: Long, // size in bytes
        @JvmField @ColumnInfo(name = "bucket_date_modified") val dateModified: Long,
    )

    @DatabaseView(
        value = "SELECT tbl_audios.*, artist AS artist_name, COUNT(*) AS tracks, COUNT(DISTINCT album) AS albums, " +
                "SUM(file_size) AS artist_size, SUM(duration) AS artist_duration FROM tbl_audios " +
                "GROUP BY artist ORDER BY date_modified DESC",
        viewName = "vw_audio_artist"
    )
    data class Artist(
        @Embedded val value: Audio,
        @JvmField @ColumnInfo(name = "artist_name") val name: String,
        @JvmField val tracks: Int,
        @JvmField val albums: Int,
        @JvmField @ColumnInfo(name = "artist_size") val size: Long,
        @JvmField @ColumnInfo(name = "artist_duration") val duration: Int,
    )

    @DatabaseView(
        viewName = "vw_audio_album",
        value = "SELECT tbl_audios.*, album AS bucket_title, COUNT(*) AS tracks, COUNT(DISTINCT album) AS albums, " +
                "SUM(file_size) AS album_size, SUM(duration) AS album_duration, MAX(year) AS last_year, MIN(year) AS first_year, album_id AS id " +
                "FROM tbl_audios GROUP BY album ORDER BY date_modified DESC"
    )
    data class Album(
        @JvmField val id: Long,
        @Embedded val value: Audio,
        @JvmField @ColumnInfo(name = "bucket_title") val title: String,
        @JvmField @ColumnInfo(name = "first_year") val firstYear: Int,
        @JvmField @ColumnInfo(name = "last_year") val lastYear: Int,
        @JvmField @ColumnInfo(name = "album_size") val size: Long,
        @JvmField @ColumnInfo(name = "album_duration") val duration: Int,
        @JvmField val tracks: Int,
    )

    @DatabaseView(
        value = "SELECT tbl_audios.*, genre AS genre_name, COUNT(*) AS tracks, SUM(file_size) AS genre_size, " +
                "SUM(duration) AS genre_duration FROM tbl_audios " +
                "GROUP BY genre ORDER BY date_modified DESC",
        viewName = "vw_audio_genre"
    )
    data class Genre(
        @Embedded val value: Audio,
        @JvmField @ColumnInfo(name = "genre_name") val name: String,
        @JvmField val tracks: Int,
        @JvmField @ColumnInfo(name = "genre_size") val size: Long,
        @JvmField @ColumnInfo(name = "genre_duration") val duration: Int,
    )
}


val Audio.Bucket.name get() = FileUtils.name(path)

/**
 * The member table of playlist.
 */
private const val TABLE_PLAYLIST_MEMBER = "tbl_playlist_members"

private const val PLAYLIST_COLUMN_ID = "playlist_id"
private const val MEMBER_COLUMN_ORDER = "play_order"
private const val MEMBER_FILE_ID = "file_id"

/**
 * @param tag Unique among modules like audio player, video player, radio etc.
 */
@Entity(tableName = "tbl_playlists")
data class Playlist(
    @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = PLAYLIST_COLUMN_ID) val id: Long = 0,
    @JvmField val name: String,
    @ColumnInfo(defaultValue = "") val desc: String,
    @JvmField val tag: String,
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long,
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long,
) {
    @Entity(
        tableName = TABLE_PLAYLIST_MEMBER,
        primaryKeys = [PLAYLIST_COLUMN_ID, MEMBER_FILE_ID],
        foreignKeys = [
            ForeignKey(
                entity = Audio::class,
                parentColumns = [AUDIO_COLUMN_ID],
                childColumns = [MEMBER_FILE_ID],
                onDelete = CASCADE
            ),
            ForeignKey(
                entity = Playlist::class,
                parentColumns = [PLAYLIST_COLUMN_ID],
                childColumns = [PLAYLIST_COLUMN_ID],
                onDelete = CASCADE
            )
        ],
        indices = [
            Index(
                value = [PLAYLIST_COLUMN_ID, MEMBER_FILE_ID],
                unique = false
            )
        ]
    )
    data class Member(
        @JvmField @ColumnInfo(name = PLAYLIST_COLUMN_ID) val playlistID: Long,
        @JvmField @ColumnInfo(name = MEMBER_FILE_ID) val id: String,
        @JvmField @ColumnInfo(name = MEMBER_COLUMN_ORDER) val order: Long
    )
}


@Database(
    entities = [Audio::class, Playlist::class, Playlist.Member::class],
    version = 1,
    exportSchema = false,
    views = [Audio.Bucket::class, Audio.Artist::class, Audio.Album::class, Audio.Genre::class, Audio.Info::class]
)
abstract class AppDatabase : RoomDatabase() {

    abstract val audios: Audios
    abstract val playlists: Playlists

    companion object {
        private const val DB_NAME = "localdb"

        /**
         * Create triggers in order to manage [MEMBER_COLUMN_PLAY_ORDER]
         */
        private const val TRIGGER = "trigger"

        //language=SQL
        private const val TRIGGER_BEFORE_INSERT =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_insert BEFORE INSERT ON $TABLE_PLAYLIST_MEMBER " +
                    "BEGIN UPDATE $TABLE_PLAYLIST_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER + 1 " +
                    "WHERE new.${PLAYLIST_COLUMN_ID} == $PLAYLIST_COLUMN_ID AND $MEMBER_COLUMN_ORDER >= new.$MEMBER_COLUMN_ORDER;" +
                    "END;"

        //language=SQL
        private const val TRIGGER_AFTER_DELETE =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_delete AFTER DELETE ON $TABLE_PLAYLIST_MEMBER " +
                    "BEGIN UPDATE $TABLE_PLAYLIST_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER - 1 " +
                    "WHERE old.${PLAYLIST_COLUMN_ID} == $PLAYLIST_COLUMN_ID AND old.$MEMBER_COLUMN_ORDER < $MEMBER_COLUMN_ORDER;" +
                    "END;"

        private val CALLBACK = object : RoomDatabase.Callback() {

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                /* Just in case not created in onCreate e.g. migration */
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }
        }

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addCallback(CALLBACK)
                    // why are we supporting main thread queries?
                    // Because we currently do not have the the kotlin implementation of the
                    // music player. we must allow main thread queries because currenlty I don't want
                    // do something stupid.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

@Dao
interface Playlists {

    // playlists
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Playlist.Member): Long

    // members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(members: List<Playlist.Member>): List<Long>

    suspend fun insert(playlist: Playlist, tracks: List<Audio>): List<Long> {
        // check if playlist exists.
        // if playlist id == 0 i.e, it doesn't exist; insert it
        // else return id
        val playlistId = if (playlist.id == 0L) insert(playlist) else playlist.id
        // insert and return ids.
        return insert(playlistId, tracks)
    }

    @Transaction
    suspend fun insert(playlistId: Long, tracks: List<Audio>): List<Long> {
        var order = lastPlayOrder(playlistId) ?: -1

        // convert to members
        val members = tracks.map {
            Playlist.Member(playlistID = playlistId, id = "${it.id}", order = ++order)
        }

        // insert and return ids.
        return insert(members)
    }

    @Transaction
    suspend fun insert(playlistId: Long, track: Audio): Long {
        val order = lastPlayOrder(playlistId) ?: -1
        return insert(Playlist.Member(playlistId, "${track.id}", order + 1))
    }

    suspend fun insert(playlist: Playlist, track: Audio): Long {
        // check if playlist exists.
        // if playlist id == 0 i.e, it doesn't exist; insert it
        // else return id
        val playlistId = if (playlist.id == 0L) insert(playlist) else playlist.id
        return insert(playlistId, track)
    }

    /**
     * Returns the [MAX] [COLUMN_PLAY_ORDER] of [TABLE_NAME_MEMBER] associated with [playlistId] or null if [playlistId] !exists
     */
    @Query("SELECT MAX(play_order) FROM tbl_playlist_members WHERE playlist_id = :playlistId")
    suspend fun lastPlayOrder(playlistId: Long): Long?

    /**
     * Update the playlist with new details.
     * Note: Please Provide [Playlist.dateModified] and [Playlist.dateCreated].
     */
    @Update
    suspend fun update(playlist: Playlist): Int

    @Update
    suspend fun update(member: Playlist.Member): Int

    @Delete
    suspend fun delete(playlist: Playlist): Int

    @Delete
    suspend fun delete(member: Playlist.Member): Int

    @Delete
    suspend fun delete(playlists: List<Playlist>): Int

    @Delete
    suspend fun delete(members: ArrayList<Playlist.Member>): Int

    /**
     * Delete from [playlistId] the track with id [track]
     */
    @Transaction
    suspend fun delete(playlistId: Long, track: Audio): Int {
        val member = Playlist.Member(playlistId, "${track.id}", 0)
        return delete(member)
    }

    /**
     * Returns the [COLUMN_PLAY_ORDER] if the track exists in [TABLE_NAME_MEMBER] else null
     */
    @Query("SELECT play_order FROM tbl_playlist_members WHERE playlist_id == :playlistId AND file_id == :trackId")
    suspend fun getPlayOrder(playlistId: Long, trackId: String): Long?

    /**
     * Returns the number of tracks in the playlist.
     */
    @Query("SELECT COUNT(*) FROM tbl_playlist_members WHERE playlist_id == :playlistId")
    suspend fun count(playlistId: Long): Int

    suspend fun count(playlist: Playlist) = count(playlist.id)

    /**
     * Check if the [Playlist.Member] exits in [Playlist]
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlist_members WHERE playlist_id == :playlistId AND file_id == :id)")
    suspend fun exists(playlistId: Long, id: String): Boolean

    /**
     * Check if the [Playlist.Member] exits in [Playlist]
     */
    suspend fun exists(playlistName: String, uTag: String, id: String): Boolean {
        val playlist = get(uTag, playlistName)
        return playlist?.let { exists(it.id, id) } ?: false
    }

    /**
     * Delete the [Playlist.Member] from the [Playlist]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND file_id == :id")
    suspend fun delete(playlistId: Long, id: String): Int

    /**
     * @return [Playlist] represented by [id]
     */
    @Query("SELECT * FROM tbl_playlists WHERE playlist_id == :id")
    suspend fun get(id: Long): Playlist?

    /**
     * @return [Playlist] represented by unique [tag] and [name]
     */
    @Query("SELECT * FROM tbl_playlists WHERE tag == :tag AND name == :name")
    suspend fun get(tag: String, name: String): Playlist?

    /**
     * @return All [Playlist]s matched by the query.
     */
    @Query("SELECT * FROM tbl_playlists WHERE :query IS NULL OR name LIKE '%' || :query || '%'")
    suspend fun get(query: String? = null): List<Playlist>

    /**
     * @return All [Flow][Playlist]s matched by the query.
     */
    @Query("SELECT * FROM tbl_playlists WHERE :query IS NULL OR name LIKE '%' || :query || '%'")
    fun observe(query: String? = null): Flow<List<Playlist>>

    /**
     * @return [Flow][Playlist] matched by [id].
     */
    @Query("SELECT * FROM tbl_playlists WHERE playlist_id == :id")
    fun observe(id: Long): Flow<Playlist?>

    /**
     * @return [Flow][Playlist] matched by the unique [tag] and [name].
     */
    @Query("SELECT * FROM tbl_playlists WHERE tag == :tag AND name = :name")
    fun observe(tag: String, name: String): Flow<Playlist?>

    /**
     * @return [List][Audio]s of [Playlist] represented by [playlistId]
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members INNER JOIN tbl_audios ON file_id == tbl_audios.audio_id WHERE playlist_id = :playlistId ORDER BY play_order ASC")
    suspend fun getAudios(playlistId: Long): List<Audio>

    /**
     * Returns a [Flow]able [Pair] of [Playlist] with [List] [Audio]
     */
    fun observe2(name: String, tag: String): Flow<Pair<Playlist, List<Audio>>?> =
        observe(name = name, tag = tag).map {
            if (it == null)
                null
            else
                it to getAudios(it.id)
        }
}


//TODO: Split each View/Table into repective Dao
@Dao
interface Audios {

    @Query("SELECT MAX(date_modified) FROM tbl_audios")
    suspend fun lastModified(): Long?

    @Insert
    fun insert(audio: Audio): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(values: List<Audio>): List<Long>

    @Query("DELETE FROM tbl_audios WHERE audio_id == :id")
    suspend fun delete(id: Long): Int

    @Deprecated("not to be used for normal purposes")
    @Suppress("FunctionName")
    @RawQuery(observedEntities = [Audio::class])
    fun __raw_delete(query: SupportSQLiteQuery): Int

    @Deprecated("not to be used for normal purposes")
    @Suppress("FunctionName")
    @Transaction
    fun _delete(ids: String): Int {
        //language=SQL
        val query = SimpleSQLiteQuery("DELETE FROM tbl_audios WHERE audio_id NOT IN ($ids)")
        return __raw_delete(query)
    }

    @Delete
    suspend fun delete(audio: Audio): Int

    @Query("SELECT COUNT(*) FROM tbl_audios")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT audio_id FROM tbl_audios WHERE audio_id == :id)")
    suspend fun exists(id: Long): Boolean

    @Query("SELECT * FROM vw_audio_info")
    fun info(): Flow<Audio.Info>

    @Query("SELECT * FROM vw_audio_info")
    suspend fun getInfo(): Audio.Info

    @Query("SELECT * FROM tbl_audios WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    fun observe(query: String? = null): Flow<List<Audio>>

    @Query("SELECT * FROM tbl_audios WHERE audio_id == :id")
    suspend fun get(id: Long): Audio?

    @Query("SELECT * FROM tbl_audios WHERE :query IS NULL OR title LIKE '%' || :query || '%'")
    suspend fun get(query: String? = null): List<Audio>

    @Deprecated("not to be used for normal purposes")
    @RawQuery
    suspend fun get(query: SupportSQLiteQuery): List<Audio>

    @Query("SELECT * FROM vw_audio_album ORDER BY date_modified DESC LIMIT :limit")
    suspend fun getRecentAlbums(limit: Int): List<Audio.Album>

    @Query("SELECT * FROM vw_audio_album ORDER BY date_modified DESC LIMIT :limit")
    fun observeRecentAlbums(limit: Int): Flow<List<Audio.Album>>

    // buckets
    @Query("SELECT * FROM vw_audio_bucket WHERE :query IS NULL OR bucket_path LIKE '%' || :query || '%'")
    fun buckets(query: String? = null): Flow<List<Audio.Bucket>>

    @Query("SELECT * FROM vw_audio_bucket WHERE bucket_path == :path")
    fun bucket(path: String): Flow<Audio.Bucket?>

    fun bucket2(path: String): Flow<Pair<Audio.Bucket, List<Audio>>?> {
        val query =
            SimpleSQLiteQuery("SELECT * FROM tbl_audios WHERE parent_path == ?", arrayOf(path))
        return bucket(path).map { bucket ->
            when (bucket == null) {
                true -> null
                else -> {
                    val list = get(query = query)
                    bucket to list
                }
            }
        }
    }

    // Artists
    @Query("SELECT * FROM vw_audio_artist WHERE :query IS NULL OR artist LIKE '%' || :query || '%'")
    fun artists(query: String? = null): Flow<List<Audio.Artist>>

    @Query("SELECT * FROM vw_audio_artist WHERE artist == :name")
    fun artist(name: String): Flow<Audio.Artist?>

    fun artist2(name: String): Flow<Pair<Audio.Artist, List<Audio>>?> {
        val query = SimpleSQLiteQuery("SELECT * FROM tbl_audios WHERE artist == ?", arrayOf(name))
        return artist(name).map { artist ->
            when (artist == null) {
                true -> null
                else -> {
                    val list = get(query = query)
                    artist to list
                }
            }
        }
    }

    // Albums
    @Query("SELECT * FROM vw_audio_album WHERE :query IS NULL OR album LIKE '%' || :query || '%'")
    fun albums(query: String? = null): Flow<List<Audio.Album>>

    @Query("SELECT * FROM vw_audio_album WHERE album == :name")
    fun album(name: String): Flow<Audio.Album?>

    fun albums2(name: String): Flow<Pair<Audio.Album, List<Audio>>?> {
        val query = SimpleSQLiteQuery("SELECT * FROM tbl_audios WHERE album == ?", arrayOf(name))
        return album(name).map { album ->
            when (album == null) {
                true -> null
                else -> {
                    val list = get(query = query)
                    album to list
                }
            }
        }
    }

    // Genres
    @Query("SELECT * FROM vw_audio_genre WHERE :query IS NULL OR genre LIKE '%' || :query || '%'")
    fun genres(query: String? = null): Flow<List<Audio.Genre>>

    @Query("SELECT * FROM vw_audio_genre WHERE genre == :name")
    fun genre(name: String): Flow<Audio.Genre?>

    fun genre2(name: String): Flow<Pair<Audio.Genre, List<Audio>>?> {
        val query = SimpleSQLiteQuery("SELECT * FROM tbl_audios WHERE genre == ?", arrayOf(name))
        return genre(name).map { genre ->
            when (genre == null) {
                true -> null
                else -> {
                    val list = get(query = query)
                    genre to list
                }
            }
        }
    }
}