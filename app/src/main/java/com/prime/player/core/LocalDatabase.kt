package com.prime.player.core

import android.content.Context
import androidx.room.*
import com.prime.player.core.models.Playlist
import kotlinx.coroutines.flow.Flow
import java.util.*


@Dao
interface LocalPlaylists {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: Playlist): Long

    @Query("DELETE FROM Playlists WHERE `id` = :playlistID")
    fun delete(playlistID: Long): Int

    @Query("SELECT EXISTS (SELECT 1 FROM Playlists WHERE `id` = :playlistID)")
    fun contains(playlistID: Long): Boolean

    /**
     * Returns the [Flow] of [Word] objects if their are any otherwise empty
     */
    @Query("SELECT * FROM Playlists")
    fun getPlaylists(): Flow<List<Playlist>>

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(playlist: Playlist): Int

    @Query("SELECT * FROM Playlists WHERE `name` = :name")
    fun getPlaylistByName(name: String): Playlist?

    companion object {
        fun get(context: Context): LocalPlaylists {
            return LocalDBImpl.get(context).localPlaylistDao()
        }
    }
}


@Database(
    entities = [Playlist::class],
    version = 1,
    exportSchema = false
)
private abstract class LocalDBImpl : RoomDatabase() {

    abstract fun localPlaylistDao(): LocalPlaylists

    companion object {
        private const val TAG = "LocalDB"
        private const val DB_NAME = "LocalDatabase"

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LocalDBImpl? = null

        fun get(context: Context): LocalDBImpl {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDBImpl::class.java,
                    DB_NAME
                )
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

