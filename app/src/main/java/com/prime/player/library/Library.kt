package com.prime.player.library

import android.view.animation.LinearInterpolator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.flaviofaria.kenburnsview.RandomTransitionGenerator
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.core.Repository
import com.prime.player.core.albumUri
import com.prime.player.core.billing.Product
import com.prime.player.core.billing.observeAsState
import com.prime.player.core.billing.purchased
import com.prime.player.core.compose.Image
import com.prime.player.core.compose.KenBurns
import com.prime.player.core.compose.Placeholder
import com.prime.player.core.db.Audio
import com.prime.player.core.playback.Playback
import com.prime.player.core.uri
import com.prime.player.directory.Type
import com.prime.player.directory.buckets.Buckets
import com.prime.player.directory.tracks.Tracks
import com.prime.player.settings.Settings
import com.primex.core.*
import com.primex.ui.*
import com.primex.ui.views.Recycler

private val REEL_HEIGHT = 100.dp
context(LibraryViewModel) @Composable
private fun Reel(modifier: Modifier = Modifier) {
    // observe the reel
    val reel by reel
    val bg = Theme.colors.overlay
    val contentColor = if (reel != null) Color.White else Theme.colors.onSurface
    Surface(
        color = bg,
        contentColor = contentColor,

        modifier = modifier
            .height(REEL_HEIGHT)
            .fillMaxWidth(),

        content = {

            // The actual content
            // Place reel and fade on even new one
            Crossfade(targetState = reel, content = { value ->
                if (value != null) {
                    val (duration, bitmap) = value
                    // the reel as ken_burns view
                    KenBurns(
                        modifier = Modifier.gradient(vertical = false),
                        view = { setImageBitmap(bitmap) },

                        generator = remember(duration) {
                            RandomTransitionGenerator(
                                duration, LinearInterpolator()
                            )
                        },
                    )
                }
            })

            //Row of icon nad title
            Row(

                modifier = Modifier
                    .padding(horizontal = ContentPadding.normal)
                    .statusBarsPadding(),

                content = {
                    // the title
                    Text(
                        text = stringHtmlResource(id = R.string.title_audio_library_html),
                        style = Theme.typography.h5,
                        fontWeight = FontWeight.Light
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    val billing = LocalContext.billingManager
                    val purchase by billing.observeAsState(id = Product.DISABLE_ADS)
                    val activity = LocalContext.activity
                    if (!purchase.purchased) IconButton(painter = painterResource(id = R.drawable.ic_remove_ads),
                        contentDescription = null,
                        onClick = {
                            billing.launchBillingFlow(
                                activity, Product.DISABLE_ADS
                            )
                        })


                    // the icon
                    val navigator = LocalNavController.current
                    IconButton(imageVector = Icons.TwoTone.Settings,
                        contentDescription = null,
                        onClick = {
                            val direction = Settings.route
                            navigator.navigate(direction)
                        })
                },
            )
        },
    )
}

private val CarousalShape = RoundedCornerShape(6)
context(LibraryViewModel) @OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Carousal(
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavController.current
    val activity = LocalContext.activity
    Surface(modifier = modifier,
        elevation = ContentElevation.high,
        shape = CarousalShape,
        onClick = {
            val direction = Buckets.direction(Type.ALBUMS)
            navigator.navigate(direction)
            activity.showAd()
        },

        content = {
            Column {
                //place carousel
                val photo by carousel.collectAsState()
                Crossfade(targetState = photo,
                    animationSpec = tween(4_000),
                    modifier = Modifier.weight(1f),
                    content = { value ->
                        value?.let {
                            KenBurns(modifier = Modifier.gradient(vertical = false), view = {
                                load(Repository.toAlbumArtUri(it))
                            })
                        }
                    })

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    // TODO: Remove background from the future version of the app.
                    //This is unnecessary
                    .background(Theme.colors.surface)
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = ContentPadding.normal),

                    content = {
                        Icon(
                            imageVector = Icons.Filled.Album,
                            contentDescription = null,
                        )

                        Text(
                            text = "ALBUMS",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = ContentPadding.normal),
                            letterSpacing = 6.sp,
                        )
                    })
            }
        })
}

@Composable
private inline fun More(
    expanded: Boolean, noinline onDismissRequest: () -> Unit
) {
    val navigator = LocalNavController.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, items = listOf(
        Icons.Outlined.Grain to "Genre",
        Icons.Outlined.Audiotrack to "Audios",
        Icons.Outlined.Folder to "Folders",
    ),

        onItemClick = { position ->
            val direction = when (position) {
                0 -> Buckets.direction(Type.GENRES)
                1 -> Tracks.direction(Type.AUDIOS)
                2 -> Buckets.direction(Type.FOLDERS)
                else -> error("No such direction supported!!")
            }
            navigator.navigate(direction)
        })
}


@Composable
private inline fun TextButton(
    text: String,
    icon: ImageVector,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = Theme.colors.onBackground),
        modifier = modifier,
        content = {
            Icon(imageVector = icon, contentDescription = null)
            Label(
                text = text.uppercase(),
                modifier = Modifier.padding(horizontal = ContentPadding.medium),
                style = Theme.typography.body2,
                fontWeight = FontWeight.SemiBold
            )
        })
}


context(LibraryViewModel) @Composable
private fun Main(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {

        //Favourite Button
        val navigator = LocalNavController.current
        TextButton(text = "Favourite",
            icon = Icons.Outlined.FavoriteBorder,
            modifier = Modifier.rotate(false),
            onClick = {
                val direction = Tracks.direction(Type.PLAYLISTS, Playback.PLAYLIST_FAVOURITE)
                navigator.navigate(direction)
            })

        // 2nd row of buttons
        Row(modifier = Modifier.rotate(false), content = {
            var expanded by rememberState(initial = false)
            IconButton(
                onClick = { expanded = true },
                content = {
                    Icon(imageVector = Icons.Outlined.MoreHoriz, contentDescription = null)
                    More(expanded = expanded) {
                        expanded = false
                    }
                },
            )
            TextButton(text = "Playlists", icon = Icons.Outlined.FeaturedPlayList, onClick = {
                val direction = Buckets.direction(Type.PLAYLISTS)
                navigator.navigate(direction)
            })
            TextButton(text = "Artists", icon = Icons.Outlined.Person, onClick = {
                val direction = Buckets.direction(Type.ARTISTS)
                navigator.navigate(direction)
            })
        })

        // 3rd element
        Carousal(
            modifier = Modifier
                .padding(end = ContentPadding.normal)
                .weight(1f)
        )
    }
}

private val RECENT_MAX_HEIGHT = 100.dp
private val RecentArtworkSize = 56.dp

@Composable
private fun Recent(
    modifier: Modifier = Modifier, value: Audio, error: Painter
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(70.dp),

        content = {
            val color = Theme.colors.onBackground
            val shape = CircleShape

            Image(
                data = value.albumUri,
                fallback = error,
                modifier = Modifier
                    .border(width = 3.dp, color = color, shape = shape)
                    .padding(5.dp)
                    .shadow(ContentElevation.low, shape, clip = false)
                    .clip(shape)
                    .requiredSize(51.dp)
            )

            Label(
                text = value.name,
                modifier = Modifier.padding(vertical = ContentPadding.small),
                style = Theme.typography.caption2,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        },
    )
}

private val callback = object : DiffUtil.ItemCallback<Audio>() {
    override fun areItemsTheSame(oldItem: Audio, newItem: Audio): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Audio, newItem: Audio): Boolean {
        return oldItem == newItem
    }
}

private fun observer(view: RecyclerView) = object : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        if ((view.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0) view.scrollToPosition(
            0
        )
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        onChanged()
    }

    override fun onItemRangeChanged(
        positionStart: Int, itemCount: Int, payload: Any?
    ) {
        onChanged()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        onChanged()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        onChanged()
    }

    override fun onItemRangeMoved(
        fromPosition: Int, toPosition: Int, itemCount: Int
    ) {
        onChanged()
    }
}


context(LibraryViewModel) @Composable
private fun Recents(modifier: Modifier = Modifier) {
    val recent by recent
    Crossfade(
        targetState = recent.isNullOrEmpty(), modifier = modifier.height(RECENT_MAX_HEIGHT)
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
                val observer = remember { { view: RecyclerView -> observer(view) } }
                val fallback = painterResource(id = R.drawable.default_art)

                Recycler(
                    list = recent ?: emptyList(),
                    callback = remember { callback },
                    contentPadding = PaddingValues(horizontal = ContentPadding.normal),
                    orientation = RecyclerView.HORIZONTAL,
                    observer = observer
                ) { audio ->
                    Recent(value = audio,
                        modifier = Modifier
                            .clip(Theme.shapes.small)
                            .clickable { }
                            .padding(4.dp),
                        error = fallback)
                }
            }
        }
    }
}


@Composable
fun Library(
    viewModel: LibraryViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        with(viewModel) {


            Reel()
            //
            val navigator = LocalNavController.current
            var query by rememberState(initial = "")
            Search(
                query = query,
                onQueryChanged = { query = it },
                modifier = Modifier
                    .offset(y = (-30).dp)
                    .padding(horizontal = 22.dp)
                    .zIndex(1f),
                elevation = 12.dp,
                placeholder = "Type here to search",
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank()) {
                            val direction = Tracks.direction(Type.AUDIOS, query = query)
                            navigator.navigate(direction)
                        }
                    },
                )
            )

            //header
            Text(
                text = "CATEGORIES",
                modifier = Modifier
                    .padding(horizontal = ContentPadding.normal)
                    .offset(y = (-15).dp),
                style = Theme.typography.overline,
                fontSize = 18.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.SemiBold
            )

            Main(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )


            //header
            Text(
                text = "RECENT",
                modifier = Modifier.padding(
                    horizontal = ContentPadding.normal, vertical = ContentPadding.normal
                ),
                style = Theme.typography.overline,
                fontSize = 18.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.SemiBold
            )

            Recents(
                modifier = Modifier.fillMaxWidth()
            )

            val windowPadding = LocalWindowPadding.current
            Spacer(
                modifier = Modifier
                    .animateContentSize()
                    .padding(windowPadding)
            )
        }
    }
}

