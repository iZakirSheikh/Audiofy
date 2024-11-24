@file:Suppress("CrossfadeLabel", "FunctionName")

package com.prime.media.old.library


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.prime.media.R
import com.prime.media.common.Banner
import com.prime.media.common.LocalSystemFacade
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.GroupBy
import com.prime.media.old.directory.playlists.Members
import com.prime.media.old.directory.store.Audios
import com.prime.media.personalize.RoutePersonalize
import com.primex.core.blend
import com.primex.core.textResource
import com.primex.material2.DropDownMenuItem
import com.primex.material2.IconButton
import com.primex.material2.Text
import com.primex.material2.appbar.TopAppBarDefaults
import com.zs.core.playback.Playback
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.None
import com.zs.core_ui.adaptive.contentInsets

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
                        .padding(WindowInsets.contentInsets)
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
                                .padding(WindowInsets.contentInsets)
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

private val com.zs.core_ui.Colors.topBar
    @Composable inline get() = accent.blend(background, 0.96f)

private val com.zs.core_ui.Colors.border
    @Composable inline get() = BorderStroke(0.2.dp, accent.copy(0.3f))

/**
 * Creates a composable header with a title, optional subtitle, and an optional "More" button.
 *
 * @param title The main text of the header.
 * @param subtitle An optional secondary text below the title.
 * @param modifier Additional modifiers to apply to the header layout.
 * @param style The text style to use for the title. Defaults to `com.zs.core_ui.AppTheme.typography.headlineSmall`.
 * @param onMoreClick An optional callback to be executed when the "More" button is clicked.
 * @param contentPadding The padding to apply around the content of the header.
 */
@Composable
private fun Header(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.headlineSmall,
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
                    .border(AppTheme.colors.border, shape = CircleShape),
                tint = AppTheme.colors.accent
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
            // Personalize
            val navigator = LocalNavController.current
            DropDownMenuItem(
                textResource(R.string.scr_personalize_title_desc),
                icon = rememberVectorPainter(Icons.Outlined.Palette),
                onClick = {
                    navigator.navigate(RoutePersonalize())
                },
                modifier = Modifier.padding(vertical = ContentPadding.medium)
            )

            // Promotions
            Promotions(
                modifier = Modifier.padding(DefaultContentPadding).fillMaxWidth(),
            )

            // Resents.
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
            val facade = LocalSystemFacade.current
            if (!facade.isAdFree)
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
                                .background(AppTheme.colors.topBar)
                                .fillMaxWidth(),
                        text = textResource(R.string.library_shortcuts),
                        style = if (isTwoPane) AppTheme.typography.titleMedium else AppTheme.typography.headlineSmall,
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
                    border = AppTheme.colors.border,
                    // Use the overlay color or the background color based on the lightness of
                    // the material colors
                    color = Color.Transparent,
                    // Use the ContentShape as the shape of the surface
                    shape = AppTheme.shapes.compact,
                    contentColor = AppTheme.colors.onBackground,
                    content = {
                        Column(content = { content() })
                    },
                )
            }
        }
    )
}