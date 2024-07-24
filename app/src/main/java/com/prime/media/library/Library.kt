@file:Suppress("CrossfadeLabel", "FunctionName")

package com.prime.media.library


import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Colors
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.about.AboutUs
import com.prime.media.caption2
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.Banner
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.None
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.purchase
import com.prime.media.core.compose.shape.FolderShape
import com.prime.media.core.compose.shimmer.pulsate
import com.prime.media.core.db.albumUri
import com.prime.media.core.playback.Playback
import com.prime.media.directory.GroupBy
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.store.Albums
import com.prime.media.directory.store.Artists
import com.prime.media.directory.store.Audios
import com.prime.media.directory.store.Genres
import com.prime.media.impl.Repository
import com.prime.media.small2
import com.primex.core.ImageBrush
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.lerp
import com.primex.core.textResource
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior

/**
 * A Composable function that lays out a screen with a top bar, content, and details section.
 *
 * @param topBar A Composable function that represents the top bar.
 * @param content A Composable function that represents the main content of the screen.
 * @param details A Composable function that represents the details section of the screen.
 * @param modifier The modifier to be applied to the layout.
 * @param offset The offset of the content from the top of the screen.
 */
// TODO - Here Scaffold is movable; consider find ways to animate its change
@Composable
private inline fun Layout(
    noinline topBar: @Composable () -> Unit,
    crossinline content: @Composable ColumnScope.() -> Unit,
    crossinline details: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    offset: Dp = Dp.Unspecified
) {
    val isTwoPane = offset != Dp.Unspecified
    when (isTwoPane) {
        false -> Scaffold(
            topBar = topBar,
            modifier = modifier,
            content = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(it)
                        .fillMaxSize(),
                    content = {
                        details()
                        content()
                    }
                )
            }
        )

        else -> TwoPane(
            second = { details() },
            // TODO - think about the gapWidth here.
            strategy = HorizontalTwoPaneStrategy(offset, false, ContentPadding.medium),
            displayFeatures = emptyList(),
            modifier = modifier,
            first = {
                Scaffold(
                    topBar = topBar,
                    modifier = modifier,
                    content = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(it)
                                .fillMaxSize(),
                            content = { content() }
                        )
                    }
                )
            },

            )
    }
}

private val DefaultContentPadding =
    PaddingValues(ContentPadding.normal, ContentPadding.medium, ContentPadding.normal)

private val Colors.topBar
    @Composable inline get() = primary.blend(background, 0.96f)

private val Colors.border
    @Composable inline get() = BorderStroke(0.2.dp, primary.copy(0.3f))

/**
 * Creates a composable header with a title, optional subtitle, and an optional "More" button.
 *
 * @param title The main text of the header.
 * @param subtitle An optional secondary text below the title.
 * @param modifier Additional modifiers to apply to the header layout.
 * @param style The text style to use for the title. Defaults to `Material.typography.h5`.
 * @param onMoreClick An optional callback to be executed when the "More" button is clicked.
 * @param contentPadding The padding to apply around the content of the header.
 */
@Composable
private fun Header(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = Material.typography.h5,
    onMoreClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Row(
        modifier = modifier.then(Modifier.padding(contentPadding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            style = style,
            text = text
        )

        // More button (conditionally displayed)
        if (onMoreClick != null) {
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                onClick = onMoreClick,
                modifier = Modifier
                    .scale(0.80f)
                    .border(Material.colors.border, shape = CircleShape),
                tint = Material.colors.primary
            )
        }
    }
}


@Composable
fun Library(state: Library) {
    val (width, height) = LocalWindowSize.current.value
    val isTwoPane = width > 700.dp
    // Define the scrollBehaviour to be used in topAppBar.
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Layout(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        // if this is a two-pane layout, the details will take 25% of total width (360dp - 500dp)
        offset = if (!isTwoPane) Dp.Unspecified else (width * 0.25f).coerceIn(300.dp, 380.dp),
        topBar = {
            // means the top_bar will fill entire width when small width display
            val immersive = width < 500.dp
            CarousalAppBar(
                state = state,
                behaviour = topAppBarScrollBehavior,
                modifier = if (immersive) Modifier
                else Modifier
                    .padding(DefaultContentPadding)
                    .statusBarsPadding()
                    .clip(RoundedCornerShape(15)),
                insets = if (immersive) WindowInsets.statusBars else WindowInsets.None
            )
        },
        content = {
            // Resents.
            val navigator = LocalNavController.current
            Header(
                modifier = Modifier.fillMaxWidth(),
                text = textResource(R.string.library_history),
                onMoreClick = { navigator.navigate(Members.direction(Playback.PLAYLIST_RECENT)) },
                contentPadding = DefaultContentPadding
            )
            // FixMe -  Can't use indented padding here because the anchor (i.e ZeroWidthSpace) in
            //  StateFulLazyList is casing issues
            RecentlyPlayedList(
                state,
                modifier = Modifier.fillMaxWidth(),
                // As spacing will be suffice for start padding.
                contentPadding = DefaultContentPadding
            )

            // Show Banner if not AdFree.
            val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
            if (!purchase.purchased)
                Banner(
                    modifier = Modifier.padding(DefaultContentPadding),
                    key = "Banner_1"
                )

            // Newly Added
            Header(
                modifier = Modifier.fillMaxWidth(),
                text = textResource(id = R.string.library_recently_added),
                onMoreClick = {
                    navigator.navigate(
                        Audios.direction(
                            Audios.GET_EVERY,
                            order = GroupBy.DateModified,
                            ascending = false
                        )
                    )
                },
                contentPadding = DefaultContentPadding
            )
            NewlyAddedList(
                state,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = DefaultContentPadding
            )
        },
        details = {
            val content = remember {
                movableContentOf {
                    Header(
                        modifier = Modifier.takeIf { !isTwoPane }
                            ?: Modifier
                                .background(Material.colors.topBar)
                                .fillMaxWidth(),
                        text = textResource(R.string.library_shortcuts),
                        style = if (isTwoPane) Material.typography.subtitle1 else Material.typography.h5,
                        contentPadding = if (isTwoPane) PaddingValues(
                            horizontal = ContentPadding.normal,
                            vertical = ContentPadding.small
                        ) else DefaultContentPadding
                    )
                    Shortcuts(
                        Modifier
                            .padding(horizontal = 28.dp, vertical = ContentPadding.medium)
                            .fillMaxWidth(),
                    )
                }
            }
            when (isTwoPane) {
                // Why?
                //  Because it will be placed inside the column of main content.
                false -> content()

                else -> Surface(
                    modifier = Modifier
                        .systemBarsPadding()
                        .padding(top = ContentPadding.medium, end = ContentPadding.normal),
                    // Use the outline color as the border stroke or null based on the lightness
                    // of the material colors
                    border = Material.colors.border,
                    // Use the overlay color or the background color based on the lightness of
                    // the material colors
                    color = Color.Transparent,
                    // Use the ContentShape as the shape of the surface
                    shape = Material.shapes.small2,
                    contentColor = Material.colors.onBackground,
                    content = {
                        Column(content = { content() })
                    },
                )
            }
        }
    )
}