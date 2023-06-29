package com.prime.media.core

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.material.ContentAlpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alias for [AnimationConstants] to provide shorter name for animation related constants.
 */
typealias Anim = AnimationConstants

/**
 * Long duration time in milliseconds, used for animations.
 */
private const val LONG_DURATION_TIME = 500

/**
 * The [Anim.LongDurationMills] is a constant value that returns the [LONG_DURATION_TIME].
 */
val Anim.LongDurationMills get() = LONG_DURATION_TIME

/**
 * Medium duration time in milliseconds, used for animations.
 */
private const val MEDIUM_DURATION_TIME = 400

/**
 * The [Anim.MediumDurationMills] is a constant value that returns the [MEDIUM_DURATION_TIME].
 */
val Anim.MediumDurationMills get() = MEDIUM_DURATION_TIME

/**
 * Short duration time in milliseconds, used for animations.
 */
private const val SHORT_DURATION_TIME = 200

/**
 * The [Anim.ShortDurationMills] is a constant value that returns the [SHORT_DURATION_TIME].
 */
val Anim.ShortDurationMills get() = SHORT_DURATION_TIME

/**
 * Short duration time in milliseconds, used for activity transitions.
 */
private const val ACTIVITY_SHORT_DURATION = 150

/**
 * The [Anim.ActivityShortDurationMills] is a constant value that returns the [ACTIVITY_SHORT_DURATION].
 */
val Anim.ActivityShortDurationMills get() = ACTIVITY_SHORT_DURATION

/**
 * Long duration time in milliseconds, used for activity transitions.
 */
private const val ACTIVITY_LONG_DURATION = 220

/**
 * The [Anim.ActivityLongDurationMills] is a constant value that returns the [ACTIVITY_LONG_DURATION].
 */
val Anim.ActivityLongDurationMills get() = ACTIVITY_LONG_DURATION

/**
 * Padding values used in [androidx.compose.foundation.layout.PaddingValues] and [androidx.compose.foundation.layout.PaddingModifier].
 */
object ContentPadding {
    /**
     * A small padding value of 4dp.
     */
    val small: Dp = 4.dp

    /**
     * A medium padding value of 8dp.
     */
    val medium: Dp = 8.dp

    /**
     * A normal padding value of 16dp.
     */
    val normal: Dp = 16.dp

    /**
     * A large padding value of 32dp.
     */
    val large: Dp = 32.dp

    /**
     * A large padding value of 22dp.
     */
    val xLarge: Dp = 32.dp
}


/**
 * Elevation values used in [androidx.compose.foundation.layout.Box] and [androidx.compose.material.MaterialTheme].
 */
object ContentElevation {
    /**
     * Zero elevation.
     */
    val none = 0.dp

    /**
     * A low elevation value of 6dp.
     */
    val low = 6.dp

    /**
     * A medium elevation value of 12dp.
     */
    val medium = 12.dp

    /**
     * A high elevation value of 20dp.
     */
    val high = 20.dp

    /**
     * An extra high elevation value of 30dp.
     */
    val xHigh = 30.dp
}


/**
 * The recommended divider Alpha
 */
val ContentAlpha.Divider
    get() = com.prime.media.core.Divider
private const val Divider = 0.12f

/**
 * The recommended LocalIndication Alpha
 */
val ContentAlpha.Indication
    get() = com.prime.media.core.Indication
private const val Indication = 0.1f