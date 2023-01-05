package com.prime.player.core

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prime.player.core.Playlist.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking


/**
 * The member table of playlist.
 */
private const val TABLE_PLAYLIST_MEMBER = "tbl_playlist_members"

private const val PLAYLIST_COLUMN_ID = "playlist_id"
private const val MEMBER_COLUMN_ORDER = "play_order"
private const val MEMBER_FILE_ID = "media_id"
private const val MEMBER_FILE_URI = "uri"

@Stable
@Entity(tableName = "tbl_playlists")
data class Playlist(
    @JvmField val name: String,
    @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = PLAYLIST_COLUMN_ID) val id: Long = 0,
    @ColumnInfo(defaultValue = "") val desc: String = "",
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis(),
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long = System.currentTimeMillis(),
) {
    @Entity(
        tableName = TABLE_PLAYLIST_MEMBER,
        primaryKeys = [PLAYLIST_COLUMN_ID, MEMBER_FILE_URI],
        foreignKeys = [
            ForeignKey(
                entity = Playlist::class,
                parentColumns = [PLAYLIST_COLUMN_ID],
                childColumns = [PLAYLIST_COLUMN_ID],
                onDelete = CASCADE
            )
        ],
        indices = [
            Index(value = [PLAYLIST_COLUMN_ID, MEMBER_FILE_URI], unique = false)
        ]
    )
    @Stable
    data class Member(
        @JvmField @ColumnInfo(name = PLAYLIST_COLUMN_ID) val playlistID: Long,
        @JvmField @ColumnInfo(name = MEMBER_FILE_ID) val id: String,
        @JvmField @ColumnInfo(name = MEMBER_COLUMN_ORDER) val order: Int,
        @JvmField @ColumnInfo(name = MEMBER_FILE_URI) val uri: String,
        @JvmField val title: String,
        @JvmField val subtitle: String,
        @JvmField @ColumnInfo(name = "artwork_uri") val artwork: String? = null
    )
}


@Database(entities = [Playlist::class, Member::class], version = 3, exportSchema = false)
abstract class Realm : RoomDatabase() {

    abstract val playlists: Playlists

    companion object {
        private const val DB_NAME = "realm_db"

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

        private val CALLBACK = object : Callback() {

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
        private var INSTANCE: Realm? = null

        fun get(context: Context): Realm {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Realm::class.java,
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

    companion object {
        operator fun invoke(context: Context) = Realm.get(context).playlists

        /**
         * A prefix char for private playlists.
         */
        const val PRIVATE_PLAYLIST_PREFIX = '_'
    }

    // playlists
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(playlist: Playlist): Long

    /**
     * Returns the [MAX] [COLUMN_PLAY_ORDER] of [TABLE_NAME_MEMBER] associated with [playlistId] or null if [playlistId] !exists
     */
    @Query("SELECT MAX(play_order) FROM tbl_playlist_members WHERE playlist_id = :playlistId")
    suspend fun lastPlayOrder(playlistId: Long): Int?

    /**
     * Update the playlist with new details.
     * Note: Please Provide [Playlist.dateModified] and [Playlist.dateCreated].
     */
    @Update
    suspend fun update(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlists: List<Playlist>): Int

    /**
     * @return [Playlist] represented by [id]
     */
    @Query("SELECT * FROM tbl_playlists WHERE playlist_id == :id")
    suspend fun get(id: Long): Playlist?

    /**
     * @return [Playlist] represented by [id]
     */
    @Query("SELECT * FROM tbl_playlists WHERE name == :name")
    suspend fun get(name: String): Playlist?

    /**
     * @return All [Flow][Playlist]s matched by the query.
     */
    @Query("SELECT * FROM tbl_playlists WHERE :query IS NULL OR name LIKE '%' || :query || '%'")
    fun observe(query: String? = null): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Member): Long

    // members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(members: List<Member>): List<Long>

    /**
     * This is not the recommended way to do it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(member: Member): Long

    @Delete
    suspend fun delete(member: Member): Int

    @Delete
    suspend fun delete(members: ArrayList<Member>): Int

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    suspend fun get(playlistId: Long, uri: String): Member?

    /**
     * Check if the [Playlist.Member] exits in [Playlist]
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri)")
    suspend fun exists(playlistId: Long, uri: String): Boolean

    /**
     * Checks if [Playlist] [name] exits
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlists WHERE name == :name)")
    suspend fun exists(name: String): Boolean

    suspend fun exists(name: String, uri: String): Boolean{
        val id = get(name)?.id ?: 0
        return exists(id, uri)
    }

    /**
     * Delete the [Playlist.Member] from the [Playlist]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    suspend fun delete(playlistId: Long, uri: String): Int


    /**
     * Delete the [Playlist.Member] where [Playlist.Member.order] > [order]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND play_order >= :order")
    suspend fun delete(playlistId: Long, order: Long): Int

    /**
     * @return [List][Audio]s of [Playlist] represented by [playlistId]
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :playlistId ORDER BY play_order ASC")
    fun observe2(playlistId: Long): Flow<List<Member>>

    /**
    * Observes the [Playlist] spacified by the name.
    */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members LEFT JOIN tbl_playlists ON tbl_playlist_members.playlist_id == tbl_playlists.playlist_id WHERE tbl_playlists.name == :name ORDER BY tbl_playlist_members.play_order ASC")
    fun observe2(name: String): Flow<List<Member>>

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :id ORDER BY play_order ASC")
    suspend fun getMembers(id: Long): List<Member>

    suspend fun getMembers(name: String): List<Member> {
        val x = get(name) ?: return emptyList()
        return getMembers(x.id)
    }
}