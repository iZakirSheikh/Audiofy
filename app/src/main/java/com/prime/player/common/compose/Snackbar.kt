package com.prime.player.common.compose

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.res.ResourcesCompat
import com.primex.core.Result
import com.primex.core.Text
import com.primex.core.buildResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

sealed interface Snack {

    val action: (() -> Unit)?

    val duration: SnackbarDuration

    val label: Text?

    val message: Text

    operator fun component1(): Text? = label

    operator fun component2(): Text = message

    operator fun component3(): SnackbarDuration = duration

    operator fun component4(): (() -> Unit)? = action
}


private class SnackImpl(
    override val action: (() -> Unit)?,
    override val duration: SnackbarDuration,
    override val label: Text?,
    override val message: Text
) : Snack {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnackImpl

        if (duration != other.duration) return false
        if (label != other.label) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action?.hashCode() ?: 0
        result = 31 * result + duration.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }

    override fun toString(): String {
        return "SnackImpl(duration=$duration, label=$label, message=$message)"
    }
}

/**
 * Construct a Snack from the provided string [label], and [message]
 */
fun Snack(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
    label: String? = null,
    action: (() -> Unit)? = null
): Snack =
    SnackImpl(
        action = action,
        duration = duration,
        label = if (label == null) null else Text(label),
        message = Text(message)
    )


/**
 * Construct a Snack from the provided resource [label], and [message]
 */
fun Snack(
    @StringRes message: Int,
    duration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes label: Int? = null,
    action: (() -> Unit)? = null,
): Snack =
    SnackImpl(
        action = action,
        duration = duration,
        label = if (label == null) null else Text(label),
        message = Text(message)
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
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
    label: String? = null,
    action: (() -> Unit)? = null
) =
    send(
        Snack(
            label = label,
            message = message,
            action = action,
            duration = duration
        )
    )

suspend fun SnackDataChannel.send(
    @StringRes message: Int,
    duration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes label: Int? = null,
    action: (() -> Unit)? = null,
) =
    send(
        Snack(
            label = label,
            message = message,
            action = action,
            duration = duration
        )
    )

suspend fun SnackDataChannel.send(
    @StringRes message: Int,
    vararg formatArgs: Any,
    duration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes label: Int? = null,
    action: (() -> Unit)? = null,
) =
    send(
        SnackImpl(
            action = action,
            duration = duration,
            label = if (label == null) null else Text(label),
            message = Text(message, formatArgs = formatArgs)
        )
    )


suspend fun SnackDataChannel.send(
    message: Text,
    duration: SnackbarDuration = SnackbarDuration.Short,
    label: Text? = null,
    action: (() -> Unit)? = null,
) = send(
    SnackImpl(action = action, duration = duration, label = label, message = message)
)


val LocalSnackDataChannel =
    staticCompositionLocalOf<SnackDataChannel> {
        error("no local messenger provided!!")
    }