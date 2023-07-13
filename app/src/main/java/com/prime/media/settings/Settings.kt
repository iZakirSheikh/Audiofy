package com.prime.media.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.NightMode
import com.prime.media.core.billing.Banner
import com.prime.media.core.billing.Placement
import com.prime.media.core.billing.Product
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowPadding
import com.prime.media.core.compose.LocalWindowSizeClass
import com.prime.media.darkShadowColor
import com.prime.media.lightShadowColor
import com.prime.media.core.compose.purchase
import com.primex.core.drawHorizontalDivider
import com.primex.core.stringHtmlResource
import com.primex.core.value
import com.primex.material2.DropDownPreference
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.Preference
import com.primex.material2.SwitchPreference
import com.primex.material2.neumorphic.NeumorphicTopAppBar


private val <T> Preference<T>.name inline @Composable get() = title.value
private val <T> Preference<T>.desc inline @Composable get() = summery?.value

@Composable
fun TopAppBar(modifier: Modifier = Modifier) {
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
private inline fun PrefHeader(text: String) {
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

    val purchase by purchase(id = Product.DISABLE_ADS)
    if (!purchase.purchased)
        Banner(
            placementID = Placement.BANNER_SETTINGS,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

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
        title = stringResource(R.string.feedback),
        summery = stringResource(id = R.string.feedback_dialog_placeholder) + "\nTap to open feedback dialog.",
        icon = Icons.Outlined.Feedback,
        modifier = Modifier.clickable(onClick = { facade.launchAppStore() })
    )

    Preference(
        title = stringResource(R.string.rate_us),
        summery = stringResource(id = R.string.review_msg),
        icon = Icons.Outlined.Star,
        modifier = Modifier.clickable(onClick = { facade.launchAppStore() })
    )

    Preference(
        title = stringResource(R.string.spread_the_word),
        summery = stringResource(R.string.spread_the_word_summery),
        icon = Icons.Outlined.Share,
        modifier = Modifier.clickable(onClick = {
            facade.shareApp()
        })
    )
}

@Composable
private inline fun ColumnScope.AboutUs() {
    val provider = LocalSystemFacade.current
    Text(
        text = stringHtmlResource(R.string.about_us_desc),
        style = MaterialTheme.typography.body2,
        modifier = Modifier
            .padding(start = RESERVE_PADDING, end = ContentPadding.xLarge)
            .padding(vertical = ContentPadding.small),
        color = LocalContentColor.current.copy(ContentAlpha.medium)
    )

    // The app version and check for updates.
    val version = BuildConfig.VERSION_NAME
    Preference(
        title = stringResource(R.string.app_version),
        summery = "$version \nClick to check for updates.",
        icon = Icons.Outlined.TouchApp,
        modifier = Modifier.clickable(
            onClick = { provider.launchUpdateFlow(true) }
        )
    )
}

@Composable
private fun Compact(state: Settings, modifier: Modifier = Modifier) {
    Scaffold(topBar = { TopAppBar(Modifier.statusBarsPadding()) }) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Body(state)
            PrefHeader(text = "Feedback")
            FeedBack()
            PrefHeader(text = stringResource(R.string.about_us))
            AboutUs()
            // Add the necessary padding.
            val padding = LocalWindowPadding.current
            Spacer(
                modifier = Modifier
                    .animateContentSize()
                    .padding(padding),
            )
        }
    }
}


@Composable
@NonRestartableComposable
fun Settings(state: Settings) {
    val windowClass = LocalWindowSizeClass.current
    when (windowClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> Compact(state = state)
        else -> Compact(state = state) // for every one currently
    }
}
