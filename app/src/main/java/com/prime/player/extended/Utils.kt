package com.prime.player.extended

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.res.Resources
import android.os.Build
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat

/**
 * A function ment to accompany composable without triggering whole composable recomposition
 */
@SuppressLint("ComposableNaming")
@Composable
@ReadOnlyComposable
fun calculate(calculation: () -> Unit) {
    calculation.invoke()
}

@ReadOnlyComposable
@Composable
fun isLight(): Boolean = MaterialTheme.colors.isLight

/**
 * The Height of the mobile display device
 */
val Density.displayHeight: Dp
    get() = Resources.getSystem().displayMetrics.heightPixels.toDp()

/**
 * The width of the mobile display device
 */
val Density.displayWidth: Dp
    get() = Resources.getSystem().displayMetrics.widthPixels.toDp()


inline fun <reified T> castTo(anything: Any): T {
    return anything as T
}


data class Quad<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val forth: D
) {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third], and [forth] values.
     */
    override fun toString(): String = "($first, $second, $third, $forth)"
}




/**
 * The source from where this install occurred.
 */
val Context.sourcePackage: String?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        else
            packageManager.getInstallerPackageName(packageName)
    }


/**
 * Utility methods if the package is present and enabled
 */
fun Context.isAppInstalled(packageName: String): Boolean {
    return try {
        val info = packageManager.getPackageInfo(packageName, GET_ACTIVITIES)
        info?.applicationInfo?.enabled ?: false
    } catch (ignored: java.lang.Exception) {
        false
    }
}


val Context.installedVersion: Long
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode
        } else {
            packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        }
    }

fun Context.checkHasPermission(permission: String): Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
