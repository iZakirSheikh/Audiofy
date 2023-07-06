package com.prime.media.core

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.material.ContentAlpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Setup animation related default things
typealias Anim = AnimationConstants

private const val LONG_DURATION_TIME = 500

/**
 * 500 Mills
 */
val Anim.LongDurationMills get() = LONG_DURATION_TIME
private const val MEDIUM_DURATION_TIME = 400

/**
 * 400 Mills
 */
val Anim.MediumDurationMills get() = MEDIUM_DURATION_TIME
private const val SHORT_DURATION_TIME = 200

/**
 * 200 Mills
 */
val Anim.ShortDurationMills get() = SHORT_DURATION_TIME
private const val ACTIVITY_SHORT_DURATION = 150

/**
 * 150 Mills
 */
val Anim.ActivityShortDurationMills get() = ACTIVITY_SHORT_DURATION
private const val ACTIVITY_LONG_DURATION = 220

/**
 * 220 Mills
 */
val Anim.ActivityLongDurationMills get() = ACTIVITY_LONG_DURATION

object ContentPadding {
    /**
     * A small 4 [Dp] Padding
     */
    val small: Dp = 4.dp

    /**
     * A Medium 8 [Dp] Padding
     */
    val medium: Dp = 8.dp

    /**
     * Normal 16 [Dp] Padding
     */
    val normal: Dp = 16.dp

    /**
     * Large 22 [Dp] Padding
     */
    val large: Dp = 22.dp

    /**
     * Large 32 [Dp] Padding
     */
    val xLarge: Dp = 32.dp
}

/**
 * The Standard Elevation Values.
 */
object ContentElevation {
    /**
     * Zero Elevation.
     */
    val none = 0.dp

    /**
     * Elevation of 6 [Dp]
     */
    val low = 6.dp

    /**
     * Elevation of 12 [Dp]
     */
    val medium = 12.dp

    /**
     * Elevation of 20 [Dp]
     */
    val high = 20.dp

    /**
     * Elevation of 30 [Dp]
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