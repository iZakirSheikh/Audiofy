package com.prime.player

import android.animation.ObjectAnimator
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prime.player.audio.Home
import com.prime.player.common.compose.*
import com.prime.player.core.SyncWorker
import com.prime.player.settings.GlobalKeys
import com.prime.player.settings.NightMode
import com.primex.preferences.LocalPreferenceStore
import com.primex.preferences.Preferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var fAnalytics: FirebaseAnalytics
    private lateinit var observer: ContentObserver

    @Inject
    lateinit var preferences: Preferences

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtain the FirebaseAnalytics instance.
        fAnalytics = Firebase.analytics

        // first thing first install
        // splash screen
        initSplashScreen(
            savedInstanceState == null //why?
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // trigger sync worker once change in MediaStore is detected.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) SyncWorker.schedule(this)
        else {
            observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    // run worker when change is detected.
                    if (!selfChange) SyncWorker.run(this@MainActivity)
                }
            }

            // observe Images in MediaStore.
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )

            // observe Videos in MediaStore.
            contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }

        // actual compose content.
        setContent {
            val sWindow = rememberWindowSizeClass()

            val permission =
                rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // observe the change to density
            val density = LocalDensity.current
            val fontScale by with(preferences) { get(GlobalKeys.FONT_SCALE).observeAsState() }
            val modified = Density(density = density.density, fontScale = fontScale)


            CompositionLocalProvider(
                LocalElevationOverlay provides null,
                LocalWindowSizeClass provides sWindow,
                LocalPreferenceStore provides preferences,
                LocalSystemUiController provides rememberSystemUiController(),
                LocalDensity provides modified
            ) {
                Material(isDark = resolveAppThemeState()) {
                    Crossfade(targetState = permission.status.isGranted) { has ->
                        when (has) {
                            true -> Home()
                            else -> PermissionRationale { permission.launchPermissionRequest() }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Manages SplashScreen
 */
fun MainActivity.initSplashScreen(isColdStart: Boolean) {
    // Install Splash Screen and Play animation when cold start.
    installSplashScreen().let { splashScreen ->
        // Animate entry of content
        // if cold start
        if (isColdStart)
            splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                val splashScreenView = splashScreenViewProvider.view
                // Create your custom animation.
                val alpha = ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.ALPHA,
                    1f,
                    0f
                )
                alpha.interpolator = AnticipateInterpolator()
                alpha.duration = AnimationConstants.LongDurationMills.toLong()

                // Call SplashScreenView.remove at the end of your custom animation.
                alpha.doOnEnd { splashScreenViewProvider.remove() }

                // Run your animation.
                alpha.start()
            }
    }
}


@Composable
private fun resolveAppThemeState(): Boolean {
    val preferences = LocalPreferenceStore.current
    val mode by with(preferences) {
        preferences[GlobalKeys.NIGHT_MODE].observeAsState()
    }
    return when (mode) {
        NightMode.YES -> true
        else -> false
    }
}


@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.storage_permission),
        message = stringResource(R.string.storage_permission_message),
        onActionTriggered = onRequestPermission,
        action = "ALLOW"
    )
}