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

package com.zs.core.common

import android.os.Environment
import org.jetbrains.annotations.Contract
import java.io.File

/**
 * A scope for [PathUtils] functions.
 */
object PathUtils {

    private const val TAG = "PathUtils"

    // Regex to validate folder names similar to Windows/Android rules, ensuring the name is not blank,
    // does not start with an underscore, and does not contain special characters overall.
    // ^(?!_): Ensures the name does not start with an underscore.
    // [\\p{L}\\p{N}]: The first character must be a letter or a number.
    // [^<>:\"/\\|?*\\s]*$: The subsequent characters must not be any special characters.
    val VALID_NAME_REGEX = Regex("^(?!_)[\\p{L}\\p{N}][\\p{L}\\p{N}^<>:\"/\\\\|?*\\s]*$")

    /**
     * The Unix separator character.
     */
    const val PATH_SEPARATOR = '/'

    /**
     * The extension separator character.
     * @since 1.4
     */
    const val EXTENSION_SEPARATOR = '.'

    const val HIDDEN_PATTERN = "/."

    /**
     * Gets the name minus the path from a full fileName.
     *
     * @param path  the fileName to query
     * @return the name of the file without the path
     */
    fun name(path: String): String =
        path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1)

    /**
     * @return parent of path.
     */
    fun parent(path: String): String =
        path.replace("$PATH_SEPARATOR${name(path = path)}", "")

    /**
     * Returns the file extension or null string if there is no extension. This method is a
     * convenience method for obtaining the extension of a url and has undefined
     * results for other Strings.
     * It is Assumed that Url is file
     *
     * @param url  Url of the file
     *
     * @return extension
     */
    fun extension(url: String): String? =
        if (url.contains(EXTENSION_SEPARATOR))
            url.substring(url.lastIndexOf(EXTENSION_SEPARATOR) + 1).lowercase()
        else
            null

    /**
     * Checks if the file or its ancestors are hidden in System.
     */
    @Contract(pure = true)
    fun areAncestorsHidden(path: String): Boolean =
        path.contains(HIDDEN_PATTERN)

    /**
     * Checks if a given path corresponds to removable storage.
     *
     * @param path The path to check.
     * @return True if the path is on removable storage, false otherwise.
     */
    fun isRemovableStorage(path: String): Boolean {
        val externalStorageDirectory = Environment.getExternalStorageDirectory().absolutePath
        val res = runCatching(TAG) {
            !path.startsWith(externalStorageDirectory) &&
                    Environment.isExternalStorageRemovable(File(path))
        }
        return res == true
    }
}