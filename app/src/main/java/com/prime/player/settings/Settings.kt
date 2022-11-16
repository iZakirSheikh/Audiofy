package com.prime.player.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import com.prime.player.*
import com.prime.player.BuildConfig
import com.prime.player.R
import com.prime.player.audio.Tokens
import com.prime.player.billing.*
import com.prime.player.common.FontFamily
import com.prime.player.common.NightMode
import com.prime.player.common.compose.*
import com.primex.core.drawHorizontalDivider
import com.primex.core.stringHtmlResource
import com.primex.ui.*
import cz.levinzonr.saferoute.core.annotations.Route


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


private val FontFamilyList = listOf(
    "Lato" to FontFamily.PROVIDED,
    "Cursive" to FontFamily.CURSIVE,
    "San serif" to FontFamily.SAN_SERIF,
    "serif" to FontFamily.SARIF,
    "System default" to FontFamily.SYSTEM_DEFAULT
)


@Route
@Composable
fun Settings(
    viewModel: SettingsViewModel
) {
    Scaffold(

        topBar = {
            val colorize by Material.colorStatusBar
            val bgColor = if (colorize) Material.colors.primaryVariant else Color.Transparent
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
                modifier = Modifier
                    .statusBarsPadding2(
                        color = bgColor, darkIcons = !colorize && Material.colors.isLight
                    )
                    .drawHorizontalDivider(color = Material.colors.onSurface)
                    .padding(vertical = ContentPadding.medium),
            )
        },

        content = {
            val state = rememberScrollState()
            //val color = if (MaterialTheme.colors.isLight) Color.White else Color.Black
            Column(
                modifier = Modifier
                    .padding(it)
                    //FixMe: Creates a issue between Theme changes
                    // needs to be study properly
                    // disabling for now
                    //.fadeEdge(state = state, length = 16.dp, horizontal = false, color = color)
                    .verticalScroll(state),
            ) {
                with(viewModel) {

                    val activity = LocalContext.activity

                    PrefHeader(text = stringResource(R.string.appearance))

                    //dark mode
                    val darkTheme by darkUiMode
                    SwitchPreference(checked = darkTheme.value,
                        title = stringResource(value = darkTheme.title),
                        summery = stringResource(value = darkTheme.summery),
                        icon = darkTheme.vector,
                        onCheckedChange = { new: Boolean ->
                            set(Audiofy.NIGHT_MODE, if (new) NightMode.YES else NightMode.NO)
                            activity.showAd(force = true)
                        })


                    //font
                    val font by font
                    DropDownPreference(title = stringResource(value = font.title),
                        entries = FontFamilyList,
                        defaultValue = font.value,
                        icon = font.vector,
                        onRequestChange = { family: FontFamily ->
                            viewModel.set(Audiofy.FONT_FAMILY, family)
                            activity.showAd(force = true)
                        })


                    // app font scale
                    val scale by fontScale
                    SliderPreference(defaultValue = scale.value,
                        title = stringResource(value = scale.title),
                        summery = stringResource(value = scale.summery),
                        valueRange = FontSliderRange,
                        steps = SLIDER_STEPS,
                        icon = scale.vector,
                        iconChange = Icons.Outlined.TextFormat,
                        onValueChange = { value: Float ->
                            set(Audiofy.FONT_SCALE, value)
                            activity.showAd(force = true)
                        })

                    val purchase by LocalContext.billingManager.observeAsState(id = Product.DISABLE_ADS)
                    if (!purchase.purchased) Banner(
                        placementID = Placement.BANNER_SETTINGS,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    //force accent
                    val forceAccent by forceAccent
                    SwitchPreference(checked = forceAccent.value,
                        title = stringResource(value = forceAccent.title),
                        summery = stringResource(value = forceAccent.summery),
                        onCheckedChange = { should: Boolean ->
                            set(Audiofy.FORCE_COLORIZE, should)
                            if (should) set(Audiofy.COLOR_STATUS_BAR, true)
                            activity.showAd(force = true)
                        })

                    //color status bar
                    val colorStatusBar by colorStatusBar
                    SwitchPreference(checked = colorStatusBar.value,
                        title = stringResource(value = colorStatusBar.title),
                        summery = stringResource(value = colorStatusBar.summery),
                        enabled = !forceAccent.value,
                        onCheckedChange = { should: Boolean ->
                            set(Audiofy.COLOR_STATUS_BAR, should)
                            activity.showAd(force = true)
                        })


                    //hide status bar
                    val hideStatusBar by hideStatusBar
                    SwitchPreference(checked = hideStatusBar.value,
                        title = stringResource(value = hideStatusBar.title),
                        summery = stringResource(value = hideStatusBar.summery),
                        onCheckedChange = { should: Boolean ->
                            set(Audiofy.HIDE_STATUS_BAR, should)
                            //TODO: Add statusBar Hide/Show logic.
                        })

                    //mini player progress bar
                    val showProgressInMini by showProgressInMini
                    SwitchPreference(checked = showProgressInMini.value,
                        title = stringResource(value = showProgressInMini.title),
                        summery = stringResource(value = showProgressInMini.summery),
                        onCheckedChange = { should: Boolean ->
                            set(Tokens.SHOW_MINI_PROGRESS_BAR, should)
                            activity.showAd(force = true)
                        })

                    PrefHeader(text = "Feedback")
                    val context = LocalContext.current
                    Preference(
                        title = stringResource(R.string.feedback),
                        summery = stringResource(id = R.string.feedback_dialog_placeholder) + "\nTap to open feedback dialog.",
                        icon = Icons.Outlined.Feedback,
                        modifier = Modifier.clickable(onClick = { context.launchPlayStore() })
                    )

                    Preference(title = stringResource(R.string.rate_us),
                        summery = stringResource(id = R.string.review_msg),
                        icon = Icons.Outlined.Star,
                        modifier = Modifier.clickable(onClick = { context.launchPlayStore() })
                    )

                    Preference(title = stringResource(R.string.spread_the_word),
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
                            .padding(
                                start = RESERVE_PADDING, end = ContentPadding.large
                            )
                            .padding(vertical = ContentPadding.small),
                        color = LocalContentColor.current.copy(ContentAlpha.medium)
                    )


                    // The app versiona and check for updates.
                    val version = BuildConfig.VERSION_NAME
                    val channel = LocalSnackDataChannel.current
                    Preference(title = stringResource(R.string.app_version),
                        summery = "$version \nClick to check for updates.",
                        icon = Icons.Outlined.TouchApp,
                        modifier = Modifier.clickable(onClick = {
                            activity.launchUpdateFlow(channel)
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
        })
}


private fun Context.shareApp() {
    ShareCompat.IntentBuilder(this).setType("text/plain")
        .setChooserTitle(getString(R.string.app_name))
        .setText("Let me recommend you this application ${Audiofy.GOOGLE_STORE}").startChooser()
}


private fun Context.launchPlayStore() {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.GOOGLE_STORE)).apply {
            setPackage(Audiofy.PKG_GOOGLE_PLAY_STORE)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.FALLBACK_GOOGLE_STORE)))
    }
}
