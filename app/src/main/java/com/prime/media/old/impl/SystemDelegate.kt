package com.prime.media.old.impl

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.core.content.res.ResourcesCompat
import com.primex.core.getQuantityText2
import com.primex.core.getText2
import com.zs.core_ui.toast.Duration
import com.zs.core_ui.toast.Result
import com.zs.core_ui.toast.ToastHostState

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
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Duration duration: Int = if (action == null) com.zs.core_ui.toast.Toast.DURATION_SHORT else com.zs.core_ui.toast.Toast.DURATION_INDEFINITE
    ): @Result Int

    suspend fun showSnackbar(
        @StringRes message: Int,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Duration duration: Int = if (action == ResourcesCompat.ID_NULL) com.zs.core_ui.toast.Toast.DURATION_SHORT else com.zs.core_ui.toast.Toast.DURATION_INDEFINITE
    ): @Result Int = showSnackbar(
        message = resources.getText2(message),
        action = if (action == ResourcesCompat.ID_NULL) null else resources.getText2(action),
        icon = icon,
        accent = accent,
        duration = duration
    )
}

fun SystemDelegate(ctx: Context, channel: ToastHostState) =
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
            icon: ImageVector?,
            accent: Color,
            duration: Int
        ): Int = channel.showToast(message, action,  icon, accent, duration)

    }