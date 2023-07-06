package com.prime.media.core.util

import android.content.Context
import org.jetbrains.annotations.Contract

/**
 * A scope for [FileUtils] functions.
 */
object FileUtils {
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
    fun name(path: String): String = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1)

    /**
     * @return parent of path.
     */
    fun parent(path: String): String = path.replace("$PATH_SEPARATOR${name(path = path)}", "")

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
        if (url.contains(EXTENSION_SEPARATOR)) url.substring(url.lastIndexOf(EXTENSION_SEPARATOR) + 1)
            .lowercase()
        else null

    /**
     * Checks if the file or its ancestors are hidden in System.
     */
    @Contract(pure = true)
    fun areAncestorsHidden(path: String): Boolean = path.contains(HIDDEN_PATTERN)

    /**
     * Returns [bytes] as formatted data unit.
     */
    @Deprecated(message = "find new solution.")
    fun toFormattedDataUnit(
        context: Context,
        bytes: Long,
        short: Boolean = true,
    ): String = when (short) {
        true -> android.text.format.Formatter.formatShortFileSize(context, bytes)
        else -> android.text.format.Formatter.formatFileSize(context, bytes)
    }
}

