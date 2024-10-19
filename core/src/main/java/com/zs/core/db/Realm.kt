/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 28-09-2024.
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

package com.zs.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zs.core.db.Playlist.Track

private const val TAG = "Realm"

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //language=RoomSql
        db.execSQL(
            """
            CREATE TABLE new_tbl_playlist_members (
                playlist_id INTEGER NOT NULL,
                uri TEXT NOT NULL,
                play_order INTEGER NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                artwork_uri TEXT DEFAULT NULL,
                mime_type TEXT DEFAULT NULL,
                PRIMARY KEY(playlist_id, uri),
                FOREIGN KEY(playlist_id) REFERENCES tbl_playlists(playlist_id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        //language=RoomSql
        db.execSQL(
            """
            INSERT INTO new_tbl_playlist_members (playlist_id, uri, play_order, title, subtitle, artwork_uri)
            SELECT playlist_id, uri, play_order, title, subtitle, artwork_uri
            FROM tbl_playlist_members
            """.trimIndent()
        )
        //language=RoomSql
        db.execSQL("DROP TABLE tbl_playlist_members")
        //language=RoomSql
        db.execSQL("ALTER TABLE new_tbl_playlist_members RENAME TO tbl_playlist_members")
        //language=RoomSql
        db.execSQL("CREATE INDEX index_tbl_playlist_members_playlist_id_uri ON tbl_playlist_members(playlist_id, uri)")
    }
}

private const val DB_NAME = "realm_db"

private val CALLBACK = object : Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(Playlists2.TRIGGER_BEFORE_INSERT)
        db.execSQL(Playlists2.TRIGGER_AFTER_DELETE)
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        /* Just in case not created in onCreate e.g. migration */
        db.execSQL(Playlists2.TRIGGER_BEFORE_INSERT)
        db.execSQL(Playlists2.TRIGGER_AFTER_DELETE)
    }
}

@Database(entities = [Playlist::class, Track::class], version = 4, exportSchema = false)
internal abstract class Realm : RoomDatabase() {
    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: Realm? = null
        operator fun invoke(context: Context): Realm {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, Realm::class.java, DB_NAME
                ).addCallback(CALLBACK)
                    // why are we supporting main thread queries?
                    // Because we currently do not have the the kotlin implementation of the
                    // music player. we must allow main thread queries because currenlty I don't want
                    // do something stupid.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigrationFrom(0, 1, 2)
                    .addMigrations(MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    /**
     *
     */
    abstract val playlists: Playlists2
    abstract val playlistsNew: Playlists
}