package com.prime.media.core.util

import android.content.Context
import android.text.format.DateUtils.*
import com.prime.media.R
import com.primex.core.Text
import java.util.*
import kotlin.time.ExperimentalTime

private const val TAG = "Util"

/**
 * A local scope for some util funs.
 */
object DateUtil


/**
 * Return given duration in a human-friendly format. For example, "4
 * minutes" or "1 second". Returns only largest meaningful unit of time,
 * from seconds up to hours.
 */
@Deprecated("find new solution.")
fun DateUtil.formatAsDuration2(mills: Long): Text {
    return when {
        mills >= HOUR_IN_MILLIS -> {
            val hours = ((mills + 1800000) / HOUR_IN_MILLIS).toInt()
            Text(R.plurals.duration_hours, hours, hours)
        }
        mills >= MINUTE_IN_MILLIS -> {
            val minutes = ((mills + 30000) / MINUTE_IN_MILLIS).toInt()
            Text(R.plurals.duration_minutes, minutes, minutes)
        }
        else -> {
            val seconds = ((mills + 500) / SECOND_IN_MILLIS).toInt()
            Text(R.plurals.duration_seconds, seconds, seconds)
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
fun DateUtil.formatAsDuration(context: Context, mills: Long): String {
    val res = context.resources
    return when {
        mills >= HOUR_IN_MILLIS -> {
            val hours = ((mills + 1800000) / HOUR_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_hours, hours, hours
            )
        }
        mills >= MINUTE_IN_MILLIS -> {
            val minutes = ((mills + 30000) / MINUTE_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_minutes, minutes, minutes
            )
        }
        else -> {
            val seconds = ((mills + 500) / SECOND_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_seconds, seconds, seconds
            )
        }
    }
}

@Deprecated("find new solution.")
fun DateUtil.formatAsDuration(mills: Int): String = formatAsDuration(mills.toLong())


/**
 * @return mills formated as hh:mm:ss
 */
@Deprecated("find new solution.")
fun DateUtil.formatAsDuration(mills: Long): String {
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
 * Returns a string describing 'time' as a time relative to 'now'.
 * <p>
 * Time spans in the past are formatted like "42 minutes ago". Time spans in
 * the future are formatted like "In 42 minutes".
 * <p>
 * Can use {@link #FORMAT_ABBREV_RELATIVE} flag to use abbreviated relative
 * times, like "42 mins ago".
 *
 * @param time the time to describe, in milliseconds
 * @param now the current time in milliseconds
 * @param minResolution the minimum timespan to report. For example, a time
 *            3 seconds in the past will be reported as "0 minutes ago" if
 *            this is set to MINUTE_IN_MILLIS. Pass one of 0,
 *            MINUTE_IN_MILLIS, HOUR_IN_MILLIS, DAY_IN_MILLIS,
 *            WEEK_IN_MILLIS
 * @param flags a bit mask of formatting options, such as
 *            {@link #FORMAT_NUMERIC_DATE} or
 *            {@link #FORMAT_ABBREV_RELATIVE}
 */
@OptIn(ExperimentalTime::class)
@Deprecated("find new solution.")
fun DateUtil.formatAsRelativeTimeSpan(mills: Long) = getRelativeTimeSpanString(
    mills, System.currentTimeMillis(), DAY_IN_MILLIS, FORMAT_ABBREV_RELATIVE
) as String

/**
 * @return: String representation of Time like 24:34 i.e., 24min and 128 secs.
 */
@OptIn(ExperimentalTime::class)
@Deprecated("find new solution.")
fun DateUtil.formatAsTime(mills: Long): String {
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


