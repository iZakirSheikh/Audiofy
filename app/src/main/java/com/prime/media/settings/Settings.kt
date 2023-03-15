package com.prime.media.settings


import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import com.prime.media.*
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.core.FontFamily
import com.prime.media.core.NightMode
import com.prime.media.core.billing.Banner
import com.prime.media.core.billing.Placement
import com.prime.media.core.billing.Product
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.*
import com.prime.media.core.launchPlayStore
import com.primex.core.drawHorizontalDivider
import com.primex.core.stringHtmlResource
import com.primex.core.stringResource
import com.primex.material2.*
import com.primex.material2.neumorphic.NeumorphicTopAppBar

private val RESERVE_PADDING = 56.dp

private const val FONT_SCALE_LOWER_BOUND = 0.5f
private const val FONT_SCALE_UPPER_BOUND = 2.0f
private const val SLIDER_STEPS = 15

private val FontSliderRange = FONT_SCALE_LOWER_BOUND..FONT_SCALE_UPPER_BOUND

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
                start = RESERVE_PADDING,
                top = ContentPadding.normal,
                end = ContentPadding.large,
                bottom = ContentPadding.medium
            )
            .fillMaxWidth()
            .drawHorizontalDivider(color = primary)
            .padding(bottom = ContentPadding.medium),
    )
}


private val FontFamilyList =
    listOf(
        "Lato" to FontFamily.PROVIDED,
        "Cursive" to FontFamily.CURSIVE,
        "San serif" to FontFamily.SAN_SERIF,
        "serif" to FontFamily.SARIF,
        "System default" to FontFamily.SYSTEM_DEFAULT
    )

private fun Context.shareApp() {
    ShareCompat.IntentBuilder(this).setType("text/plain")
        .setChooserTitle(getString(R.string.app_name))
        .setText("Let me recommend you this application ${Audiofy.GOOGLE_STORE}").startChooser()
}


@Composable
private fun Layout(
    resolver: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state = rememberScrollState()
    //val color = if (MaterialTheme.colors.isLight) Color.White else Color.Black
    Column(
        modifier = modifier
            //FixMe: Creates a issue between Theme changes
            // needs to be study properly
            // disabling for now
            //.fadeEdge(state = state, length = 16.dp, horizontal = false, color = color)
            .verticalScroll(state),
    ) {

        val provider = LocalsProvider.current

        PrefHeader(text = stringResource(R.string.appearance))

        //dark mode
        val darkTheme by resolver.darkUiMode
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
                resolver.set(Audiofy.NIGHT_MODE, it)
                provider.showAd(force = true)
            }
        )

        //font
        val font by resolver.font
        DropDownPreference(title = stringResource(value = font.title),
            entries = FontFamilyList,
            defaultValue = font.value,
            icon = font.vector,
            onRequestChange = { family: FontFamily ->
                resolver.set(Audiofy.FONT_FAMILY, family)
                provider.showAd(force = true)
            }
        )


        // app font scale
        val scale by resolver.fontScale
        SliderPreference(defaultValue = scale.value,
            title = stringResource(value = scale.title),
            summery = stringResource(value = scale.summery),
            valueRange = FontSliderRange,
            steps = SLIDER_STEPS,
            icon = scale.vector,
            iconChange = Icons.Outlined.TextFormat,
            onValueChange = { value: Float ->
                resolver.set(Audiofy.FONT_SCALE, value)
                provider.showAd(force = true)
            }
        )

        val purchase by purchase(id = Product.DISABLE_ADS)
        if (!purchase.purchased) Banner(
            placementID = Placement.BANNER_SETTINGS,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        //force accent
        val forceAccent by resolver.forceAccent
        SwitchPreference(
            checked = forceAccent.value,
            title = stringResource(value = forceAccent.title),
            summery = stringResource(value = forceAccent.summery),
            onCheckedChange = { should: Boolean ->
                resolver.set(Audiofy.FORCE_COLORIZE, should)
                if (should) resolver.set(Audiofy.COLOR_STATUS_BAR, true)
                provider.showAd(force = true)
            }
        )

        //color status bar
        val colorStatusBar by resolver.colorStatusBar
        SwitchPreference(
            checked = colorStatusBar.value,
            title = stringResource(value = colorStatusBar.title),
            summery = stringResource(value = colorStatusBar.summery),
            enabled = !forceAccent.value,
            onCheckedChange = { should: Boolean ->
                resolver.set(Audiofy.COLOR_STATUS_BAR, should)
                provider.showAd(force = true)
            }
        )


        //hide status bar
        val hideStatusBar by resolver.hideStatusBar
        SwitchPreference(
            checked = hideStatusBar.value,
            title = stringResource(value = hideStatusBar.title),
            summery = stringResource(value = hideStatusBar.summery),
            onCheckedChange = { should: Boolean ->
                resolver.set(Audiofy.HIDE_STATUS_BAR, should)
                //TODO: Add statusBar Hide/Show logic.
            }
        )

        PrefHeader(text = "Feedback")
        val context = LocalContext.current
        Preference(
            title = stringResource(R.string.feedback),
            summery = stringResource(id = R.string.feedback_dialog_placeholder) + "\nTap to open feedback dialog.",
            icon = Icons.Outlined.Feedback,
            modifier = Modifier.clickable(onClick = { provider.launchAppStore() })
        )

        Preference(
            title = stringResource(R.string.rate_us),
            summery = stringResource(id = R.string.review_msg),
            icon = Icons.Outlined.Star,
            modifier = Modifier.clickable(onClick = { provider.launchAppStore() })
        )

        Preference(
            title = stringResource(R.string.spread_the_word),
            summery = stringResource(R.string.spread_the_word_summery),
            icon = Icons.Outlined.Share,
            modifier = Modifier.clickable(onClick = {
                context.shareApp()
            })
        )

        PrefHeader(text = stringResource(R.string.about_us))
        Text(
            text = stringHtmlResource(R.string.about_us_desc),
            style = MaterialTheme.typography.body2,
            modifier = Modifier
                .padding(start = RESERVE_PADDING, end = ContentPadding.large)
                .padding(vertical = ContentPadding.small),
            color = LocalContentColor.current.copy(ContentAlpha.medium)
        )

        // The app version and check for updates.
        val version = BuildConfig.VERSION_NAME
        Preference(
            title = stringResource(R.string.app_version),
            summery = "$version \nClick to check for updates.",
            icon = Icons.Outlined.TouchApp,
            modifier = Modifier.clickable(onClick = {
                provider.launchUpdateFlow()
            })
        )

        // Add the necessary padding.
        val padding = LocalWindowPadding.current
        Spacer(
            modifier = Modifier
                .animateContentSize()
                .padding(padding),
        )
    }
}

@Composable
fun Settings(
    viewModel: SettingsViewModel
) {
    Scaffold(

        topBar = {
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
                lightShadowColor = Theme.colors.lightShadowColor,
                darkShadowColor = Theme.colors.darkShadowColor,
                elevation = ContentElevation.low,
                modifier = Modifier
                    .statusBarsPadding()
                    .drawHorizontalDivider(color = Theme.colors.onSurface)
                    .padding(vertical = ContentPadding.medium),
            )
        },

        content = { Layout(resolver = viewModel, modifier = Modifier.padding(it)) }
    )
}