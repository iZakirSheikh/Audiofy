@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.NightMode
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Banner
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowPadding
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.Range
import com.prime.media.core.compose.purchase
import com.prime.media.darkShadowColor
import com.prime.media.lightShadowColor
import com.primex.core.drawHorizontalDivider
import com.primex.core.rememberState
import com.primex.core.stringHtmlResource
import com.primex.core.stringResource
import com.primex.core.textResource
import com.primex.core.value
import com.primex.material2.DropDownPreference
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Preference
import com.primex.material2.SliderPreference
import com.primex.material2.SwitchPreference
import com.primex.material2.Text
import com.primex.material2.neumorphic.NeumorphicTopAppBar

private const val TAG = "Settings"

private val <T> Preference<T>.name inline @Composable get() = title.value
private val <T> Preference<T>.desc inline @Composable get() = summery?.value

@Composable
@NonRestartableComposable
private fun TopAppBar(
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavController.current
    NeumorphicTopAppBar(
        title = { Label(text = stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = { navigator.navigateUp() },
                imageVector = Icons.Outlined.ReplyAll,
                contentDescription = null
            )
        },
        shape = CircleShape,
        lightShadowColor = Material.colors.lightShadowColor,
        darkShadowColor = Material.colors.darkShadowColor,
        elevation = ContentElevation.low,
        modifier = modifier
            .drawHorizontalDivider(color = Material.colors.onSurface)
            .padding(vertical = ContentPadding.medium),
    )
}

private val RESERVE_PADDING = 56.dp

@Composable
private inline fun PrefHeader(text: CharSequence) {
    val primary = MaterialTheme.colors.secondary
    Label(
        text = text,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        color = primary,

        modifier = Modifier
            .padding(
                start = RESERVE_PADDING, // the amount of space taken by icon.
                top = ContentPadding.normal,
                end = ContentPadding.xLarge,
                bottom = ContentPadding.medium
            )
            .fillMaxWidth()
            .drawHorizontalDivider(color = primary)
            .padding(bottom = ContentPadding.medium),
    )
}

@Composable
private inline fun ColumnScope.Body(
    state: Settings
) {
    PrefHeader(text = stringResource(R.string.appearance))

    //Dark mode
    val provider = LocalSystemFacade.current
    val darkTheme = state.darkUiMode
    DropDownPreference(
        title = darkTheme.name,
        defaultValue = darkTheme.value,
        icon = darkTheme.vector,
        entries = listOf(
            "Dark" to NightMode.YES,
            "Light" to NightMode.NO,
            "Sync with System" to NightMode.FOLLOW_SYSTEM
        ),
        onRequestChange = {
            state.set(Settings.NIGHT_MODE, it)
            provider.showAd(force = true)
        }
    )

    val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
    if (!purchase.purchased)
        Banner(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

//    App font scale
//    val scale = state.fontScale
//    SliderPreference(
//        defaultValue = scale.value,
//        title = stringResource(value = scale.title),
//        summery = stringResource(value = scale.summery),
//        valueRange = 0.5f ..2f,
//        steps = 15,
//        icon = scale.vector,
//        iconChange = Icons.Outlined.TextFormat,
//        onValueChange = { value: Float ->
//            state.set(Settings.FONT_SCALE, value)
//            //.showAd(force = true)
//        }
//    )

    //Force accent
    val forceAccent = state.forceAccent
    SwitchPreference(
        checked = forceAccent.value,
        title = forceAccent.name,
        summery = forceAccent.desc,
        onCheckedChange = { should: Boolean ->
            state.set(Settings.FORCE_COLORIZE, should)
            if (should) state.set(Settings.COLOR_STATUS_BAR, true)
            provider.showAd(force = true)
        }
    )

    //color status bar
    val colorStatusBar = state.colorStatusBar
    SwitchPreference(
        checked = colorStatusBar.value,
        title = colorStatusBar.name,
        summery = colorStatusBar.desc,
        enabled = !forceAccent.value,
        onCheckedChange = { should: Boolean ->
            state.set(Settings.COLOR_STATUS_BAR, should)
            provider.showAd(force = true)
        }
    )

    // Hide/Show status bar
    val hideStatusBar = state.hideStatusBar
    SwitchPreference(
        checked = hideStatusBar.value,
        title = hideStatusBar.name,
        summery = hideStatusBar.desc,
        onCheckedChange = { should: Boolean ->
            state.set(Settings.HIDE_STATUS_BAR, should)
        }
    )
}

@Composable
private inline fun ColumnScope.FeedBack() {
    val facade = LocalSystemFacade.current
    Preference(
        title = stringResource(R.string.pref_feedback),
        summery = stringResource(id = R.string.pref_feedback_summery) + "\nTap to open feedback dialog.",
        icon = Icons.Outlined.Feedback,
        modifier = Modifier.clickable { facade.launchAppStore() }
    )

    Preference(
        title = stringResource(R.string.pref_rate_us),
        summery = stringResource(id = R.string.pref_review_summery),
        icon = Icons.Outlined.Star,
        modifier = Modifier.clickable { facade.launchAppStore() }
    )

    Preference(
        title = stringResource(R.string.pref_spread_the_word),
        summery = stringResource(R.string.pref_spread_the_word_summery),
        icon = Icons.Outlined.Share,
        modifier = Modifier.clickable { facade.shareApp() }
    )
}

@Composable
private inline fun ColumnScope.AboutUs() {
    val provider = LocalSystemFacade.current
    Text(
        text = stringHtmlResource(R.string.pref_about_us_summery),
        style = MaterialTheme.typography.body2,
        modifier = Modifier
            .padding(start = RESERVE_PADDING, end = ContentPadding.xLarge)
            .padding(vertical = ContentPadding.small),
        color = LocalContentColor.current.copy(ContentAlpha.medium)
    )

    // The app version and check for updates.
    val ctx = LocalContext.current
    Preference(
        title = "Privacy Policy",
        summery = "Click here to view the privacy policy.",
        icon = Icons.Outlined.PrivacyTip,
        modifier = Modifier.clickable { ctx.startActivity(PrivacyPolicyIntent) }
    )

    // The app version and check for updates.
    val version = BuildConfig.VERSION_NAME
    Preference(
        title = stringResource(R.string.app_version),
        summery = "$version \nClick to check for updates.",
        icon = Icons.Outlined.TouchApp,
        modifier = Modifier.clickable { provider.launchUpdateFlow(true) }
    )
}

context(ColumnScope)
@Composable
private inline fun General(
    state: Settings
) {
    val trashcan = state.enableTrashCan
    SwitchPreference(
        title = stringResource(value = trashcan.title),
        checked = trashcan.value,
        summery = stringResource(value = trashcan.summery),
        onCheckedChange = {
            state.set(Settings.TRASH_CAN_ENABLED, it)
        },
        icon = Icons.Outlined.Recycling
    )

    val legacyArtwork = state.fetchArtworkFromMS
    SwitchPreference(
        title = stringResource(value = legacyArtwork.title),
        checked = legacyArtwork.value,
        summery = stringResource(value = legacyArtwork.summery),
        onCheckedChange = {
            state.set(Settings.USE_LEGACY_ARTWORK_METHOD, it)
        },
        icon = Icons.Outlined.Camera
    )

    val excludeTrackDuration = state.minTrackLength
    SliderPreference(
        title = stringResource(value = excludeTrackDuration.title),
        defaultValue = excludeTrackDuration.value.toFloat(),
        summery = stringResource(value = excludeTrackDuration.summery),
        onValueChange = {
            state.set(Settings.MIN_TRACK_LENGTH_SECS, it.toInt())
        },
        valueRange = 0f..100f,
        steps = 5,
        icon = Icons.Outlined.Straighten,
        preview = {
            com.primex.material2.Text(
                text = "${excludeTrackDuration.value}s",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }

    )

    val list = state.excludedFiles
    var showBlackListDialog by rememberState(initial = false)
    // The Blacklist Dialog.
    BlacklistDialog(
        showBlackListDialog,
        state = state,
        onDismissRequest = { showBlackListDialog = false }
    )

    Preference(
        title = stringResource(value = list.title),
        summery = stringResource(value = list.summery),
        icon = Icons.Outlined.AudioFile,
        modifier = Modifier.clickable {
            showBlackListDialog = true
        }
    )

    val maxRecentSize = state.recentPlaylistLimit
    SliderPreference(
        title = stringResource(value = maxRecentSize.title),
        defaultValue = maxRecentSize.value.toFloat(),
        summery = stringResource(value = maxRecentSize.summery),
        onValueChange = {
            state.set(Settings.RECENT_PLAYLIST_LIMIT, it.toInt())
        },
        icon = Icons.Outlined.Straighten,
        valueRange = 50f..200f,
        steps = 5,
        preview = {
            com.primex.material2.Text(
                text = "${maxRecentSize.value} files",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
    )

    val useInbuiltAudioFx = state.useInbuiltAudioFx
    SwitchPreference(
        title = stringResource(value = useInbuiltAudioFx.title),
        checked = useInbuiltAudioFx.value,
        summery = stringResource(value = useInbuiltAudioFx.summery),
        onCheckedChange = {
            state.set(Settings.USE_IN_BUILT_AUDIO_FX, it)
        },
        icon = Icons.Outlined.Tune
    )

    val closePlaybackWhenTaskRemoved = state.closePlaybackWhenTaskRemoved
    SwitchPreference(
        title = stringResource(value = closePlaybackWhenTaskRemoved.title),
        checked = closePlaybackWhenTaskRemoved.value,
        summery = stringResource(value = closePlaybackWhenTaskRemoved.summery),
        onCheckedChange = {
            state.set(Settings.CLOSE_WHEN_TASK_REMOVED, it)
        },
        icon = Icons.Outlined.HideSource
    )
}

private val FeedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:helpline.prime.zs@gmail.com")
    putExtra(Intent.EXTRA_SUBJECT, "Feedback/Suggestion for Audiofy")
}

private val PrivacyPolicyIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://docs.google.com/document/d/1AWStMw3oPY8H2dmdLgZu_kRFN-A8L6PDShVuY8BAhCw/edit?usp=sharing")
}

private val GitHubIssuesPage = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy/issues")
}

private val TelegramIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://t.me/audiofy_support")
}

private val GithubIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy")
}


@Composable
private fun GetToKnowUs(modifier: Modifier = Modifier) {
    ListTile(
        modifier = modifier.offset(x = ContentPadding.normal),
        color = Color.Transparent,
        overline = {
            Text(
                text = textResource(id = R.string.app_name),
                style = Material.typography.h3,
                fontWeight = FontWeight.Bold,
                fontFamily = Settings.DancingScriptFontFamily
            )
        },
        headline = {
            Text(
                text = textResource(
                    R.string.pref_get_to_know_us_subttile_s,
                    BuildConfig.VERSION_NAME
                ),
                style = Material.typography.caption
            )
        },
        centerAlign = true,
        leading = {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Material.colors.primary,
                modifier = Modifier
                    .scale(2f)
                    .size(56.dp)
                    .offset(x = -ContentPadding.medium)
            )
        },
        subtitle = {
            Row(
                modifier = Modifier
                    .padding(top = ContentPadding.medium)
                    .offset(x = -ContentPadding.medium)
            ) {
                val ctx = LocalContext.current
                IconButton(
                    imageVector = Icons.Outlined.AlternateEmail,
                    onClick = { ctx.startActivity(FeedbackIntent) },
                    modifier = Modifier
                        .scale(0.85f)
                        .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.DataObject,
                    onClick = { ctx.startActivity(GithubIntent) },
                    modifier = Modifier
                        .scale(0.85f)
                        .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.BugReport,
                    onClick = { ctx.startActivity(GitHubIssuesPage) },
                    modifier = Modifier
                        .scale(0.85f)
                        .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.SupportAgent,
                    onClick = { ctx.startActivity(TelegramIntent) },
                    modifier = Modifier
                        .scale(0.85f)
                        .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
            }
        }
    )
}


@Composable
private fun Compact(state: Settings) {
    Scaffold(topBar = { TopAppBar(Modifier.statusBarsPadding()) }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            PrefHeader(text = textResource(R.string.pref_get_to_know_us))
            GetToKnowUs(modifier = Modifier.padding(start = ContentPadding.normal))
            Body(state)
            PrefHeader(text = textResource(R.string.general))
            General(state = state)
            PrefHeader(text = textResource(R.string.feedback))
            FeedBack()
            PrefHeader(text = textResource(R.string.about_us))
            AboutUs()
            // Add the necessary padding.
            val padding = LocalWindowPadding.current
            Spacer(
                modifier = Modifier
                    .animateContentSize()
                    .navigationBarsPadding()
                    .padding(padding),
            )
        }
    }
}

@Composable
@NonRestartableComposable
fun Settings(state: Settings) {
    val reach = LocalWindowSize.current.widthRange
    when (reach) {
        Range.Compact -> Compact(state = state)
        else -> Compact(state = state) // for every one currently
    }
}

