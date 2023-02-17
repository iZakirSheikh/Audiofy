package com.prime.player.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.load
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.ContentElevation
import com.prime.player.common.ContentPadding
import com.prime.player.common.LocalNavController
import com.prime.player.common.LocalWindowPadding
import com.prime.player.core.Repository
import com.prime.player.core.billing.Product
import com.prime.player.core.billing.observeAsState
import com.prime.player.core.billing.purchased
import com.prime.player.core.compose.Image
import com.prime.player.core.compose.KenBurns
import com.prime.player.core.compose.OutlinedButton2
import com.prime.player.core.compose.Placeholder
import com.prime.player.core.db.Playlist
import com.prime.player.core.playback.Playback
import com.prime.player.directory.local.*
import com.prime.player.settings.Settings
import com.primex.core.gradient
import com.primex.core.rememberState
import com.primex.core.stringHtmlResource
import com.primex.ui.*

@Composable
fun Header(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        elevation = 0.dp,
        backgroundColor = Theme.colors.background,

        // navigation icon pointing to the about section of the app.
        // TODO - Add navigation to about us in future.
        navigationIcon = {
            IconButton(
                onClick = { /*TODO*/ },
                painter = painterResource(id = R.drawable.ic_app),
                contentDescription = "about us"
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

private val MIAN_CARD_SHAPE = RoundedCornerShape(4)

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainCard(
    image: Any?,
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.padding(4.dp),
        elevation = ContentElevation.xHigh,
        shape = MIAN_CARD_SHAPE,
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
private fun Recent(
    title: String,
    image: Any?,
    error: Painter,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(70.dp),

        content = {
            val color = Theme.colors.onBackground
            val shape = CircleShape

            Image(
                data = image,
                fallback = error,
                modifier = Modifier
                    .border(width = 3.dp, color = color, shape = shape)
                    .padding(5.dp)
                    .shadow(ContentElevation.low, shape, clip = false)
                    .clip(shape)
                    .requiredSize(51.dp)
            )

            Label(
                text = title,
                modifier = Modifier.padding(vertical = ContentPadding.small),
                style = Theme.typography.caption2,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        },
    )
}

private val RECENT_MAX_HEIGHT = 100.dp
private val RecentArtworkSize = 56.dp

// The list of recent items.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Recents(
    list: List<Playlist.Member>?, // TODO: Make it a general list
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = list.isNullOrEmpty(),
        modifier = modifier.height(RECENT_MAX_HEIGHT)
    ) { state ->
        when (state) {

            // emit placeholder
            // with appropriate message
            true -> Placeholder(
                iconResId = R.raw.lt_empty_box,
                title = "",
                message = "Recently played tracks will be appear here!."
            )

            // emit the recent RecycleView

            // FixMe: Find the procedure to correctly use LazyList
            //As using recycler view is not appropriate in compose.
            else -> {
                val fallback = painterResource(id = R.drawable.default_art)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = ContentPadding.normal),
                ) {

                    // this will make sure the
                    // scroll
                    item() {
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    items(list ?: emptyList(), key = { it.id }) {
                        Recent(
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
            }
        }
    }
}

@Composable
private inline fun VertButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    noinline onClick: () -> Unit
) {
    OutlinedButton2(
        label = label,
        onClick = onClick,
        modifier = modifier.padding(horizontal = 3.dp),
        crown = rememberVectorPainter(
            image = icon
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent
        ),
        shape = RoundedCornerShape(15),
    )
}

/**
 * The shortcut of Android [MediaStore]
 */
@Composable
private fun MediaStore(modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            // may the user wants to add the padding.
            .then(modifier),
    ) {

        val navigator = LocalNavController.current
        VertButton(
            onClick = {
                val direction = Genres.direction()
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.Grain,
            label = "Genres"
        )

        VertButton(
            onClick = {
                val direction = Artists.direction()
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.Person,
            label = "Artists"
        )

        VertButton(
            onClick = {
                val direction = Members.direction(Playback.PLAYLIST_FAVOURITE)
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.HeartBroken,
            label = "Favourite"
        )

        VertButton(
            onClick = {
                val direction = Audios.direction(Audios.GET_EVERY)
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.Audiotrack,
            label = "Audios"
        )

        VertButton(
            onClick = {
                val direction = Playlists.direction()
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.PlaylistAdd,
            label = "Playlist"
        )

        VertButton(
            onClick = {
                val direction = Folders.direction()
                navigator.navigate(direction)
            },
            icon = Icons.Outlined.Folder,
            label = "Folders"
        )
    }
}

@Composable
fun Library(viewModel: LibraryViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // TODO: Maybe replace this with lazyColumn in future.
            .verticalScroll(rememberScrollState())
            .background(color = Theme.colors.background),
    ) {

        // The TopBar.
        // TODO: Maybe make it collapsable.
        Header(
            modifier = Modifier.statusBarsPadding()
        )

        // Search
        val navigator = LocalNavController.current
        var query by rememberState(initial = "")
        Search(
            query = query,
            onQueryChanged = { query = it },
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 5.dp)
                .zIndex(1f),
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

        //header
        // TODO - Add support for navigate to recents.
        Header(
            text = "Recent",
            modifier = Modifier.padding(horizontal = ContentPadding.normal, vertical = 8.dp),
            style = Theme.typography.h4,
            fontWeight = FontWeight.Light
        )

        //
        val recent by viewModel.recent
        Recents(
            recent,
            modifier = Modifier.fillMaxWidth(),
        )


        //header
        var showMore by rememberState(true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding.normal, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Header(
                text = "Categories",
                style = Theme.typography.h4,
                fontWeight = FontWeight.Light
            )

            val rotate by animateFloatAsState(targetValue = if (showMore) 180f else 0f)
            IconButton(
                onClick = { showMore = !showMore },
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    this.rotationZ = rotate
                }
            )
        }

        AnimatedVisibility(visible = showMore) {
            MediaStore(
                Modifier.padding(horizontal = ContentPadding.normal, vertical = 8.dp)
            )
        }

        //Carousel
        // TODO - Add proper carousal support with other destinations as well.
        val photo by viewModel.carousel.collectAsState()
        MainCard(
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

        //The placer space.
        val inset = LocalWindowPadding.current
        Spacer(modifier = Modifier.padding(inset))
    }
}