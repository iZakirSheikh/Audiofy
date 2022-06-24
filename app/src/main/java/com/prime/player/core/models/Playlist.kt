package com.prime.player.core.models

import androidx.room.*
import org.json.JSONArray

@Entity(tableName = "Playlists", indices = [Index(value = ["id", "name"], unique = true)])
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val updateTime: Long = System.currentTimeMillis(),
    @field:TypeConverters(ListJSONTypeConverter::class)
    val audios: List<Long> = ArrayList()
)

/**
 * A Type Converter class required to serialized data to and from list
 */
class ListJSONTypeConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun fromJSONtoList(value: String): List<Long> {
            val array = JSONArray(value)
            val result = ArrayList<Long>()
            for (i in 0 until array.length()) {
                result.add(array.getLong(i))
            }
            return result
        }

        @TypeConverter
        @JvmStatic
        fun fromListToJSON(list: List<Long>): String {
            val array = JSONArray()
            list.forEach {
                array.put(it)
            }
            return array.toString()
        }
    }
}
