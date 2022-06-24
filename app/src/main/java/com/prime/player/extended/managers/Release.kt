package com.prime.player.extended.managers

import android.util.Log
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

private const val TAG = "Release"

/**
 * This reflects the [Json] class in your server.
 * @param markets: The [Market]s in descending order of priority.
 */
data class Release(
    val name: String,
    @SerializedName("version_code")
    val version: Long,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("release_notes")
    val notes: String,
    @SerializedName("available_markets")
    @JsonAdapter(MarketListAdapter::class)
    val markets: List<Market>,
)


private class MarketListAdapter : TypeAdapter<List<Market>>() {
    override fun write(out: JsonWriter?, value: List<Market>?) {
        error("operation not separated.")
    }

    override fun read(reader: JsonReader): List<Market> {
        val list = ArrayList<Market>()
        reader.beginArray()
        while (reader.hasNext()) {
            val token = reader.nextString()
            try {
                val market = Market.valueOf(token)
                list.add(market)
            } catch (e: Exception) {
                Log.e(TAG, "read: $token parse exception")
            }
        }
        reader.endArray()
        return list
    }
}