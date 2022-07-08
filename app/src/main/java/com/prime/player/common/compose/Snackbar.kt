package com.prime.player.common.compose

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel


sealed interface Snack {

    val action: (() -> Unit)?

    val duration: SnackbarDuration

    /**
     * Constructs a text obj from the string itself.
     */
    data class Text(
        val label: String = "",
        override val duration: SnackbarDuration,
        val message: String,
        override val action: (() -> Unit)? = null
    ) : Snack

    /**
     * Construct a string resource object.
     */
    data class Resource(
        override val duration: SnackbarDuration,
        @StringRes val label: Int,
        @StringRes val message: Int,
        override val action: (() -> Unit)? = null,
        @Suppress("ArrayInDataClass") val formatArgs: Array<Any> = emptyArray()
    ) : Snack
}


/**
 * Construct a Snack from the provided string [label], and [message]
 */
fun Snack(
    duration: SnackbarDuration = SnackbarDuration.Short,
    label: String = "",
    message: String,
    action: (() -> Unit)? = null
): Snack =
    Snack.Text(
        label = label,
        message = message,
        action = action,
        duration = duration
    )

/**
 * Construct a Snack from the provided resource [label], and [message]
 */
fun Snack(
    duration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes label: Int = ResourcesCompat.ID_NULL,
    @StringRes message: Int,
    vararg formatArgs: Any,
    action: (() -> Unit)? = null,
): Snack =
    Snack.Resource(
        label = label,
        message = message,
        formatArgs = arrayOf(*formatArgs),
        action = action,
        duration = duration
    )

typealias SnackDataChannel = Channel<Snack>

fun SnackDataChannel(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((Snack) -> Unit)? = null
): SnackDataChannel =
    Channel(
        capacity,
        onBufferOverflow,
        onUndeliveredElement
    )

suspend fun SnackDataChannel.send(
    duration: SnackbarDuration = SnackbarDuration.Short,
    label: String = "",
    message: String,
    action: (() -> Unit)? = null
) = send(
    Snack(
        label = label,
        message = message,
        action = action,
        duration = duration
    )
)


suspend fun SnackDataChannel.send(
    duration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes label: Int = ResourcesCompat.ID_NULL,
    @StringRes message: Int,
    vararg formatArgs: Any,
    action: (() -> Unit)? = null,
) =
    send(
        Snack(
            label = label,
            message = message,
            formatArgs = formatArgs,
            action = action,
            duration = duration
        )
    )


val LocalSnackDataChannel = staticCompositionLocalOf<SnackDataChannel> {
    error("no local messenger provided!!")
}