@file:OptIn(ExperimentalTextApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.old.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.NightMode
import com.prime.media.common.Banner
import com.prime.media.old.common.LocalNavController
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.SystemFacade
import com.prime.media.settings.ColorizationStrategy
import com.prime.media.settings.Settings
import com.prime.media.settings.SettingsViewState
import com.primex.core.plus
import com.primex.core.stringResource
import com.primex.core.textResource
import com.primex.material2.DropDownPreference
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.Preference
import com.primex.material2.SliderPreference
import com.primex.material2.SwitchPreference
import com.primex.material2.Text
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.ads.AdSize
import com.zs.core_ui.AppTheme
import com.zs.core_ui.None
import com.zs.core_ui.adaptive.contentInsets

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Constants
// Region: Preference Item Shapes - Used to style individual items within a preference section.
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
private val TopTileShape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
private val CentreTileShape = RectangleShape
private val BottomTileShape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)
private val SingleTileShape = RoundedCornerShape(24.dp)

private val com.zs.core_ui.Colors.tileBackgroundColor
    @ReadOnlyComposable @Composable get() =
        background(elevation = 1.dp)

@Composable
@NonRestartableComposable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    behavior: TopAppBarScrollBehavior? = null
) {
    LargeTopAppBar(
        title = { Label(text = textResource(id = R.string.settings)) },
        scrollBehavior = behavior,
        modifier = modifier,
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = navController::navigateUp
            )
        },
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = AppTheme.colors.background(elevation = 1.dp),
            containerColor = AppTheme.colors.background,
            scrolledContentColor = AppTheme.colors.onBackground,
            contentColor = AppTheme.colors.onBackground,
        )
    )
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun GroupHeader(
    text: CharSequence,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(ContentPadding.xLarge),
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(paddingValues)
            .then(modifier),
        color = AppTheme.colors.accent,
        style = AppTheme.typography.titleSmall
    )
}

context(ColumnScope)
@Composable
private inline fun General(
    viewState: SettingsViewState
) {
    // Enable/Disable Trash Can
    val trashcan = viewState.enableTrashCan
    val facade = LocalSystemFacade.current
    SwitchPreference(
        title = stringResource(value = trashcan.title),
        checked = trashcan.value,
        summery = stringResource(value = trashcan.summery),
        onCheckedChange = {
            viewState.set(Settings.TRASH_CAN_ENABLED, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Recycling,
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, TopTileShape),
    )

    // Legacy Artwork Method
    val legacyArtwork = viewState.fetchArtworkFromMS
    SwitchPreference(
        title = stringResource(value = legacyArtwork.title),
        checked = legacyArtwork.value,
        summery = stringResource(value = legacyArtwork.summery),
        onCheckedChange = {
            viewState.set(Settings.USE_LEGACY_ARTWORK_METHOD, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Camera,
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    // Exclude Track Duration
    // The duration from which below tracks are excluded from the library.
    val excludeTrackDuration = viewState.minTrackLength
    SliderPreference(
        title = stringResource(value = excludeTrackDuration.title),
        defaultValue = excludeTrackDuration.value.toFloat(),
        summery = stringResource(value = excludeTrackDuration.summery),
        onValueChange = {
            viewState.set(Settings.MIN_TRACK_LENGTH_SECS, it.toInt())
        },
        valueRange = 0f..100f,
        steps = 5,
        icon = Icons.Outlined.Straighten,
        preview = {
            Text(
                text = "${excludeTrackDuration.value}s",
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    val list = viewState.excludedFiles
    var showBlackListDialog by remember { mutableStateOf(false) }
    // The Blacklist Dialog.
    BlacklistDialog(
        showBlackListDialog,
        state = viewState,
        onDismissRequest = { showBlackListDialog = false; }
    )
    // The Blacklist Preference.
    // The list of files that are excluded from the library.
    Preference(
        title = stringResource(value = list.title),
        summery = stringResource(value = list.summery),
        icon = Icons.Outlined.AudioFile,
        modifier = Modifier
            .clickable {
                showBlackListDialog = true
                facade.showAd(true)
            }
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Recent Playlist Limit
    // The maximum number of recent files in the recent playlist.
   /* val maxRecentSize = viewState.recentPlaylistLimit
    SliderPreference(
        title = stringResource(value = maxRecentSize.title),
        defaultValue = maxRecentSize.value.toFloat(),
        summery = stringResource(value = maxRecentSize.summery),
        onValueChange = {
            viewState.set(Settings.RECENT_PLAYLIST_LIMIT, it.toInt())
            facade.showAd(true)
        },
        icon = Icons.Outlined.Straighten,
        valueRange = 50f..200f,
        steps = 5,
        preview = {
            Text(
                text = "${maxRecentSize.value} files",
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )*/

    // Use Inbuilt Audio FX
    // Whether to use inbuilt audio effects or inApp.
    val useInbuiltAudioFx = viewState.useInbuiltAudioFx
    SwitchPreference(
        title = stringResource(value = useInbuiltAudioFx.title),
        checked = useInbuiltAudioFx.value,
        summery = stringResource(value = useInbuiltAudioFx.summery),
        onCheckedChange = {
            viewState.set(Settings.USE_IN_BUILT_AUDIO_FX, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Tune,
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Close Playback When Task Removed
    // Whether to close the playback when the app is removed from recent tasks.
    /*val closePlaybackWhenTaskRemoved = viewState.closePlaybackWhenTaskRemoved
    SwitchPreference(
        title = stringResource(value = closePlaybackWhenTaskRemoved.title),
        checked = closePlaybackWhenTaskRemoved.value,
        summery = stringResource(value = closePlaybackWhenTaskRemoved.summery),
        onCheckedChange = {
            viewState.set(Settings.CLOSE_WHEN_TASK_REMOVED, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.HideSource,
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
    )*/
}

context(ColumnScope)
@Composable
private inline fun Appearance(
    viewState: SettingsViewState
) {
    // Night Mode Strategy
    // The strategy to use for night mode.
    val facade = LocalSystemFacade.current
    val darkTheme = viewState.darkUiMode
    DropDownPreference(
        title = stringResource(value = darkTheme.title),
        defaultValue = darkTheme.value,
        icon = darkTheme.vector,
        entries = listOf(
            "Dark" to NightMode.YES,
            "Light" to NightMode.NO,
            "Sync with System" to NightMode.FOLLOW_SYSTEM
        ),
        onRequestChange = {
            viewState.set(Settings.NIGHT_MODE, it)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, TopTileShape)
    )

    // Colorization Strategy
    // The strategy to use for colorization.
    val colorizationStrategy = viewState.colorizationStrategy
    SwitchPreference(
        checked = colorizationStrategy.value == ColorizationStrategy.Artwork,
        title = stringResource(colorizationStrategy.title),
        summery = stringResource(colorizationStrategy.summery),
        onCheckedChange = { should: Boolean ->
            val strategy =
                if (should) ColorizationStrategy.Artwork else ColorizationStrategy.Default
            viewState.set(Settings.COLORIZATION_STRATEGY, strategy)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // App font scale
    // The font scale to use for the app if -1 is used, the system font scale is used.
    val scale = viewState.fontScale
    SliderPreference(
        defaultValue = scale.value,
        title = stringResource(value = scale.title),
        summery = stringResource(value = scale.summery),
        valueRange = 0.7f..2f,
        // (2.0 - 0.7) / 0.1 = 13 steps
        steps = 13,
        icon = scale.vector,
        preview = {
            Label(
                text = if (scale.value == -1f) "System" else "%.1fx".format(scale.value),
                fontWeight = FontWeight.Bold
            )
        },
        onValueChange = { value: Float ->
            val newValue = if (value < 0.76f) -1f else value
            viewState.set(Settings.FONT_SCALE, newValue)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Grid Item Multiplier
    // The multiplier increases/decreases the size of the grid item from 0.6 to 2f
    val gridItemSizeMultiplier = viewState.gridItemSizeMultiplier
    SliderPreference(
        defaultValue = gridItemSizeMultiplier.value,
        title = stringResource(value = gridItemSizeMultiplier.title),
        summery = stringResource(value = gridItemSizeMultiplier.summery),
        valueRange = 0.6f..2f,
        // (2.0 - 0.7) / 0.1 = 13 steps
        steps = 7,
        icon = gridItemSizeMultiplier.vector,
        preview = {
            Label(
                text = "%.1fx".format(gridItemSizeMultiplier.value),
                fontWeight = FontWeight.Bold
            )
        },
        onValueChange = { value: Float ->
            viewState.set(Settings.GRID_ITEM_SIZE_MULTIPLIER, value)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Translucent System Bars
    // Whether System Bars are rendered as translucent or Transparent.
    val translucentSystemBars = viewState.translucentSystemBars
    SwitchPreference(
        checked = translucentSystemBars.value,
        title = stringResource(translucentSystemBars.title),
        summery = stringResource(translucentSystemBars.summery),
        onCheckedChange = { should: Boolean ->
            viewState.set(Settings.TRANSLUCENT_SYSTEM_BARS, should)
            viewState.set(Settings.TRANSLUCENT_SYSTEM_BARS, should)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Hide/Show SystemBars for Immersive View
    // Whether System Bars are hidden for immersive view or not.
    val immersiveView = viewState.immersiveView
    SwitchPreference(
        checked = immersiveView.value,
        title = stringResource(immersiveView.title),
        summery = stringResource(immersiveView.summery),
        onCheckedChange = { should: Boolean ->
            viewState.set(Settings.IMMERSIVE_VIEW, should)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
    )
}

context(ColumnScope)
@Composable
private inline fun AboutUs(
) {
    // About Us
    // The app version and check for updates.
    val provider = LocalSystemFacade.current
    Text(
        text = textResource(R.string.pref_about_us_summery),
        style = AppTheme.typography.bodyMedium,
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, TopTileShape)
            .padding(horizontal = ContentPadding.xLarge, vertical = ContentPadding.normal),
        color = LocalContentColor.current.copy(ContentAlpha.medium)
    )

    // Privacy Policy
    //
    val ctx = LocalContext.current
    Preference(
        title = stringResource(R.string.pref_privacy_policy),
        summery = stringResource(R.string.pref_privacy_policy_summery),
        icon = Icons.Outlined.PrivacyTip,
        modifier = Modifier
            .clickable { ctx.startActivity(Settings.PrivacyPolicyIntent) }
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    // The app version and check for updates.
    val version = BuildConfig.VERSION_NAME
    Preference(
        title = stringResource(R.string.app_version),
        summery = "$version \nClick to check for updates.",
        icon = Icons.Outlined.TouchApp,
        modifier = Modifier
            .clickable { provider.initiateUpdateFlow(true) }
            .background(AppTheme.colors.tileBackgroundColor, BottomTileShape),
    )
}

context(ColumnScope)
@Composable
private inline fun Feedback(
) {
    val facade = LocalSystemFacade.current
    Preference(
        title = stringResource(R.string.pref_feedback),
        summery = stringResource(id = R.string.pref_feedback_summery) + "\nTap to open feedback dialog.",
        icon = Icons.Outlined.Feedback,
        modifier = Modifier
            .clickable { facade.launchAppStore() }
            .background(AppTheme.colors.tileBackgroundColor, TopTileShape),
    )
    Preference(
        title = stringResource(R.string.pref_rate_us),
        summery = stringResource(id = R.string.pref_review_summery),
        icon = Icons.Outlined.Star,
        modifier = Modifier
            .clickable { facade.launchAppStore() }
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    Preference(
        title = stringResource(R.string.pref_spread_the_word),
        summery = stringResource(R.string.pref_spread_the_word_summery),
        icon = Icons.Outlined.Share,
        modifier = Modifier
            .clickable { facade.shareApp() }
            .background(AppTheme.colors.tileBackgroundColor, BottomTileShape),
    )
}

private fun SystemFacade.shareApp(){
//    TODO("Not Implemented yet!")
}

@Composable
fun Settings(
    viewState: SettingsViewState
) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val insets = WindowInsets.contentInsets
    Scaffold(
        topBar = { TopAppBar(behavior = behavior) },
        contentWindowInsets = WindowInsets.None,
        modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        content = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(insets + it)
                    .padding(
                        horizontal = ContentPadding.large,
                        vertical = ContentPadding.normal
                    ),
                content = {
                    GroupHeader(
                        text = stringResource(id = R.string.general),
                        paddingValues = PaddingValues(
                            top = ContentPadding.small,
                            bottom = ContentPadding.xLarge,
                            start = ContentPadding.xLarge,
                            end = ContentPadding.xLarge
                        )
                    )
                    General(viewState = viewState)
                    val facade = LocalSystemFacade.current
                    if (!facade.isAdFree)
                        Banner(
                            modifier = Modifier
                                .padding(vertical = ContentPadding.normal)
                                .align(Alignment.CenterHorizontally),
                            AdSize.LARGE_BANNER,
                            key = "Banner2"
                        )
                    GroupHeader(text = stringResource(id = R.string.appearance))
                    Appearance(viewState = viewState)
                    GroupHeader(text = stringResource(id = R.string.feedback))
                    Feedback()
                    GroupHeader(text = stringResource(id = R.string.about_us))
                    AboutUs()
                }
            )
        }
    )
}