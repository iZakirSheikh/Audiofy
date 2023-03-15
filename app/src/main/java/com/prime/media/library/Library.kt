package com.prime.media.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.load
import com.prime.media.*
import com.prime.media.R
import com.prime.media.common.ContentElevation
import com.prime.media.common.ContentPadding
import com.prime.media.common.LocalNavController
import com.prime.media.common.LocalWindowPadding
import com.prime.media.core.Repository
import com.prime.media.core.albumUri
import com.prime.media.core.billing.Product
import com.prime.media.core.billing.observeAsState
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.KenBurns
import com.prime.media.core.compose.OutlinedButton2
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.key
import com.prime.media.core.launchPlayStore
import com.prime.media.core.playback.Playback
import com.prime.media.directory.GroupBy
import com.prime.media.directory.local.*
import com.prime.media.settings.Settings
import com.primex.core.gradient
import com.primex.core.padding
import com.primex.core.rememberState
import com.primex.core.stringHtmlResource
import com.primex.ui.*

private const val TAG = "Library"
private val TOP_BAR_HEIGHT = 160.dp

@Composable
@NonRestartableComposable
private fun TopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = Modifier
            .background(Theme.colors.overlay)
            .requiredHeight(TOP_BAR_HEIGHT)
            .then(modifier),
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        ///contentColor = Theme.colors.primary,

        // navigation icon pointing to the about section of the app.
        // TODO - Add navigation to about us in future.
        navigationIcon = {
            IconButton(
                onClick = { /*TODO*/ },
                painter = painterResource(id = R.drawable.ic_app),
                contentDescription = "about us",
                tint = Color.Unspecified
            )
        },

        // The library title.
        // formatted as html resource.
        title = {
            Text(
                text = stringHtmlResource(id = R.string.title_audio_library_html),
                style = Theme.typography.h5,
                fontWeight = FontWeight.Light
            )
        },

        // Constitutes two actions.
        // 1. Action to buy the app full version.
        // 2. Action to navigate to settings section of the app.
        actions = {
            // Buy full version button.
            val billing = LocalContext.billingManager
            val purchase by billing.observeAsState(id = Product.DISABLE_ADS)
            val activity = LocalContext.activity
            if (!purchase.purchased)
                IconButton(
                    painter = painterResource(id = R.drawable.ic_remove_ads),
                    contentDescription = null,
                    onClick = {
                        billing.launchBillingFlow(
                            activity, Product.DISABLE_ADS
                        )
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

@Composable
@NonRestartableComposable
private fun Search(
    modifier: Modifier = Modifier,
) {
    //FIXMe: Consider changing this to remember savable.
    var query by rememberState(initial = "")
    val navigator = LocalNavController.current
    Search(
        query = query,
        onQueryChanged = { query = it },
        modifier = modifier,
        elevation = 12.dp,
        placeholder = "Type here to search",
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

@Composable
@NonRestartableComposable
private fun Header(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = LocalContentColor.current
        Text(
            text = buildAnnotatedString {
                append(title)
                if (subtitle != null)
                    withStyle(
                        SpanStyle(
                            color = color.copy(ContentAlpha.disabled),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append("\n$subtitle")
                    }
            },
            modifier = Modifier.padding(horizontal = ContentPadding.normal, vertical = 8.dp),
            style = Theme.typography.h5,
            fontWeight = FontWeight.Light
        )

        if (onMoreClick != null)
            IconButton(
                onClick = onMoreClick,
                imageVector = Icons.Outlined.NavigateNext,
                contentDescription = null,
            )
    }
}

@Composable
private inline fun Shortcut(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    noinline onAction: () -> Unit
) {
    OutlinedButton2(
        label = label,
        onClick = onAction,
        crown = rememberVectorPainter(image = icon),
        shape = RoundedCornerShape(15),

        modifier = modifier
            .width(105.dp)
            .padding(horizontal = 3.dp, vertical = 3.dp),

        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = LocalContentColor.current
        ),
    )
}

@Composable
@NonRestartableComposable
private fun MediaStore(modifier: Modifier = Modifier) {
    Row(modifier) {
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
    }
}

@Composable
@NonRestartableComposable
private fun Playlists(modifier: Modifier = Modifier) {
    // shortcut row.
    Row(modifier = modifier) {
        val navigator = LocalNavController.current
        Shortcut(
            onAction = { navigator.navigate(Members.direction(Playback.PLAYLIST_FAVOURITE)) },
            icon = Icons.Outlined.FavoriteBorder,
            label = "Liked"
        )

        Shortcut(
            onAction = { navigator.navigate(Playlists.direction()) },
            icon = Icons.Outlined.PlaylistAdd,
            label = "Playlists"
        )
    }
}

private val CAROUSAL_SHAPE = RoundedCornerShape(4)

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Carousal(
    image: Any?,
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.padding(4.dp),
        elevation = ContentElevation.xHigh,
        shape = CAROUSAL_SHAPE,
        onClick = onClick,
    ) {
        Column {
            // The representation image of this card or default.
            // Make it fade on change
            // FixMe - Currently it suffers from glitches; don't know the reason.
            Crossfade(
                targetState = image,
                animationSpec = tween(4_000),
                modifier = Modifier.weight(1f),
                content = { value ->
                    value?.let {
                        KenBurns(
                            modifier = Modifier.gradient(vertical = false),
                            view = {
                                load(value)
                            }
                        )
                    }
                }
            )

            // title and representation vector icon.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    // TODO: Remove background from the future version of the app.
                    //This is unnecessary
                    .background(Theme.colors.surface)
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = ContentPadding.normal),

                content = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )

                    Text(
                        text = title.uppercase(),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = ContentPadding.normal),
                        letterSpacing = 6.sp,
                    )
                }
            )
        }
    }
}

@Composable
private fun Tile(
    title: String,
    image: Any?,
    error: Painter,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxHeight().aspectRatio(0.7f),
        content = {
            val color = Theme.colors.onBackground
            val shape = CircleShape

            Image(
                data = image,
                fallback = error,

                modifier = Modifier
                    .border(width = 2.dp, color = color, shape = shape)
                    //.padding(5.dp)
                    .shadow(ContentElevation.low, shape, clip = true)
                    // .clip(shape)
                    .size(60.dp)
            )

            Label(
                text = title,
                modifier = Modifier.padding(vertical = ContentPadding.small),
                style = Theme.typography.caption2,
                color = color,
                maxLines = 2
            )
        },
    )
}

private val SHOW_CASE_MAX_HEIGHT = 110.dp

@Composable
private inline fun <T> List(
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
                    contentPadding = PaddingValues(horizontal = ContentPadding.normal),
                ) {

                    // this will make sure the
                    // scroll
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

@Composable
private fun RateUs(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Theme.colors.overlay) {
        Column(
            Modifier.padding(horizontal = ContentPadding.large, vertical = ContentPadding.normal),
        ) {
            // top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Amber
                )
                Header(
                    text = stringResource(id = R.string.rate_us),
                    modifier = Modifier.padding(start = ContentPadding.normal)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { /*TODO*/ },
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null
                )
            }
            // message
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.review_msg),
                style = Theme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
            // button
            OutlinedButton(
                label = stringResource(id = R.string.rate_us),
                onClick = { context.launchPlayStore() },
                border = ButtonDefaults.outlinedBorder,
                modifier = Modifier.padding(top = ContentPadding.normal),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
            )
        }
    }
}

private val OFFSET_Y_SEARCH = (-23).dp
private const val CONTENT_TYPE_HEADER = "Header"
private const val CONTENT_TYPE_SHOW_CASE = "_show_case"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Library(viewModel: LibraryViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Theme.colors.background
    ) {
        val navigator = LocalNavController.current
        val fallback = painterResource(id = R.drawable.default_art)
        LazyColumn() {

            // The TopBar.
            // TODO: Maybe make it collapsable.
            item(contentType = "TopBar") {
                TopBar(
                    modifier = Modifier.statusBarsPadding()
                )
            }

            // Search
            // Consider adding more features.
            item(contentType = "Search_view") {
                Search(
                    modifier = Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = 22.dp)
                        .zIndex(1f),
                )
            }

            // The shortcuts.
            item(contentType = CONTENT_TYPE_HEADER) {
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = OFFSET_Y_SEARCH)
                        .padding(horizontal = ContentPadding.medium),
                    title = "Shortcuts",
                    subtitle = "The faster way to get things done.",
                )
            }

            //MediaStore shortcuts
            item(contentType = "MediaStore") {
                MediaStore(
                    Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .fillMaxWidth()
                        // maybe use lazy row
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            horizontal = ContentPadding.normal,
                            vertical = ContentPadding.small
                        )
                )
            }

            // Playlist shortcuts:
            // Maybe allow user to shortcut to playlist here.
            item(contentType = "Playlists") {
                Playlists(
                    Modifier
                        .offset(y = OFFSET_Y_SEARCH)
                        .fillMaxWidth()
                        // maybe use lazy row
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            horizontal = ContentPadding.normal,
                            vertical = ContentPadding.small
                        )
                )
            }

            //RateUs Banner
            // Maybe allow the use to close the rate_us dialog
            item(contentType = "RateUs_Banner") {
                RateUs(
                    modifier = Modifier
                        .padding(vertical = ContentPadding.medium)
                        .fillMaxWidth()
                )
            }

            // Resents
            //FixMe - Allow play on click or something.
            item(contentType = CONTENT_TYPE_HEADER) {
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentPadding.normal, vertical = 8.dp),
                    title = "Recent",
                    subtitle = "The recently played tracks.",
                ) {
                    navigator.navigate(Members.direction(Playback.PLAYLIST_RECENT))
                }
            }

            item(contentType = CONTENT_TYPE_SHOW_CASE) {
                val recents by viewModel.recent.collectAsState(initial = null)
                List(items = recents, key = { it.key }) {
                    Tile(
                        image = it.artwork,
                        modifier = Modifier
                            .clip(Theme.shapes.small)
                            // TODO: Play on click
                            .clickable { }
                            .animateItemPlacement()
                            .padding(4.dp),
                        error = fallback,
                        title = it.title
                    )
                }
            }

            //Carousel
            // FixMe - Add proper carousal support with other destinations as well.
            item(contentType = "carousal") {
                val photo by viewModel.carousel.collectAsState()
                Carousal(
                    image = Repository.toAlbumArtUri(photo ?: 0),
                    title = "Albums",
                    icon = Icons.Outlined.Album,
                    modifier = Modifier
                        // .padding(start = ContentPadding.normal)
                        .padding(
                            horizontal = ContentPadding.normal,
                            vertical = 10.dp
                        )
                        .aspectRatio(1.2f)
                        .fillMaxWidth()
                ) {
                    val direction = Albums.direction()
                    navigator.navigate(direction)
                }
            }

            //Newly Added
            // Resents
            //FixMe - Allow play on click or something.
            item(contentType = CONTENT_TYPE_HEADER) {
                Header(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ContentPadding.normal, vertical = 8.dp),
                    title = "Recently Added",
                    subtitle = "The tracks that have been recently added",
                ) {
                    navigator.navigate(
                        Audios.direction(Audios.GET_EVERY, order = GroupBy.DateModified, ascending = false)
                    )
                }
            }

            item(contentType = CONTENT_TYPE_SHOW_CASE) {
                val recents by viewModel.newlyAdded.collectAsState(initial = null)
                List(items = recents, key = { it.key }) {
                    Tile(
                        image = it.albumUri,
                        modifier = Modifier
                            .clip(Theme.shapes.small)
                            // TODO: Play on click
                            .clickable { }
                            .animateItemPlacement()
                            .padding(4.dp),
                        error = fallback,
                        title = it.name
                    )
                }
            }

            item(contentType = "Bottom_padding") {
                //The placer space.
                val inset = LocalWindowPadding.current
                Spacer(modifier = Modifier.padding(inset))
            }
        }
    }
}