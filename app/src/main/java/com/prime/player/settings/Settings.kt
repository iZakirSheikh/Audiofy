package com.prime.player.settings

import android.annotation.SuppressLint
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.player.Material
import com.prime.player.R
import com.prime.player.audio.Type
import com.prime.player.audio.tracks.TracksRoute
import com.prime.player.common.compose.*
import com.prime.player.primary
import com.primex.preferences.LocalPreferenceStore
import com.primex.ui.*
import cz.levinzonr.saferoute.core.annotations.Route
import cz.levinzonr.saferoute.core.annotations.RouteNavGraph
import cz.levinzonr.saferoute.core.navigateTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RESERVE_PADDING = 48.dp

private const val FONT_SCALE_LOWER_BOUND = 0.5f
private const val FONT_SCALE_UPPER_BOUND = 2.0f

private const val SLIDER_STEPS = 15

@Composable
private inline fun PrefHeader(text: String) {
    val primary = MaterialTheme.colors.secondary
    val modifier =
        Modifier
            .padding(
                start = RESERVE_PADDING,
                top = ContentPadding.normal,
                end = ContentPadding.large,
                bottom = ContentPadding.medium
            )
            .fillMaxWidth()
            .drawHorizontalDivider(color = primary)
            .padding(bottom = ContentPadding.medium)
    Label(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        color = primary
    )
}

@Composable
private inline fun ColumnScope.AboutUs() {
    PrefHeader(text = "Feedback")

    // val feedbackCollector = LocalFeedbackCollector.current
    val onRequestFeedback = {
        // TODO: Handle feedback
    }

    Preference(
        title = stringResource(R.string.feedback),
        summery = stringResource(id = R.string.feedback_dialog_placeholder) + "\nTap to open feedback dialog.",
        icon = Icons.Outlined.Feedback,
        modifier = Modifier.clickable(onClick = onRequestFeedback)
    )


    val onRequestRateApp = {
        // TODO: Handle rate app.
    }
    Preference(
        title = stringResource(R.string.rate_us),
        summery = stringResource(id = R.string.review_msg),
        icon = Icons.Outlined.Star,
        modifier = Modifier.clickable(onClick = onRequestRateApp)
    )

    val onRequestShareApp = {
        // TODO: Share app.
    }
    Preference(
        title = stringResource(R.string.spread_the_word),
        summery = stringResource(R.string.spread_the_word_summery),
        icon = Icons.Outlined.Share,
        modifier = Modifier.clickable(onClick = onRequestShareApp)
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

    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }
    //val updateNotifier = LocalUpdateNotifier.current
    val scope = rememberCoroutineScope()
    val onCheckUpdate: () -> Unit = {
        scope.launch {
            //TODO: Check for update.
        }
    }
    Preference(
        title = stringResource(R.string.app_version),
        summery = "$version \nClick to check for updates.",
        icon = Icons.Outlined.TouchApp,
        modifier = Modifier.clickable(onClick = onCheckUpdate)
    )
}


@Composable
private fun TopAppBar(modifier: Modifier = Modifier) {
    val navigator = LocalNavController.current

    NeumorphicTopAppBar(
        title = { Label(text = stringResource(R.string.settings)) },
        modifier = modifier.padding(top = ContentPadding.medium),
        navigationIcon = {
            IconButton(
                onClick = {  navigator.navigateUp() },
                imageVector = Icons.Outlined.ReplyAll,
                contentDescription = null
            )
        },
        shape = CircleShape,
        elevation = ContentElevation.low,
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Route(navGraph = RouteNavGraph())
@Composable
fun Settings(viewModel: SettingsViewModel) {
    with(viewModel) {
        val topBar =
            @Composable {
                val (colorStatusBar, _, _, _) = colorStatusBar.value
                val primaryOrTransparent =
                    Material.colors.primary(colorStatusBar, Color.Transparent)
                TopAppBar(
                    modifier = Modifier
                        .statusBarsPadding2(
                            color = primaryOrTransparent,
                            darkIcons = !colorStatusBar && Material.colors.isLight
                        )
                        .drawHorizontalDivider(color = Material.colors.onSurface)
                        .padding(bottom = ContentPadding.medium)
                )
            }
        Scaffold(topBar = topBar) {
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

                PrefHeader(text = stringResource(R.string.appearance))

                //dark mode
                val darkTheme by darkUiMode
                val onCheckedChange = { new: Boolean ->
                    set(GlobalKeys.NIGHT_MODE, if (new) NightMode.YES else NightMode.NO)
                }
                SwitchPreference(
                    checked = darkTheme.value,
                    title = darkTheme.rawTitle,
                    summery = darkTheme.rawSummery,
                    icon = darkTheme.vector,
                    onCheckedChange = onCheckedChange
                )


                //font
                val font by font
                val familyList = listOf(
                    "Roboto" to FontFamily.PROVIDED,
                    "Cursive" to FontFamily.CURSIVE,
                    "San serif" to FontFamily.SAN_SERIF,
                    "serif" to FontFamily.SARIF,
                    "System default" to FontFamily.SYSTEM_DEFAULT
                )
                val onRequestChange = { family: FontFamily ->
                    viewModel.set(GlobalKeys.FONT_FAMILY, family)
                }
                DropDownPreference(
                    title = font.rawTitle,
                    entries = familyList,
                    defaultValue = font.value,
                    icon = font.vector,
                    onRequestChange = onRequestChange
                )

                // app font scale
                val scale by fontScale
                val onValueChange = { value: Float ->
                    set(GlobalKeys.FONT_SCALE, value)
                }
                SliderPreference(
                    defaultValue = scale.value,
                    title = scale.rawTitle,
                    summery = scale.rawSummery,
                    valueRange = FONT_SCALE_LOWER_BOUND..FONT_SCALE_UPPER_BOUND,
                    steps = SLIDER_STEPS,
                    icon = scale.vector,
                    onValueChange = onValueChange,
                    iconChange = Icons.Outlined.TextFormat
                )

                //force accent
                val forceAccent by forceAccent
                val onRequestForceAccent = { should: Boolean ->
                    set(GlobalKeys.FORCE_COLORIZE, should)
                    if (should)
                        set(GlobalKeys.COLOR_STATUS_BAR, true)
                }
                SwitchPreference(
                    checked = forceAccent.value,
                    title = forceAccent.rawTitle,
                    summery = forceAccent.rawSummery,
                    onCheckedChange = onRequestForceAccent
                )

                //color status bar
                val colorStatusBar by colorStatusBar
                val onRequestColorChange = { should: Boolean ->
                    set(GlobalKeys.COLOR_STATUS_BAR, should)
                }
                SwitchPreference(
                    checked = colorStatusBar.value,
                    title = colorStatusBar.rawTitle,
                    summery = colorStatusBar.rawSummery,
                    onCheckedChange = onRequestColorChange,
                    enabled = !forceAccent.value
                )

                //hide status bar
                val hideStatusBar by hideStatusBar
                val onRequestHideStatusBar = { should: Boolean ->
                    set(GlobalKeys.HIDE_STATUS_BAR, should)
                }
                SwitchPreference(
                    checked = hideStatusBar.value,
                    title = hideStatusBar.rawTitle,
                    summery = hideStatusBar.rawSummery,
                    onCheckedChange = onRequestHideStatusBar
                )

                //About us section.
                AboutUs()

                val padding = LocalWindowPadding.current
                // The necessary padding as suggested by the LocalWindowPadding
                Spacer(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(padding),
                )
            }
        }
    }
}
