package com.prime.media.impl

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.Channel.Result as SnackbarResult
import com.prime.media.core.compose.Channel.Duration as SnackbarDuration
import com.prime.media.core.compose.Channel as SnackbarHostState2
import com.primex.core.getQuantityText2
import com.primex.core.getText2

interface SystemDelegate {

    @Deprecated("Try to avoid using this.")
    val resources: Resources

    @Deprecated("Try to avoid using this.")
    val context: Context

    /**
     * @see getText2
     */
    fun getText(@StringRes id: Int): CharSequence

    /**
     * @see getText2
     */
    fun getText(@StringRes id: Int, vararg args: Any): CharSequence

    /**
     * @see getQuantityText2
     */
    fun getQuantityText(@PluralsRes id: Int, quantity: Int, vararg args: Any): CharSequence

    /**
     * @see getQuantityText2
     */
    fun getQuantityText(@PluralsRes id: Int, quantity: Int): CharSequence

    /**
     * Shows [Toast] to user.
     */
    fun showToast(msg: CharSequence, duration: Int = Toast.LENGTH_SHORT)

    fun showToast(@StringRes msg:Int, duration: Int = Toast.LENGTH_SHORT)

    /**
     * @see show
     */
    suspend fun showSnackbar(
        message: CharSequence,
        action: CharSequence? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = if (action == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
    ): SnackbarResult

    suspend fun showSnackbar(
        @StringRes message: Int,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = if (action == ResourcesCompat.ID_NULL) SnackbarDuration.Short else SnackbarDuration.Indefinite
    ): SnackbarResult
}

@OptIn(ExperimentalTextApi::class)
fun SystemDelegate(ctx: Context, channel: SnackbarHostState2) =
    object : SystemDelegate {

        @Deprecated("Try to avoid using this.")
        override val resources: Resources
            get() = ctx.resources

        @Deprecated("Try to avoid using this.")
        override val context: Context
            get() = ctx

        override fun getText(id: Int): CharSequence = resources.getText2(id)
        override fun getText(id: Int, vararg args: Any) =
            resources.getText2(id, *args)

        override fun getQuantityText(id: Int, quantity: Int) =
            resources.getQuantityText2(id, quantity)

        override fun getQuantityText(id: Int, quantity: Int, vararg args: Any) =
            resources.getQuantityText2(id, quantity, *args)

        override fun showToast(msg: CharSequence, duration: Int) {
            Toast.makeText(ctx, msg, duration).show()
        }

        override fun showToast(msg: Int, duration: Int) {
            Toast.makeText(ctx, msg, duration).show()
        }

        override suspend fun showSnackbar(
            message: CharSequence,
            action: CharSequence?,
            icon: Any?,
            accent: Color,
            duration: SnackbarDuration
        ): SnackbarResult {
            return channel.show(message, null,  action, icon, accent, duration)
        }

        override suspend fun showSnackbar(
            message: Int,
            action: Int,
            icon: Any?,
            accent: Color,
            duration: SnackbarDuration
        ): SnackbarResult {
            return showSnackbar(
                getText(message),
                if (action == ResourcesCompat.ID_NULL) null else getText(action),
                icon,
                accent,
                duration
            )
        }
    }