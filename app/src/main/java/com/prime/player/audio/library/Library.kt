package com.prime.player.audio.library

import android.view.animation.LinearInterpolator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import com.flaviofaria.kenburnsview.RandomTransitionGenerator
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.AudioNavigationActions
import com.prime.player.extended.*
import com.prime.player.extended.managers.Banner

private val HEADER_HEIGHT = 130.dp
private val RECENT_ROW_HEIGHT = 115.dp

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Library(padding: State<PaddingValues>, viewModel: LibraryViewModel) {
    with(viewModel) {
        Scaffold(
            topBar = { TitleBar() },
            modifier = Modifier
                .fillMaxSize()
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val stateOfPager = rememberPagerState(
                    initialPage = 1,
                )

                Header(
                    text = "Collections",
                    secondaryText = "locally available collections.",
                    style = PlayerTheme.typography.h4,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(
                        top = Padding.MEDIUM,
                        start = Padding.LARGE,
                        end = Padding.LARGE
                    )
                )

                CollectionRow(
                    modifier = Modifier
                        .weight(1f),
                    stateOfPager
                )

                Banner(
                    modifier = Modifier
                        .padding(
                            vertical = Padding.MEDIUM,
                            horizontal = Padding.LARGE
                        )
                        .animate(),
                    placementID = stringResource(id = R.string.library_banner_id)
                )



                Header(
                    text = "Recents",
                    secondaryText = "Recently played tracks.",
                    style = PlayerTheme.typography.h4,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = Padding.LARGE)
                )

                RecentRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(maxHeight = RECENT_ROW_HEIGHT)
                        .padding(vertical = Padding.MEDIUM)
                        .animate()
                )

                val padding by padding
                Spacer(modifier = Modifier
                    .padding(padding)
                    .animate())

            }
        }
    }
}

@Composable
fun LibraryViewModel.TitleBar() {
    val actions = LocalNavActionProvider.current as AudioNavigationActions

    val megaArt by megaArt.collectAsState()

    val systemUI = rememberSystemUiController()
    val darkIcons = isLight() && megaArt == null
    SideEffect {
        systemUI.setStatusBarColor(color = Color.Transparent, darkIcons = darkIcons)
    }

    Crossfade(
        targetState = megaArt,
        modifier = Modifier.requiredHeight(HEADER_HEIGHT),
        animationSpec = tween(Anim.DURATION_LONG)
    ) { value ->
        if (value != null) {
            KenBurns(generator = remember(value.second) {
                RandomTransitionGenerator(value.second, LinearInterpolator())
            }, modifier = Modifier.horizontalGradient()) {
                setImageBitmap(value.first)
            }
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .padding(horizontal = Padding.LARGE)
            .requiredHeight(HEADER_HEIGHT)
            .statusBarsPadding()
            .fillMaxWidth()
    ) {
        val (title1R, title2R, subtitleR, settingsR) = createRefs()
        // chains
        createVerticalChain(title1R, title2R, subtitleR, chainStyle = ChainStyle.Packed(0f))
        createHorizontalChain(title1R, settingsR, chainStyle = ChainStyle.SpreadInside)

        val color = if (megaArt == null) PlayerTheme.colors.onBackground else Color.White
        CompositionLocalProvider(LocalContentColor provides color) {
            Header(
                text = "Audio",
                modifier = Modifier
                    .offset(y = -Padding.MEDIUM)
                    .constrainAs(title1R) {},
                style = PlayerTheme.typography.h4
            )

            IconButton(
                modifier = Modifier.constrainAs(settingsR) {
                    top.linkTo(settingsR.top)
                    bottom.linkTo(settingsR.bottom)
                },
                onClick = {
                    actions.toSettings()
                }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                )
            }

            Header(
                text = "Library",
                modifier = Modifier
                    .offset(y = -Padding.LARGE)
                    .constrainAs(title2R) {},
                fontWeight = FontWeight.Light,
                style = PlayerTheme.typography.h4
            )

            Caption(
                text = "Let's make the magic happen",
                modifier = Modifier
                    .offset(y = -Padding.LARGE)
                    .constrainAs(subtitleR) {}
            )
        }
    }
}
