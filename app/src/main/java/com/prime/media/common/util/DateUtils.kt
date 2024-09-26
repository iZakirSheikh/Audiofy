package com.prime.media.common.util

import android.content.Context
import android.text.format.DateUtils
import com.prime.media.R
import com.primex.core.Text
import java.util.Locale
import kotlin.time.ExperimentalTime

/**
 * A local scope for some util funs.
 */
object DateUtils {
    /**
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     */
    @Deprecated("find new solution.")
    fun formatAsDuration2(mills: Long): Text {
        return when {
            mills >= DateUtils.HOUR_IN_MILLIS -> {
                val hours = ((mills + 1800000) / DateUtils.HOUR_IN_MILLIS).toInt()
                Text(R.plurals.duration_hours_d, hours, hours)
            }

            mills >= DateUtils.MINUTE_IN_MILLIS -> {
                val minutes = ((mills + 30000) / DateUtils.MINUTE_IN_MILLIS).toInt()
                Text(R.plurals.duration_minutes_d, minutes, minutes)
            }

            else -> {
                val seconds = ((mills + 500) / DateUtils.SECOND_IN_MILLIS).toInt()
                Text(R.plurals.duration_seconds_d, seconds, seconds)
            }
        }
    }

    /**
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     *
     * @hide
     */
    @Deprecated(message = "Use toDuration(mills) with Text return ", level = DeprecationLevel.ERROR)
    fun formatAsDuration(context: Context, mills: Long): String {
        val res = context.resources
        return when {
            mills >= DateUtils.HOUR_IN_MILLIS -> {
                val hours = ((mills + 1800000) / DateUtils.HOUR_IN_MILLIS).toInt()
                res.getQuantityString(
                    R.plurals.duration_hours_d, hours, hours
                )
            }

            mills >= DateUtils.MINUTE_IN_MILLIS -> {
                val minutes = ((mills + 30000) / DateUtils.MINUTE_IN_MILLIS).toInt()
                res.getQuantityString(
                    R.plurals.duration_minutes_d, minutes, minutes
                )
            }

            else -> {
                val seconds = ((mills + 500) / DateUtils.SECOND_IN_MILLIS).toInt()
                res.getQuantityString(
                    R.plurals.duration_seconds_d, seconds, seconds
                )
            }
        }
    }

    @Deprecated("find new solution.")
    fun formatAsDuration(mills: Int): String = formatAsDuration(mills.toLong())

    /**
     * @return mills formated as hh:mm:ss
     */
    @Deprecated("find new solution.")
    fun formatAsDuration(mills: Long): String {
        var minutes: Long = mills / 1000 / 60
        val seconds: Long = mills / 1000 % 60
        return if (minutes < 60) {
            String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        } else {
            val hours = minutes / 60
            minutes %= 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        }
    }

    /**
     * @see DateUtils.getRelativeTimeSpanString
     */
    @Deprecated("find new solution.")
    fun formatAsRelativeTimeSpan(mills: Long) =
        DateUtils.getRelativeTimeSpanString(
            mills,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ) as String

    /**
     * @return: String representation of Time like 24:34 i.e., 24min and 128 secs.
     */
    @OptIn(ExperimentalTime::class)
    @Deprecated("find new solution.")
    fun formatAsTime(mills: Long): String {
        var minutes: Long = mills / 1000 / 60
        val seconds: Long = mills / 1000 % 60
        return if (minutes < 60) String.format(
            Locale.getDefault(), "%01d:%02d", minutes, seconds
        ) else {
            val hours = minutes / 60
            minutes %= 60
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        }
    }
}