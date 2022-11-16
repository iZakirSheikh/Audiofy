package com.prime.player.audio.tracks

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.twotone.Share
import androidx.compose.material.icons.twotone.Shuffle
import androidx.compose.material.icons.twotone.Sort
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import com.prime.player.*
import com.prime.player.R
import com.prime.player.audio.*
import com.prime.player.billing.*
import com.prime.player.common.FileUtils
import com.prime.player.common.Util
import com.prime.player.common.compose.*
import com.prime.player.common.formatAsDuration
import com.prime.player.common.toFormattedDataUnit
import com.prime.player.core.Audio
import com.primex.core.Text
import com.primex.core.drawHorizontalDivider
import com.primex.core.raw
import com.primex.core.rememberState
import com.primex.ui.*
import cz.levinzonr.saferoute.core.annotations.Route
import cz.levinzonr.saferoute.core.annotations.RouteArg
import cz.levinzonr.saferoute.core.navigateTo


context(TracksViewModel) @Composable
private fun TopAppBar(
    modifier: Modifier = Modifier
) {
    val showActionBar by remember { derivedStateOf { selected.size > 0 } }
    // show/ hide action bar
    Crossfade(
        targetState = showActionBar,
        modifier = modifier
    ) { show ->
        when (show) {
            true -> ActionBar()
            else -> Toolbar()
        }
    }
}


val TopBarShape = CircleShape
context(TracksViewModel)  @Composable
private fun Toolbar(
    modifier: Modifier = Modifier
) {
    val title = stringResource(value = title)
    val query = query
    val navigator = LocalNavController.current

    NeumorphicTopAppBar(
        shape = TopBarShape,
        elevation = ContentElevation.low,
        modifier = modifier.padding(top = ContentPadding.medium),
        lightShadowColor = Material.colors.lightShadowColor,
        darkShadowColor = Material.colors.darkShadowColor,

        title = {
            Column(modifier = Modifier.fillMaxWidth(0.60f)) {
                Label(text = title)
                if (query != null) {
                    Label(
                        text = "$query",
                        style = Material.typography.caption2
                    )
                }
            }
        },

        navigationIcon = {
            IconButton(
                onClick = { navigator.navigateUp() },
                imageVector = Icons.Outlined.ReplyAll,
                contentDescription = null
            )
        },
    )
}

context(TracksViewModel)  @Composable
private fun Actions(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    var showPlaylistViewer by rememberState(initial = false)
    //TODO: Add code for Playlist Viewer
    val context = LocalContext.current
    Playlists(
        value = playlists.collectAsState(initial = emptyList()).value,
        expanded = showPlaylistViewer,
        onPlaylistClick = {
            if (selected.isNotEmpty() && it != null) {
                addToPlaylist(it, selected)
                Toast.makeText(context, "Adding tracks to Playlist ${it.name}.", Toast.LENGTH_SHORT)
                    .show()
            }
            showPlaylistViewer = false
        }
    )

    DropdownMenu(
        expanded = expanded,
        items = listOf(
            Icons.Outlined.PlaylistAdd to "Add to Playlist",
        ), onDismissRequest = onDismissRequest,
        onItemClick = { index ->
            if (index == 0) showPlaylistViewer = true
        }
    )
}

context(TracksViewModel)  @Composable
private fun ActionBar(
    modifier: Modifier = Modifier
) {
    val count = selected.size
    NeumorphicTopAppBar(
        title = { Label(text = "$count selected") },
        modifier = modifier.padding(top = ContentPadding.medium),
        elevation = ContentElevation.low,
        shape = TopBarShape,
        contentColor = Material.colors.secondary,

        navigationIcon = {
            IconButton(
                onClick = { selected.clear() },
                imageVector = Icons.Outlined.Close,
                contentDescription = null
            )
        },

        actions = {
            var show by rememberState(initial = false)
            IconButton(onClick = { show = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More"
                )
                Actions(expanded = show) {
                    show = false
                }
            }
        }
    )
}

context(TracksViewModel) @Composable
private fun SortBy(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    var checked by rememberState(initial = 0)
    val channel = LocalSnackDataChannel.current
    DropdownMenu(
        title = "Sort By",
        checked = checked,
        expanded = expanded,
        onDismissRequest = onDismissRequest,

        items = listOf(
            Icons.Outlined.ShortText to "Name",
            Icons.Outlined.Person to "Artist",
            Icons.Outlined.Album to "Album",
            Icons.Outlined.Timer to "Duration"
        ),

        onItemClick = { index ->
            filter(
                value =
                when (index) {
                    0 -> GroupBy.NAME
                    1 -> GroupBy.ARTIST
                    2 -> GroupBy.ALBUM
                    3 -> GroupBy.DURATION
                    else -> error("no idea!!")
                },

                //TODO add support for ascending
                ascending = false,
                channel = channel
            )
            checked = index
        },
    )
}

@Composable
@NonRestartableComposable
private inline fun ConstraintLayoutScope.Text(
    reference: ConstrainedLayoutReference,
    value: String,
    title: String,
    noinline block: ConstrainScope.() -> Unit
) {
    // value
    Header(
        text = value,
        style = Material.typography.h6,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.constrainAs(reference, block)
    )

    // Label
    Label(
        text = title,
        modifier = Modifier.constrainAs(createRef()) {
            start.linkTo(reference.start)
            end.linkTo(reference.end)
            top.linkTo(reference.bottom, ContentPadding.medium)
        },
        style = Material.typography.caption2,
        color = LocalContentColor.current.copy(ContentAlpha.medium),
        fontWeight = FontWeight.SemiBold
    )
}

private val HeaderArtWorkShape = RoundedCornerShape(20)
context(TracksViewModel) @Composable
fun Header(
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier) {

        val meta by header
        val title = stringResource(value = title)
        val (Artwork, Play, Title, Subtitle, Share, Shuffle, Sort, Divider) = createRefs()

        // create the chain
        // this will determine the size of the
        // Header
        constrain(
            ref = createVerticalChain(Artwork, Share, Divider, chainStyle = ChainStyle.Packed),
            constrainBlock = {
                // Divider will act as the center anchor of Play Button
                top.linkTo(parent.top, ContentPadding.normal)
                bottom.linkTo(parent.bottom, ContentPadding.large)
            }
        )

        Surface(
            shape = HeaderArtWorkShape,
            elevation = ContentElevation.high,

            content = {
                val artwork = meta?.artwork
                Image(data = artwork)
            },

            modifier = Modifier
                .constrainAs(Artwork) {
                    start.linkTo(parent.start, ContentPadding.normal)
                    width = Dimension.value(76.dp)
                    height = Dimension.ratio("0.61")
                },
        )

        val context = LocalContext.current
        val channel = LocalSnackDataChannel.current
        val enabled by remember { derivedStateOf { selected.size > 0 } }
        //Share
        IconButton(
            onClick = { share(context, channel) },
            imageVector = Icons.TwoTone.Share,
            contentDescription = null,
            enabled = enabled,
            modifier = Modifier
                .padding(top = ContentPadding.large)
                .constrainAs(Share) {}
        )

        // Divider
        Divider(
            modifier = Modifier
                .constrainAs(Divider) {}
        )


        // line 2 is the line of Button aligned with share
        constrain(
            ref = createHorizontalChain(Share, Shuffle, Sort, chainStyle = ChainStyle.Packed(0f)),
            constrainBlock = {
                start.linkTo(Artwork.start)
            }
        )

        //Shuffle
        IconButton(
            imageVector = Icons.TwoTone.Shuffle,
            contentDescription = null,

            onClick = { onRequestPlay(context, true, channel = channel) },

            modifier = Modifier.constrainAs(Shuffle) {
                bottom.linkTo(Share.bottom)
            }
        )

        // Sort
        var show by rememberState(initial = false)
        IconButton(
            onClick = { show = true },

            modifier = Modifier.constrainAs(Sort) {
                bottom.linkTo(Share.bottom)
            },

            content = {
                Icon(imageVector = Icons.TwoTone.Sort, contentDescription = null)
                SortBy(expanded = show) {
                    show = false
                }
            }
        )

        // Play Button
        NeumorphicButton(
            shape = CircleShape,
            onClick = { onRequestPlay(context, false, null, channel = channel) },

            colors = NeumorphicButtonDefaults.neumorphicButtonColors(
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor
            ),

            modifier = Modifier
                .constrainAs(Play) {
                    top.linkTo(Divider.top)
                    bottom.linkTo(Divider.bottom)
                    end.linkTo(parent.end, ContentPadding.large)
                    width = Dimension.value(60.dp)
                    height = Dimension.value(60.dp)
                },

            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(20.dp)
                )
            }
        )


        // Title
        Header(
            text = title,
            style = Material.typography.h5,
            maxLines = 2,
            textAlign = TextAlign.Start,

            modifier = Modifier.constrainAs(Title) {
                start.linkTo(Artwork.end, ContentPadding.normal)
                end.linkTo(parent.end, ContentPadding.normal)
                top.linkTo(Artwork.top)
                width = Dimension.fillToConstraints
            }
        )

        // Subtitle
        val subtitle = stringResource(value = meta?.subtitle) ?: AnnotatedString("")
        Label(
            text = subtitle,
            textAlign = TextAlign.Start,
            style = Material.typography.caption2,
            fontWeight = FontWeight.SemiBold,
            color = LocalContentColor.current.copy(ContentAlpha.medium),
            maxLines = 2,
            modifier = Modifier
                .constrainAs(Subtitle) {
                    end.linkTo(parent.end, ContentPadding.normal)
                    top.linkTo(Title.bottom)
                    start.linkTo(Title.start)
                    width = Dimension.fillToConstraints
                },
        )

        // line 3 of details
        val (Duration, Divider1, Tracks, Divider2, Size) = createRefs()
        constrain(
            ref = createHorizontalChain(
                Duration,
                Divider1,
                Tracks,
                Divider2,
                Size,
                chainStyle = ChainStyle.SpreadInside
            ),

            constrainBlock = {
                start.linkTo(Artwork.end, ContentPadding.normal)
                end.linkTo(parent.end, ContentPadding.normal)
            }
        )

        //Duration
        val duration = Util.formatAsDuration((meta?.duration ?: 0).toLong())
        Text(
            reference = Duration,
            value = duration,
            title = "Duration",

            block = {
                top.linkTo(Subtitle.bottom, ContentPadding.normal)
            }
        )

        // Divider 1
        Divider(
            modifier = Modifier.constrainAs(Divider1) {
                top.linkTo(Duration.top, -ContentPadding.small)
                height = Dimension.value(56.dp)
                width = Dimension.value(1.dp)
            }
        )

        //Tracks
        val count = meta?.cardinality ?: 0
        Text(
            reference = Tracks,
            value = "$count",
            title = "Tracks",

            block = {
                top.linkTo(Duration.top)
            },
        )

        //Divider 2
        Divider(
            modifier = Modifier.constrainAs(Divider2) {
                height = Dimension.value(56.dp)
                top.linkTo(Duration.top, -ContentPadding.small)
                width = Dimension.value(1.dp)
            }
        )

        // Size
        val size = FileUtils.toFormattedDataUnit(context, meta?.size ?: 0L)
        Text(
            reference = Size,
            value = size,
            title = "Size",
            block = {
                top.linkTo(Duration.top)
            },
        )
    }
}


@Composable
@NonRestartableComposable
private fun Header(
    text: Text,
    modifier: Modifier = Modifier
) {
    val title = stringResource(value = text)
    val secondary = Material.colors.secondary

    val hModifier = Modifier
        .drawHorizontalDivider(color = Material.colors.secondary)
        .fillMaxWidth()
        .then(modifier)

    when (title.length) {

        // draw a single char/line header
        // in case the length of the title string is 1
        1 -> Header(
            text = title,
            modifier = hModifier
                .padding(top = ContentPadding.normal)
                .padding(horizontal = ContentPadding.large),
            style = Material.typography.h4,
            fontWeight = FontWeight.Bold,
            color = secondary
        )

        // draw a multiline line header
        // in case the length of the title string is 1
        else -> Label(
            text = title,
            modifier = hModifier
                .padding(top = ContentPadding.large, bottom = ContentPadding.small)
                .padding(horizontal = ContentPadding.normal),
            color = secondary,
            maxLines = 2,
            fontWeight = FontWeight.SemiBold,
            style = Material.typography.body1,
        )
    }
}

@Composable
private fun Audio.More(
    resolver: TracksViewModel,
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    var showTrackInfo by rememberState(initial = false)
    Properties(
        expanded = showTrackInfo,
        onDismissRequest = { showTrackInfo = false }
    )

    var showPlaylistViewer by rememberState(initial = false)
    val context = LocalContext.current
    Playlists(
        value = resolver.playlists.collectAsState(initial = emptyList()).value,
        expanded = showPlaylistViewer,
        onPlaylistClick = {
            if (it != null) {
                resolver.addToPlaylist(it, listOf(id))
                Toast.makeText(context, "Adding tracks to Playlist ${it.name}.", Toast.LENGTH_SHORT)
                    .show()
            }
            showPlaylistViewer = false
        }
    )
    val navigator = LocalNavController.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,

        isEnabled = { index ->
            when (index) {
                1 -> artist.isNotBlank()
                3 -> album.isNotBlank()
                else -> true
            }
        },

        items = listOf(
            Icons.Outlined.PlaylistAdd to "Add to Playlist", //0
            Icons.Outlined.Person to "Go to Artist", //1
            Icons.Outlined.Info to "Info", //2
            Icons.Outlined.Album to "Go to Album",  //3
            Icons.Outlined.Share to "Share" //4
        ),

        onItemClick = { index ->
            when (index) {
                0 -> showPlaylistViewer = true
                1 -> {
                    val direction = TracksRoute(Type.ARTISTS.name, this.artist)
                    navigator.navigateTo(direction)
                }
                2 -> showTrackInfo = true
                3 -> {
                    val direction = TracksRoute(Type.ALBUMS.name, this.album)
                    navigator.navigateTo(direction)
                }
                4 -> resolver.share(context)
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Track(
    value: Audio,
    modifier: Modifier = Modifier,
    fallback: Painter? = null,
    drawDivider: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    trailing: @Composable () -> Unit
) {
    ListTile(

        selected = selected,
        centreVertically = false,

        overlineText = {
            Label(
                text = value.title,
                style = Material.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        },
        text = {
            Label(
                text = value.artist,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = ContentPadding.small),
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                fontWeight = FontWeight.SemiBold,
            )
        },
        trailing = trailing,

        // secondary text
        secondaryText = {
            Label(
                style = Material.typography.body2,
                text = value.album,
                color = LocalContentColor.current.copy(ContentAlpha.medium),

                fontWeight = FontWeight.SemiBold,
            )
        },

        //
        modifier = Modifier
            .then(
                if (drawDivider) Modifier.drawHorizontalDivider(
                    color = Material.colors.onSurface,
                ) else Modifier
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(top = 2.dp)
            .then(modifier),

        leading = {
            Surface(
                modifier = Modifier.requiredSize(54.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White),
                elevation = ContentElevation.high
            ) {
                Image(
                    albumId = value.albumId,
                    fallback = fallback,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    )
}


private const val CONTENT_TYPE_HEADER = "Header"
private const val CONTENT_TYPE_LIST_HEADER = "List_Header"
private const val CONTENT_TYPE_LIST_ITEM = "List_Item"

@Composable
private inline fun Audio.trailing(resolver: TracksViewModel) =
    @Composable {
        Row {
            val favourite = resolver.favourite.value.contains(id)
            val channel = LocalSnackDataChannel.current

            Crossfade(targetState = favourite) {
                IconButton(
                    contentDescription = null,

                    imageVector =
                    if (it) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    onClick = { resolver.toggleFav(id, channel) },
                )
            }

            // audio menu
            var show by rememberState(initial = false)
            IconButton(
                onClick = { show = true },
                content = {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null
                    )

                    // menu
                    More(
                        expanded = show,
                        resolver = resolver,

                        onDismissRequest = {
                            show = false
                        }
                    )
                }
            )
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Route(
    args = [
        // type of the group whose tracks are to be displayed
        RouteArg(
            name = "type",
            type = String::class
        ),

        // id of the particular group in type.
        RouteArg(name = "id", type = String::class),

        // optional query works only with all audios.
        RouteArg(
            name = "query",
            type = String::class,
            isOptional = true,
            isNullable = true
        ),
    ]
)
@Composable
fun Tracks(
    viewModel: TracksViewModel
) {
    with(viewModel) {
        Scaffold(
            topBar = {
                val colorize by Material.colorStatusBar
                TopAppBar(
                    modifier = Modifier
                        .statusBarsPadding2(
                            color = if (colorize) Material.colors.primaryVariant else Color.Transparent,
                            darkIcons = !colorize && Material.colors.isLight
                        )
                        .drawHorizontalDivider(color = Material.colors.onSurface)
                        .padding(bottom = ContentPadding.medium)
                )

            },
            content = {
                val purchase by LocalContext.billingManager.observeAsState(id = Product.DISABLE_ADS)
                Placeholder(
                    value = result,
                    modifier = Modifier.padding(it)
                ) { data ->

                    val fallback = painterResource(id = R.drawable.default_art)
                    val padding = LocalWindowPadding.current
                    val context = LocalContext.current
                    LazyColumn(
                        contentPadding = padding
                    ) {
                        // emit the actual header of the list only once
                        item(
                            "Header",
                            contentType = CONTENT_TYPE_HEADER,
                            content = { Header() }
                        )

                        if (!purchase.purchased)
                            item(
                                contentType = "banner_ad",
                                content = {
                                    Banner(
                                        placementID = Placement.BANNER_VIEWER,
                                        modifier = Modifier
                                            .padding(vertical = ContentPadding.medium),
                                    )
                                }
                            )

                        data.forEach { (header, list) ->

                            //emit  list header
                            item(
                                header.raw,
                                contentType = CONTENT_TYPE_LIST_HEADER,
                                content = {
                                    Header(
                                        text = header,
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            )

                            // emit list
                            items(
                                list,
                                key = { it.id },
                                contentType = { CONTENT_TYPE_LIST_ITEM }
                            ) { audio ->
                                // emit checked for each item.
                                val checked by remember { derivedStateOf { selected.contains(audio.id) } }
                                Track(
                                    value = audio,
                                    fallback = fallback,
                                    drawDivider = list.last() != audio,
                                    selected = checked,
                                    trailing = audio.trailing(resolver = this@with),
                                    modifier = Modifier.animateItemPlacement(),
                                    onLongClick = { select(audio.id) },

                                    onClick = {
                                        if (!selected.isEmpty())
                                            select(audio.id)
                                        else
                                            onRequestPlay(context, true, audio)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}