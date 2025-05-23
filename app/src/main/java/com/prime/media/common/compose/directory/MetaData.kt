/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 23-05-2025.
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

package com.prime.media.common.compose.directory

import android.net.Uri

/**
 * A data class representing the metadata associated with list of files.
 *
 * This class is used to store common information that can be displayed or used for sorting/filtering.
 *
 * @property title The primary title or name of the media item or directory.
 * @property artwork An optional URI or path to an image representing the artwork for this metadata.
 *                  Defaults to `null` if no artwork is available.
 * @property cardinality An optional integer representing a count associated with the list.
 *                       For example, this could be the number of tracks in an album or files in a folder.
 *                       Defaults to `-1` if not applicable or unknown.
 * @property dateModified An optional timestamp (in milliseconds since epoch) indicating when the files
 *                        or folder associated with this metadata was last modified.
 *                        Defaults to `-1L` if not applicable or unknown.
 */
data class MetaData(
    val title: CharSequence,
    val artwork: Uri? = null,
    val cardinality: Int = -1,
    val dateModified: Long = -1L
)