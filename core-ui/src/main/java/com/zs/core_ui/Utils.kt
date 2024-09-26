package com.zs.core_ui

import android.content.Context
import android.content.pm.PackageManager
import android.view.Window
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zs.core_ui.toast.Duration
import com.zs.core_ui.toast.Toast
import android.widget.Toast as AndroidWidgetToast

/**
 * Checks if a given permission is granted for the application in the current context.
 *
 * @param permission The permission string tocheck.
 * @return `true` if the permission is granted, `false` otherwise.
 */
fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * @see isPermissionGranted
 */
fun Context.checkSelfPermissions(values: List<String>) =
    values.all { isPermissionGranted(it) }

/**
 * Shows a platform Toast message with the given text.
 *
 * This function uses the standard Android Toast class to display a short message to the user.
 *
 * @param message The text message to display in the Toast.
 * @param duration The duration of the Toast. Must be either [Toast.DURATION_SHORT] or [Toast.DURATION_LONG].
 */
fun Context.showPlatformToast(message: String, @Duration duration: Int = Toast.DURATION_SHORT) {
    // Ensure the duration is valid
    require(duration == Toast.DURATION_SHORT || duration == Toast.DURATION_LONG) {
        "Duration must be either Toast.DURATION_SHORT or Toast.DURATION_LONG"
    }
    // Create and show the Toast
    val toastDuration = if (duration == Toast.DURATION_SHORT) AndroidWidgetToast.LENGTH_SHORT else AndroidWidgetToast.LENGTH_LONG
    AndroidWidgetToast.makeText(this, message, toastDuration).show()
}

/**
 * @see showPlatformToast
 */
fun Context.showPlatformToast(
    @StringRes message: Int,
    @Duration duration: Int = Toast.DURATION_SHORT
) {
    require(duration == Toast.DURATION_SHORT || duration == Toast.DURATION_LONG) {
        "Duration must be either Toast.DURATION_SHORT or Toast.DURATION_LONG"
    }
    // Create and show the Toast
    val toastDuration = if (duration == Toast.DURATION_SHORT) AndroidWidgetToast.LENGTH_SHORT else AndroidWidgetToast.LENGTH_LONG
    AndroidWidgetToast.makeText(this, message, toastDuration).show()
}


/**
 * Controls whether both the system status bars and navigation bars have a light appearance.
 *
 * - When `true`, both the status bar and navigation bar will use a light theme (dark icons on a light background).
 * - When `false`, both will use a dark theme (light icons on a dark background).
 *
 * Setting this property adjusts both `isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars`.
 *
 * @property value `true` to apply light appearance, `false` for dark appearance.
 */
var WindowInsetsControllerCompat.isAppearanceLightSystemBars: Boolean
    set(value) {
        isAppearanceLightStatusBars = value
        isAppearanceLightNavigationBars = value
    }
    get() = isAppearanceLightStatusBars && isAppearanceLightNavigationBars

/**
 * Controls the color of both the status bar and the navigation bar.
 *
 * - When setting a value, it sets the same color for both the status bar and navigation bar.
 * - The color value is converted to an ARGB format using [Color.toArgb].
 *
 * Note: Getting the current system bar color is not supported and will throw an error if accessed.
 *
 * @property value The color to be applied to both system bars.
 * @throws UnsupportedOperationException when trying to retrieve the current system bar color.
 */
var Window.systemBarsColor: Color
    set(value) {
        statusBarColor = value.toArgb()
        navigationBarColor = value.toArgb()
    }
    get() = error("Not supported!")


