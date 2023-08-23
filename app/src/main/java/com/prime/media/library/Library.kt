@file:Suppress("CrossfadeLabel", "FunctionName")

package com.prime.media.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.load
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.KenBurns
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowPadding
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.purchase
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.core.db.albumUri
import com.prime.media.core.playback.Playback
import com.prime.media.directory.GroupBy
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.playlists.Playlists
import com.prime.media.directory.store.Albums
import com.prime.media.directory.store.Artists
import com.prime.media.directory.store.Audios
import com.prime.media.directory.store.Folders
import com.prime.media.directory.store.Genres
import com.prime.media.impl.Repository
import com.prime.media.onOverlay
import com.prime.media.overlay
import com.prime.media.settings.Settings
import com.prime.media.small2
import com.primex.core.gradient
import com.primex.core.padding
import com.primex.core.rememberState
import com.primex.core.stringHtmlResource
import com.primex.core.withSpanStyle
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.Search

private const val TAG = "Library"

@Composable
private fun GridItem(
    title: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(Material.shapes.small)
            .clickable(onClick = onClick)
            .then(modifier),
        content = {
            // Place the icon.
            icon()
            // Place the label
            Label(
                text = title,
                modifier = Modifier.padding(vertical = ContentPadding.small),
                style = Material.typography.caption2,
                maxLines = 2
            )
        }
    )
}

private val SHOW_CASE_MAX_HEIGHT = 110.dp

@Composable
private inline fun <T> LazyList(
    items: List<T>?,
    modifier: Modifier = Modifier,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    // state of the list.
    val state = when {
        items == null -> 0 // loading
        items.isEmpty() -> 1 // empty.
        else -> 2
    }

    Crossfade(
        targetState = state,
        modifier = modifier.height(SHOW_CASE_MAX_HEIGHT)
    ) {
        when (it) {
            // loading
            0 -> Placeholder(
                iconResId = R.raw.lt_loading_bubbles,
                title = "",
                message = "Loading"
            )
            // empty
            1 -> Placeholder(
                iconResId = R.raw.lt_empty_box,
                title = "",
                message = "Oops!! empty."
            )
            // show list.
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = ContentPadding.large),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // this will make sure the item placed at front is visible,
                    item(contentType = "library_list_spacer") {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    // place actual items.
                    items(
                        items = items ?: emptyList(),
                        contentType = { "list_items" },
                        key = key,
                        itemContent = itemContent
                    )
                }
            }
        }
    }
}

private val TOP_BAR_HEIGHT = 140.dp
context(LazyListScope)
private fun TopBar(modifier: Modifier = Modifier) {
    item(contentType = "content_type_header") {
        val provider = LocalSystemFacade.current
        TopAppBar(
            modifier = Modifier
                .background(Material.colors.overlay)
                .requiredHeight(TOP_BAR_HEIGHT)
                .then(modifier),
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            contentColor = Material.colors.onOverlay,
            // navigation icon pointing to the about section of the app.
            // TODO - Add navigation to about us in future.
            navigationIcon = {
                IconButton(
                    onClick = { provider.launchAppStore() },
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "about us"
                )
            },
            // The library title.
            // Formatted as html resource.
            title = {
                Text(
                    text = stringHtmlResource(id = R.string.title_audio_library_html),
                    style = Material.typography.h5,
                    fontWeight = FontWeight.Light
                )
            },
            // Constitutes two actions.
            // 1. Action to buy the app full version.
            // 2. Action to navigate to settings section of the app.
            actions = {
                // Buy full version button.

                val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
                if (!purchase.purchased)
                    IconButton(
                        painter = painterResource(id = R.drawable.ic_remove_ads),
                        contentDescription = null,
                        onClick = {
                            provider.launchBillingFlow(BuildConfig.IAP_NO_ADS)
                        }
                    )
                // settings navigation button.
                val navigator = LocalNavController.current
                IconButton(
                    imageVector = Icons.TwoTone.Settings,
                    contentDescription = null,
                    onClick = {
                        val direction = Settings.route
                        navigator.navigate(direction)
                    }
                )
            }
        )
    }
}

context(LazyListScope)
private fun SideBar(modifier: Modifier = Modifier) {
    // TODO: Add in future release.
}

context(LazyListScope)
private fun Search(modifier: Modifier = Modifier) {
    item(contentType = "content_type_search") {
        //FIXMe: Consider changing this to remember savable.
        var query by rememberState(initial = "")
        val navigator = LocalNavController.current
        Search(
            query = query,
            onQueryChanged = { query = it },
            modifier = modifier,
            elevation = ContentElevation.xHigh,
            placeholder = stringResource(id = R.string.search_placeholder),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank()) {
                        val direction = Audios.direction(Audios.GET_EVERY, query = query)
                        navigator.navigate(direction)
                    }
                },
            )
        )
    }
}

@Composable
private inline fun Shortcut(
    icon: ImageVector,
    label: CharSequence,
    modifier: Modifier = Modifier,
    noinline onAction: () -> Unit
) {
    OutlinedButton2(
        onClick = onAction,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = LocalContentColor.current.copy(ContentAlpha.medium)
        ),
    ) {
        // val color = Material.colors.onSurface.copy(ContentAlpha.medium)
        Icon(imageVector = icon, contentDescription = null)
        Label(
            text = label,
            style = Material.typography.caption,
            modifier = Modifier.padding(top = ButtonDefaults.IconSpacing)
        )
    }
}

context(LazyListScope)
@OptIn(ExperimentalLayoutApi::class)
private fun Shortcuts(
    modifier: Modifier = Modifier
) {
    item(contentType = "content_type_shortcuts") {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                val navigator = LocalNavController.current
                Shortcut(
                    onAction = { navigator.navigate(Folders.direction()) },
                    icon = Icons.Outlined.Folder,
                    label = "Folders"
                )

                Shortcut(
                    onAction = { navigator.navigate(Genres.direction()) },
                    icon = Icons.Outlined.Grain,
                    label = "Genres"
                )
                Shortcut(
                    onAction = { navigator.navigate(Audios.direction(Audios.GET_EVERY)) },
                    icon = Icons.Outlined.Audiotrack,
                    label = "Audios"
                )
                Shortcut(
                    onAction = { navigator.navigate(Artists.direction()) },
                    icon = Icons.Outlined.Person,
                    label = "Artists"
                )
                Shortcut(
                    onAction = { navigator.navigate(Members.direction(Playback.PLAYLIST_FAVOURITE)) },
                    icon = Icons.Outlined.FavoriteBorder,
                    label = "Favourite"
                )

                Shortcut(
                    onAction = { navigator.navigate(Playlists.direction()) },
                    icon = Icons.Outlined.PlaylistAdd,
                    label = "Playlists"
                )
            }
        )
    }
}

context(LazyListScope)
@OptIn(ExperimentalFoundationApi::class)
private fun History(
    state: Library,
    modifier: Modifier = Modifier
) {
    item(contentType = "content_type_recents") {
        val recents by state.recent.collectAsState(initial = null)
        LazyList(
            items = recents,
            key = { it.uri },
            modifier = modifier,
            itemContent = {
                GridItem(
                    it.title,
                    onClick = {},
                    modifier = Modifier
                        .width(75.dp)
                        .animateItemPlacement(),
                    icon = {
                        Image(
                            data = it.artwork,
                            modifier = Modifier
                                .border(2.dp, Color.White, CompactDisk)
                                .shadow(ContentElevation.low, CompactDisk)
                                .size(60.dp)
                        )
                    }
                )
            },
        )
    }
}

context(LazyListScope)
@OptIn(ExperimentalMaterialApi::class)
private fun Tile(
    state: Library,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    item(contentType = "content_type_tile") {
        val id by state.carousel.collectAsState()
        // The Title row at the bottom of Tile.
        val bottomRow: @Composable RowScope.() -> Unit = {
            Icon(
                imageVector = Icons.Outlined.Album,
                contentDescription = null,
            )

            Text(
                text = "Albums".uppercase(),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = ContentPadding.normal),
                letterSpacing = 6.sp,
            )
        }
        Surface(
            modifier = modifier,
            elevation = ContentElevation.xHigh,
            shape = Material.shapes.small2,
            onClick = onClick,
            content = {
                Column {
                    // The representation image of this card or default.
                    // Make it fade on change
                    // FixMe - Currently it suffers from glitches; don't know the reason.
                    Crossfade(
                        targetState = Repository.toAlbumArtUri(
                            id ?: 0
                        ), // remove this dependency on repo.
                        animationSpec = tween(4_000),
                        modifier = Modifier.weight(1f),
                        content = { value ->
                            KenBurns(
                                modifier = Modifier.gradient(vertical = false),
                                view = {
                                    load(value)
                                }
                            )
                        }
                    )

                    // Bottom Row (Representational icon + Title )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            // .height(56.dp)
                            .padding(all = ContentPadding.normal),
                        content = bottomRow
                    )
                }
            }
        )
    }
}

context(LazyListScope)
private fun Header(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    item(contentType = "content_type_header") {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            val color = LocalContentColor.current
            Text(
                modifier = Modifier.padding(horizontal = ContentPadding.normal, vertical = 8.dp),
                style = Material.typography.h5,
                fontSize = 28.sp,
                text = buildAnnotatedString {
                    append(title)
                    if (subtitle == null)
                        return@buildAnnotatedString
                    // Subtitle
                    withSpanStyle(
                        color = color.copy(ContentAlpha.disabled),
                        fontSize = 11.sp,
                        baselineShift = BaselineShift(0.3f),
                    ) {
                        append("\n$subtitle")
                    }
                }
            )
            // early return
            if (onClick == null)
                return@Row

            IconButton(
                onClick = onClick,
                imageVector = Icons.Outlined.NavigateNext,
                contentDescription = null,
            )
        }
    }
}

context(LazyListScope)
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
fun NewlyAdded(
    state: Library,
    modifier: Modifier = Modifier
) {
    item(
        contentType = "content_type_newly_added",
        content = {
            val recents by state.newlyAdded.collectAsState(initial = null)
            LazyList(
                items = recents,
                key = { it.id },
                modifier = modifier,
            ) {
                GridItem(
                    it.name,
                    onClick = {},
                    modifier = Modifier
                        .width(75.dp)
                        .animateItemPlacement(),
                    icon = {
                        Image(
                            data = it.albumUri,
                            modifier = Modifier
                                .border(2.dp, Material.colors.onSurface, CircleShape)
                                // scale it down so that it appears distant from border.
                                .scale(0.8f)
                                .shadow(ContentElevation.low, CircleShape)
                                .size(60.dp)
                        )
                    }
                )
            }
        }
    )
}

private val OFFSET_Y_SEARCH = (-32).dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Library(
    viewModel: Library
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Material.colors.background,
        content = {
            val navigator = LocalNavController.current
            LazyColumn() {
                // The TopBar.
                // TODO: Maybe make it collapsable.
                TopBar(Modifier.statusBarsPadding())
                // Search
                // Consider adding more features.
                Search(
                    modifier = Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = 22.dp)
                        .zIndex(1f),
                )
                // Shortcuts.
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = ContentPadding.medium),
                    title = "Shortcuts",
                    subtitle = "The faster way to get things done.",
                )

                Shortcuts(
                    Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .fillMaxWidth()
                        .padding(
                            horizontal = ContentPadding.xLarge,
                            vertical = ContentPadding.small
                        )
                )
                // Recents.
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = ContentPadding.medium),
                    title = "Recent",
                    subtitle = "The recently played tracks.",
                    onClick = { navigator.navigate(Members.direction(Playback.PLAYLIST_RECENT)) }
                )

                History(
                    viewModel,
                    modifier = Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .fillMaxWidth(),
                )

                // Album Tile
                Tile(
                    viewModel,
                    onClick = { navigator.navigate(Albums.direction()) },
                    modifier = Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = ContentPadding.xLarge)
                        .aspectRatio(1.3f)
                        .fillMaxWidth()
                )
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = ContentPadding.medium),
                    title = "Recently Added",
                    subtitle = "The tracks that have been recently added",
                    onClick = {
                        navigator.navigate(
                            Audios.direction(
                                Audios.GET_EVERY,
                                order = GroupBy.DateModified,
                                ascending = false
                            )
                        )
                    }
                )

                NewlyAdded(
                    viewModel,
                    modifier = Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .fillMaxWidth(),
                )

                // Add the bottom window padding.
                item(contentType = "Bottom_padding") {
                    val inset = LocalWindowPadding.current
                    Spacer(modifier = Modifier.padding(inset))
                }
            }
        }
    )
}

