package com.prime.player.common

import java.io.Serializable

/**
 * Represents a generic pentad of five values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pentad exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @property first First value.
 * @property second Second value.
 * @constructor Creates a new instance of Pair.
 */
data class Pentad<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) : Serializable {
    /**
     * Returns string representation of the [Pentad] including its [first], [second],[third] [forth] [fifth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}



data class Tetrad<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) : Serializable {
    /**
     * Returns string representation of the [Tetrad] including its [first], [second],[third] and [forth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth)"
}