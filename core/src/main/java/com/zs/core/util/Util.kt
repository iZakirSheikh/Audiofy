/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 11-07-2024.
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

package com.zs.core.util

import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import java.util.regex.Matcher
import java.util.regex.Pattern

private const val TAG = "Util"

//language=RegExp
private val ISO6709LocationPattern = Pattern.compile("([+\\-][0-9.]+)([+\\-][0-9.]+)")


internal inline fun <T, R> T.runCatching(tag: String, block: T.() -> R): R? {
    return try {
        block()
    } catch (e: Throwable) {
        Log.d(tag, "runCatching: $e")
        null
    }
}

/**
 * This method parses the given string representing a geographic point location by coordinates in ISO 6709 format
 * and returns the latitude and the longitude in float. If `location` is not in ISO 6709 format,
 * this method returns `null`
 *
 * @param location a String representing a geographic point location by coordinates in ISO 6709 format
 * @return `null` if the given string is not as expected, an array of floats with size 2,
 * where the first element represents latitude and the second represents longitude, otherwise.
 */
val MediaMetadataRetriever.latLong: DoubleArray?
    get() = runCatching(TAG) {
        val location =
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION) ?: return@runCatching null
        val m: Matcher = ISO6709LocationPattern.matcher(location)
        if (m.find() && m.groupCount() == 2) {
            val latstr: String = m.group(1) ?: return@runCatching null
            val lonstr: String = m.group(2) ?: return@runCatching null
            val lat = latstr.toDouble()
            val lon = lonstr.toDouble()
            doubleArrayOf(lat, lon)
        } else null
    }

/**
 * Returns a flow that mirrors the original flow, but debounces emissions after the first one.
 *
 * The first emission from the original flow is emitted immediately. Subsequent emissions
 * are debounced by the specified [delayMillis].
 *
 * @param delayMillis The duration in milliseconds to debounce subsequent emissions.
 * @return A flow that debounces emissions after the first one.
 *
 * @see [debounce]
 */
@FlowPreview
fun <T> Flow<T>.debounceAfterFirst(delayMillis: Long): Flow<T> {
    var firstEmission = true
    return this
        .debounce {
            if (firstEmission) {
                firstEmission = false
                0L
            } else {
                delayMillis
            }
        }
}