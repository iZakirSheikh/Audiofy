package com.prime.player

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prime.player.audio.Home
import com.prime.player.audio.HomeViewModel
import com.prime.player.extended.LocalSystemUiController
import com.prime.player.extended.managers.*
import com.prime.player.extended.memorize
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.hideStatusBar
import com.prime.player.preferences.isCurrentThemeDark
import com.prime.player.preferences.isNewInstall
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var fAnalytics: FirebaseAnalytics
    private lateinit var advertiser: Advertiser
    private lateinit var updateNotifier: UpdateNotifier
    private lateinit var reviewManager: ReviewManager

    // The update notifier
    private var update by mutableStateOf<Update?>(null)

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fAnalytics = Firebase.analytics
        // init ads sdk
        UnityAds.initialize(
            this as Context,
            getString(R.string.unity_app_id),
            App.DEBUG,
            true,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    fAnalytics.logEvent("advertiser_initialized_successfully", null)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    fAnalytics.logEvent("advertiser_initialization_error:${error.name}", null)
                }
            }
        )
        advertiser = Advertiser(context = this)
        // The update notifier
        updateNotifier = UpdateNotifier(
            context = this,
            listener = object : IUpdate {
                override fun onUpdateAvailable(new: Update) {
                    update = new
                }

                override fun otherwise(msg: String) {
                    // currently show toast
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
        // The review Manager
        reviewManager = ReviewManager(
            context = this,
            priority = listOf(
                Market.GOOGLE,
                Market.SAMSUNG,
                Market.AMAZON
            ),
        )

        val prefs = Preferences.get(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val hideStatusBar by with(prefs) { hideStatusBar().collectAsState() }
            window.toggleStatusBar(hideStatusBar)

            val isNewInstall by with(prefs) { isNewInstall().collectAsState() }
            val uiController = rememberSystemUiController()

            // content
            PlayerTheme(darkTheme = isCurrentThemeDark()) {
                ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                    ProvideGlobals {
                        Crossfade(
                            targetState = isNewInstall,
                            modifier = Modifier.navigationBarsPadding()
                        ) { value ->
                            when (value) {
                                true -> Intro()
                                else -> {
                                    CompositionLocalProvider(
                                        LocalElevationOverlay provides null,
                                        LocalSystemUiController provides uiController,
                                    ) {
                                        val viewModel = hiltViewModel<HomeViewModel>()
                                        Home(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        advertiser.show(false)
    }

    @Composable
    private fun ProvideGlobals(content: @Composable () -> Unit) {
        // on update
        update?.let {
            updateNotifier.Dialog(update = it) {
                update = null
            }
        }

        // feedback
        // The feedback dialog state
        val feedbackDialog = memorize {
            if (update == null)
                Feedback {
                    hide()
                }
        }

        val reviewDialog = memorize {
            CompositionLocalProvider(LocalFeedbackCollector provides feedbackDialog) {
                if (update == null)
                    reviewManager.Dialog {
                        hide()
                    }
            }
        }


        // init update and review check after some delay
        LaunchedEffect(key1 = Unit) {
            // after app is started delay for some time.
            delay(5000)
            updateNotifier.checkForUpdates(false)
            // maybe show rating dialog
            val show = reviewManager.showRatingDialog()
            if (show)
                reviewDialog.show()
        }

        CompositionLocalProvider(
            LocalUpdateNotifier provides updateNotifier,
            LocalAdvertiser provides advertiser,
            LocalReviewCollector provides reviewDialog,
            LocalFeedbackCollector provides feedbackDialog
        ) {
            content()
        }
    }
}

private fun Window.toggleStatusBar(hide: Boolean) {
    when (hide) {
        // Hide Status Bar.
        true -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                insetsController?.hide(WindowInsets.Type.statusBars())
            else
                addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                insetsController?.show(WindowInsets.Type.statusBars())
            else
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}