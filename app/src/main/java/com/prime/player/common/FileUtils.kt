package com.prime.player.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import org.jetbrains.annotations.Contract

object FileUtils {
    /**
     * The Unix separator character.
     */
    const val PATH_SEPARATOR = '/'

    /**
     * The extension separator character.
     * @since 1.4
     */
    private const val EXTENSION_SEPARATOR = '.'

    private const val HIDDEN_PATTERN = "/."

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
        if (url.contains(EXTENSION_SEPARATOR))
            url.substring(url.lastIndexOf(EXTENSION_SEPARATOR) + 1).lowercase()
        else
            null

    /**
     * Checks if the file or its ancestors are hidden in System.
     */
    @Contract(pure = true)
    fun areAncestorsHidden(path: String): Boolean = path.contains(HIDDEN_PATTERN)


    const val FLAG_SHORTER = 1 shl 0
    const val FLAG_CALCULATE_ROUNDED = 1 shl 1
    const val FLAG_SI_UNITS = 1 shl 2
    const val FLAG_IEC_UNITS = 1 shl 3

    fun checkStoragePermission(context: Context): Boolean {
        // Verify that all required contact permissions have been granted.
        return (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    fun toFormattedDataUnit(bytes: Long): String {
        return formatBytes(bytes, FLAG_IEC_UNITS)
    }

    private fun formatBytes(sizeBytes: Long, flags: Int): String {
        val unit = if (flags and FLAG_IEC_UNITS != 0) 1024 else 1000
        val isNegative = sizeBytes < 0
        var result = if (isNegative) (-sizeBytes).toFloat() else sizeBytes.toFloat()
        var suffix = "B"
        var mult: Long = 1
        if (result > 900) {
            suffix = "KB"
            mult = unit.toLong()
            result /= unit
        }
        if (result > 900) {
            suffix = "MB"
            mult *= unit.toLong()
            result /= unit
        }
        if (result > 900) {
            suffix = "GB"
            mult *= unit.toLong()
            result /= unit
        }
        if (result > 900) {
            suffix = "TB"
            mult *= unit.toLong()
            result /= unit
        }
        if (result > 900) {
            suffix = "PB"
            mult *= unit.toLong()
            result /= unit
        }
        // Note we calculate the rounded long by ourselves, but still let String.format()
        // compute the rounded value. String.format("%f", 0.1) might not return "0.1" due to
        // floating point errors.
        val roundFactor: Int
        val roundFormat: String
        if (mult == 1L || result >= 100) {
            roundFactor = 1
            roundFormat = "%.0f"
        } else if (result < 1) {
            roundFactor = 100
            roundFormat = "%.2f"
        } else if (result < 10) {
            if (flags and FLAG_SHORTER != 0) {
                roundFactor = 10
                roundFormat = "%.1f"
            } else {
                roundFactor = 100
                roundFormat = "%.2f"
            }
        } else { // 10 <= result < 100
            if (flags and FLAG_SHORTER != 0) {
                roundFactor = 1
                roundFormat = "%.0f"
            } else {
                roundFactor = 100
                roundFormat = "%.2f"
            }
        }
        if (isNegative) {
            result = -result
        }
        val roundedString = String.format(roundFormat, result)

        // Note this might overflow if abs(result) >= Long.MAX_VALUE / 100, but that's like 80PB so
        // it's okay (for now)...
        val roundedBytes =
            if (flags and FLAG_CALCULATE_ROUNDED == 0) 0 else Math.round(result * roundFactor)
                .toLong() * mult / roundFactor
        return "$roundedString $suffix"
    }
}
