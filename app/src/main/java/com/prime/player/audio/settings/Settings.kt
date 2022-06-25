package com.prime.player.audio.settings


import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.Toolbar
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import com.prime.player.extended.managers.LocalFeedbackCollector
import com.prime.player.extended.managers.LocalReviewCollector
import com.prime.player.extended.managers.LocalUpdateNotifier
import com.prime.player.preferences.Font
import com.prime.player.preferences.NightMode
import kotlinx.coroutines.launch


private val RESERVE_PADDING = 56.dp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Settings(padding: State<PaddingValues>, viewModel: SettingsViewModel) {
    val advertiser = LocalAdvertiser.current
    with(viewModel) {
        Scaffold(
            topBar = {
                Toolbar(title = stringResource(id = R.string.settings))
            }
        ) { inner ->

            val state = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fadeEdge(state = state, length = 16.dp, horizontal = false)
                    .verticalScroll(state)
            ) {

                val primary = PlayerTheme.colors.primary

                PrefHeader(text = stringResource(R.string.appearance))


                val nightModeStrategy by nightModeStrategy.collectAsState()
                DropDownPreference(
                    title = nightModeStrategy.second, entries = listOf(
                        "Light" to NightMode.NO,
                        "Dark" to NightMode.YES,
                        "Sync With OS" to NightMode.FOLLOW_SYSTEM,
                        "Set by battery saver" to NightMode.AUTO_BATTER,
                        "Sync with time" to NightMode.AUTO_TIME
                    ),
                    defaultValue = nightModeStrategy.forth,
                    icon = nightModeStrategy.first
                ) { new ->
                    viewModel.setDefaultNightMode(new)
                    advertiser.show(true)
                }

                val primaryColor by primaryColor.collectAsState()

                ColorPickerPreference(
                    title = primaryColor.second,
                    defaultEntry = primaryColor.forth,
                    entries = Color.AppColors,
                    summery = primaryColor.third
                ) {
                    viewModel.setPrimaryColor(it)
                    advertiser.show(true)
                }

                /*MRec(
                    modifier = Modifier
                        .padding(bottom = Padding.MEDIUM)
                        .fillMaxWidth()
                        .animate()
                )*/

                val secondaryColor by viewModel.secondaryColor.collectAsState()
                ColorPickerPreference(
                    title = secondaryColor.second,
                    defaultEntry = secondaryColor.forth,
                    entries = Color.AppColors,
                    summery = secondaryColor.third
                ) {
                    viewModel.setSecondaryColor(it)
                    advertiser.show(true)
                }

                val font by font.collectAsState()
                DropDownPreference(
                    title = font.second, entries = listOf(
                        "Lato" to Font.PROVIDED,
                        "Cursive" to Font.CURSIVE,
                        "San serif" to Font.SAN_SERIF,
                        "serif" to Font.SARIF,
                        "System default" to Font.SYSTEM_DEFAULT
                    ),
                    defaultValue = font.forth,
                    icon = font.first
                ) { new ->
                    viewModel.setDefaultFont(new)
                    advertiser.show(true)
                }

                val forceUtilizeAccent by viewModel.requiresAccentThoroughly.collectAsState()

                SwitchPreference(
                    checked = forceUtilizeAccent.forth,
                    title = forceUtilizeAccent.second,
                    summery = forceUtilizeAccent.third
                ) {
                    viewModel.useAccentThoroughly(it)
                    advertiser.show(true)
                }

                val requiresColoringStatusBar by viewModel.requiresColoringStatusBar.collectAsState()
                SwitchPreference(
                    checked = requiresColoringStatusBar.forth,
                    title = requiresColoringStatusBar.second,
                    summery = requiresColoringStatusBar.third,
                    enabled = !forceUtilizeAccent.forth
                ) {
                    viewModel.colorStatusBar(it)
                    advertiser.show(true)
                }

                val hideStatusBar by hideStatusBar.collectAsState()
                SwitchPreference(
                    checked = hideStatusBar.forth,
                    title = hideStatusBar.second,
                    summery = hideStatusBar.third
                ) {
                    setHideStatusBar(it)
                    advertiser.show(true)
                }

                PrefHeader(text = "Audio Player")

                val showProgressInMiniPlayer by showProgressInMini.collectAsState()
                SwitchPreference(
                    checked = showProgressInMiniPlayer.forth,
                    title = showProgressInMiniPlayer.second,
                    summery = showProgressInMiniPlayer.third
                ) {
                    showProgressInMiniPlayer(it)
                    advertiser.show(true)
                }

               /* val microphonePermission =
                    rememberPermissionState(Manifest.permission.RECORD_AUDIO)
                val showVisualizer by showVisualizer.collectAsState()
                SwitchPreference(
                    checked = showVisualizer.forth,
                    title = showVisualizer.second,
                    summery = showVisualizer.third
                ) { show ->
                    when (show) {
                        true -> {
                            if (microphonePermission.hasPermission)
                                setShowVisualizer(true)
                            else
                                microphonePermission.launchPermissionRequest()
                        }
                        else -> setShowVisualizer(false)
                    }
                    advertiser.show(true)
                }
*/
                // about section.
                AboutUs()


                val padding by padding

                Spacer(
                    modifier = Modifier
                        .padding(padding)
                        .animate()
                )
            }
        }
    }
}

@Composable
private fun AboutUs() {

    PrefHeader(text = "About Us")

    Text(
        text = "Rhythm - Audio Player is the best Media Player for Android. With all formats supported and Stylish UI designed with Material Theme, Rhythm - Audio Player provides the best experience for you and guess what it is all free.",
        style = PlayerTheme.typography.body2,
        modifier = Modifier
            .padding(start = RESERVE_PADDING, end = Padding.LARGE)
            .padding(vertical = Padding.SMALL),
        color = LocalContentColor.current.copy(ContentAlpha.medium)
    )

    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    val updateNotifier = LocalUpdateNotifier.current
    val scope = rememberCoroutineScope()

    Preference(
        title = "App Version",
        summery = "version: $version \nClick to check for updates.",
        icon = Icons.Outlined.TouchApp,
        modifier = Modifier.clickable {
            scope.launch {
                updateNotifier.checkForUpdates(true)
            }
        }
    )

    val review = LocalReviewCollector.current

    Preference(
        title = "Rate Us",
        summery = stringResource(id = R.string.review_msg) + "\nTap to rate.",
        icon = Icons.Outlined.Star,
        modifier = Modifier.clickable {
            review.show()
        }
    )

    val feedbackCollector = LocalFeedbackCollector.current

    Preference(
        title = "Feedback",
        summery = stringResource(id = R.string.feedback_dialog_placeholder) + "\nTap to open feedback dialog.",
        icon = Icons.Outlined.Feedback,
        modifier = Modifier.clickable {
            feedbackCollector.show()
        }
    )
}

@Composable
private fun PrefHeader(text: String) {

    val primary = PlayerTheme.colors.primary
    Label(
        text = text,
        modifier = Modifier.padding(
            start = RESERVE_PADDING,
            top = Padding.LARGE,
            bottom = Padding.MEDIUM
        ),
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        color = PlayerTheme.colors.primary
    )

    Divider(
        modifier = Modifier.padding(
            start = RESERVE_PADDING,
            end = Padding.LARGE,
            bottom = Padding.MEDIUM
        ), color = primary.copy(0.12f)
    )


}