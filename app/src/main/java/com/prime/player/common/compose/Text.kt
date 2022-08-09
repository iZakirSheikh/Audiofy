@file:Suppress("FunctionName")

package com.prime.player.common.compose


import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import kotlin.reflect.KProperty

/**
 * Represents a Text Item.
 *
 * This interface can be used to send text [Resources] directly from the [ViewModel]s, [Services]; i.e,
 * directly from the non UI components
 */
@Immutable
@Stable
sealed interface Text

/**
 * A Raw text. i.e., [String]
 */
@JvmInline
private value class Raw(val value: AnnotatedString) : Text

/**
 * A value class holding [Resource] [String]
 */
@JvmInline
private value class StringResource(val id: Int) : Text

/**
 * A data class holding [Resource] String with [formatArgs]
 */
data class StringResource2(val id: Int, val formatArgs: Array<out Any>) : Text {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringResource2

        if (id != other.id) return false
        if (!formatArgs.contentEquals(other.formatArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

/**
 * A value class holding [Html] [Resource]. Currently only supports as are supported by
 * [stringHtmlResource()] function.
 */
@JvmInline
private value class HtmlResource(val id: Int) : Text

/**
 * A data class holds [PluralImpl] resource [String]s.
 * TODO: use packed value to save [quantity] + [id] in [Long] value
 */
private data class PluralImpl(
    val id: Int,
    val quantity: Int,
    val formatArgs: Array<out Any>? = null
) : Text {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluralImpl

        if (id != other.id) return false
        if (quantity != other.quantity) return false
        if (formatArgs != null) {
            if (other.formatArgs == null) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false
        } else if (other.formatArgs != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + quantity
        result = 31 * result + (formatArgs?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Constructs a [Raw] String [Text] wrapper.
 */
fun Text(value: String): Text = Raw(AnnotatedString(value))

/**
 * Constructs an [AnnotatedString] String [Text] wrapper.
 */
fun Text(value: AnnotatedString): Text = Raw(value)

/**
 * Constructs an [StringResource] String [Text] wrapper.
 */
fun Text(@StringRes id: Int): Text = StringResource(id)

/**
 * Constructs html string resource [Text] wrapper.
 */
fun Html(@StringRes id: Int): Text = HtmlResource(id)

/**
 * @see Plural
 */
fun Plural(id: Int, quantity: Int): Text = PluralImpl(id, quantity)

/**
 * Composes the [Plural] Text resource wrapper along with [formatArgs].
 */
fun Plural(id: Int, quantity: Int, vararg formatArgs: Any): Text =
    PluralImpl(id, quantity, formatArgs)


/**
 * A simple function that returns the [Bundle] value inside this wrapper.
 *  It is either resource id from which this Wrapper was created or the raw text [String]
 */
val Text.raw: Any
    get() =
        when (this) {
            is HtmlResource -> id
            is PluralImpl -> id
            is Raw -> value.text
            is StringResource -> id
            is StringResource2 -> id
        }

/**
 * Unpacks the text wrapper to result [AnnotatedString]
 */
@Composable
@ReadOnlyComposable
@NonRestartableComposable
fun stringResource(value: Text): AnnotatedString =
    when (value) {
        is HtmlResource -> stringHtmlResource(id = value.id)
        is PluralImpl -> {
            val (id, quantity, args) = value
            val result =
                when (args == null) {
                    true -> stringQuantityResource(id = id, quantity = quantity)
                    else -> stringQuantityResource(id = id, quantity = quantity, formatArgs = args)
                }
            AnnotatedString(result)
        }
        is Raw -> value.value
        is StringResource -> AnnotatedString(stringResource(id = value.id))
        is StringResource2 -> AnnotatedString(stringResource(id = value.id, value.formatArgs))
    }


/**
 * Unpacks the text wrapper to result [AnnotatedString]
 */
@JvmName("stringResource1")
@Composable
@ReadOnlyComposable
@NonRestartableComposable
fun stringResource(value: Text?): AnnotatedString? =
    when (value == null) {
        true -> null
        else -> stringResource(value = value)
    }


/**
 * Permits property delegation of 'State' with [Text] using `by` for [State].
 * @return [AnnotatedString] from [Text]
 */
@Composable
@ReadOnlyComposable
@NonRestartableComposable
@Suppress("NOTHING_TO_INLINE")
@JvmName("getValue2")
@Deprecated(
    "Some sort of error occurs when this is used with State",
    level = DeprecationLevel.HIDDEN
)
operator fun State<Text>.getValue(thisObj: Text?, property: KProperty<*>): AnnotatedString =
    stringResource(value = value)

@JvmName("getValueText1")
@Composable
@ReadOnlyComposable
@NonRestartableComposable
@Suppress("NOTHING_TO_INLINE")
@Deprecated(
    "Some sort of error occurs when this is used with State",
    level = DeprecationLevel.HIDDEN
)
operator fun State<Text?>.getValue(thisObj: Text?, property: KProperty<*>): AnnotatedString? =
    stringResource(value = value)

/**
 * Permits property delegation of 'Text' using `by` for [Text].
 * @return [AnnotatedString] from [Text]
 */
@Composable
@ReadOnlyComposable
@NonRestartableComposable
@Suppress("NOTHING_TO_INLINE")
inline operator fun Text.getValue(thisObj: Any?, property: KProperty<*>): AnnotatedString =
    stringResource(value = this)


/**
 * @see getValue
 */
@JvmName("getValue1")
@Composable
@ReadOnlyComposable
@NonRestartableComposable
@Suppress("NOTHING_TO_INLINE")
inline operator fun Text?.getValue(thisObj: Any?, property: KProperty<*>): AnnotatedString? =
    stringResource(value = this)