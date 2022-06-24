package com.prime.player.audio.tracks

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.insets.statusBarsPadding
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.*
import com.prime.player.core.models.Audio
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser
import com.prime.player.utils.share


private val HEADER_HEIGHT = 230.dp

@Composable
private fun TracksViewModel.TracksHeader(subtitle: String) {
    val advertiser = LocalAdvertiser.current
    val messenger = LocalMessenger.current
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
    ) {
        val line = createGuidelineFromBottom(30.dp)

        val (bgRef, titleRef, subtitleRef, shareRef, sortRef, shuffleRef, buttonRef) = createRefs()

        Spacer(modifier = Modifier
            .background(
                color = if (isLight()) PlayerTheme.colors.surface else Color.JetBlack
            )
            .constrainAs(bgRef) {
                top.linkTo(parent.top)
                bottom.linkTo(line)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            })

        createVerticalChain(titleRef, subtitleRef, chainStyle = ChainStyle.Packed(0f))

        val title by title.collectAsState()

        Header(
            text = title,
            style = PlayerTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(
                    start = Padding.LARGE,
                    top = Padding.MEDIUM,
                    bottom = Padding.SMALL,
                    end = Padding.LARGE
                )
                .constrainAs(titleRef) {},
            maxLines = 2
        )

        Caption(
            text = subtitle,
            modifier = Modifier
                .padding(horizontal = Padding.LARGE)
                .constrainAs(subtitleRef) {},
        )

        createHorizontalChain(shareRef, shuffleRef, sortRef, chainStyle = ChainStyle.Packed(0f))

        val context = LocalContext.current
        IconButton(
            onClick = {
                share(context)
                advertiser.show(false)
            },
            modifier = Modifier.constrainAs(shareRef) {
                start.linkTo(parent.start)
                bottom.linkTo(line)
            },
            enabled = selected.size > 0
        ) {
            Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share")
        }

        IconButton(
            onClick = { onRequestPlay(null, true, messenger); advertiser.show(false) },
            Modifier.constrainAs(shuffleRef) {
                top.linkTo(shareRef.top)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shuffle),
                contentDescription = "Shuffle"
            )
        }

        var show by remember {
            mutableStateOf(false)
        }

        IconButton(
            onClick = { show = true },
            Modifier.constrainAs(sortRef) {
                top.linkTo(shareRef.top)
            }
        ) {

            Icon(imageVector = Icons.Outlined.Sort, contentDescription = "Sort")

            DropdownMenu(
                title = "Sort By",
                expanded = show,
                items = listOf(
                    Icons.Outlined.ShortText to "Name",
                    Icons.Outlined.Person to "Artist",
                    Icons.Outlined.Album to "Album",
                    Icons.Outlined.Timer to "Duration"
                ),
                onDismissRequest = { show = false },
                onItemClick = { index ->
                    sortBy(
                        when (index) {
                            0 -> GroupBy.NAME
                            1 -> GroupBy.ARTIST
                            2 -> GroupBy.ALBUM
                            3 -> GroupBy.DURATION
                            else -> error("no idea!!")
                        },
                        messenger
                    )
                    advertiser.show(false)
                }
            )
        }

        FloatingActionButton(
            onClick = { onRequestPlay(null, false, messenger); advertiser.show(false) },
            modifier = Modifier.constrainAs(createRef()) {
                end.linkTo(parent.end, 30.dp)
                bottom.linkTo(line)
                top.linkTo(line)
            },
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.requiredSize(20.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TracksViewModel.Track(
    modifier: Modifier = Modifier,
    audio: Audio,
    isFavourite: Boolean,
    isSelected: Boolean
) {
    val icon = @Composable {
        Frame(
            modifier = Modifier.requiredSize(60.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, Color.White),
            elevation = Elevation.MEDIUM
        ) {
            AlbumArt(contentDescription = null, albumId = audio.album?.id ?: -1)
        }
    }

    val overline: @Composable() (() -> Unit) =
        @Composable {
            Label(text = audio.artist?.name ?: stringResource(id = R.string.unknown))
        }


    val text = @Composable {
        Label(
            text = audio.title,
            fontWeight = FontWeight.SemiBold
        )
    }

    val secondary = @Composable {
        Label(
            text = audio.album?.title ?: stringResource(id = R.string.unknown),
            fontWeight = FontWeight.SemiBold
        )
    }

    val trailing = @Composable {
        Row {
            IconButton(onClick = { toggleFav(audio.id) }, enabled = selected.size == 0) {
                Icon(
                    painter = painterResource(
                        when (isFavourite) {
                            true -> R.drawable.ic_heart_filled
                            else -> R.drawable.ic_heart
                        }
                    ),
                    contentDescription = null,
                    tint = PlayerTheme.colors.primary.copy(LocalContentAlpha.current)
                )
            }

            val show = remember {
                mutableStateOf(false)
            }

            IconButton(onClick = { show.value = true }, enabled = selected.size == 0) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                More(audio = audio, expanded = show)
            }
        }
    }


    Frame(
        modifier = modifier,
        color = when (isSelected) {
            true -> LocalContentColor.current.copy(Alpha.Indication)
            else -> Color.Transparent
        }
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.LARGE, vertical = Padding.MEDIUM),
            icon = icon,
            overlineText = overline,
            text = text,
            secondaryText = secondary,
            trailing = trailing
        )
    }

}

@Composable
fun More(audio: Audio, expanded: MutableState<Boolean>) {

    val advertiser = LocalAdvertiser.current
    var showPlaylistViewer = memorize {
        AddToPlaylist(audios = listOf(audio.id)) {
            hide()
            advertiser.show(false)
        }
    }

    val actions = LocalNavActionProvider.current as AudioNavigationActions

    var showTrackInfo = memorize {
        TrackInfo(audio) {
            hide()
            advertiser.show(false)
        }
    }

    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded.value,
        isEnabled = { index ->
            when (index) {
                1 -> audio.artist != null
                3 -> audio.album != null
                else -> true
            }
        },
        items = listOf(
            Icons.Outlined.PlaylistAdd to "Add to Playlist",
            Icons.Outlined.Person to "Go to Artist",
            Icons.Outlined.Info to "Info",
            Icons.Outlined.Album to "Go to Album",
            Icons.Outlined.Share to "Share"
        ), onDismissRequest = { expanded.value = false },
        onItemClick = { index ->
            when (index) {
                0 -> showPlaylistViewer.show()
                1 -> audio.artist?.let {
                    actions.toGroupViewer(GroupOf.ARTISTS, "${it.id}")
                }
                2 -> showTrackInfo.show()
                3 -> audio.album?.let {
                    actions.toGroupViewer(GroupOf.ALBUMS, "${it.id}")
                }
                4 -> context.share(audio)
            }
            advertiser.show(false)
        }
    )
}


@Composable
private fun TracksViewModel.ActionBar() {
    val systemUI = LocalSystemUiController.current
    SideEffect {
        systemUI.setStatusBarColor(Color.Transparent, false)
    }

    val bg = PlayerTheme.colors.secondary

    //Status background
    Spacer(
        modifier = Modifier
            .zIndex(1f)
            .background(bg)
            .fillMaxWidth()
            .statusBarsPadding()
    )

    val count = selected.size

    val showMenu = remember {
        mutableStateOf(false)
    }

    TopAppBar(
        modifier = Modifier
            .zIndex(0.5f)
            .statusBarsPadding(),
        title = {
            Label(
                text = "$count",
                modifier = Modifier.padding(end = Padding.EXTRA_LARGE),
                fontWeight = FontWeight.SemiBold
            )
        },
        backgroundColor = bg,
        navigationIcon = {
            IconButton(onClick = { selected.clear() }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "navigate back"
                )
            }
        },
        elevation = Elevation.MEDIUM,
        actions = {
            IconButton(onClick = { showMenu.value = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More"
                )
                ActionBarMenu(showMenu)
            }
        }
    )
}

@Composable
fun TracksViewModel.ActionBarMenu(expanded: MutableState<Boolean>) {

    val advertiser = LocalAdvertiser.current
    var showPlaylistViewer = memorize {
        AddToPlaylist(audios = selected) {
            hide()
            advertiser.show(false)
        }
    }


    DropdownMenu(
        expanded = expanded.value,
        items = listOf(
            Icons.Outlined.PlaylistAdd to "Add to Playlist",
        ), onDismissRequest = { expanded.value = false },
        onItemClick = { index ->
            if (index == 0)
                showPlaylistViewer.show()
        }
    )
}

@Composable
fun Tracks(padding: State<PaddingValues>, viewModel: TracksViewModel) {
    with(viewModel) {
        Scaffold(
            topBar = {
                Crossfade(
                    targetState = selected.size > 0,
                    animationSpec = tween(Anim.DURATION_MEDIUM)
                ) { show ->
                    when (show) {
                        true -> ActionBar()
                        else -> {
                            val title by title.collectAsState()
                            Toolbar(title = title)
                        }
                    }
                }
            }
        ) { inner ->
            val state by result.state.collectAsState()
            Crossfade(
                targetState = state,
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) { value ->
                when (value) {
                    Resource.State.Loading -> PlaceHolder(
                        lottieResource = R.raw.loading,
                        message = "Loading!!",
                        modifier = Modifier.fillMaxSize()
                    )
                    Resource.State.Success -> TrackList(padding = padding)
                    Resource.State.Error -> PlaceHolder(
                        lottieResource = R.raw.error,
                        message = result.message ?: "",
                        modifier = Modifier.fillMaxSize()
                    )
                    Resource.State.Empty -> PlaceHolder(
                        lottieResource = R.raw.empty,
                        message = result.message ?: "",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TracksViewModel.TrackList(padding: State<PaddingValues>) {
    val data by result.data
    data?.let { (subtitle, map) ->
        val favouriteList by favourite.collectAsState()
        val secondary = PlayerTheme.colors.secondaryVariant
        val messenger = LocalMessenger.current

        val padding by padding
        LazyColumn(contentPadding = padding) {
            item("Header") {
                TracksHeader(subtitle = subtitle)
            }

            //header
            map.forEach { (header, list) ->
                if (header.isNotEmpty()) {
                    item(key = header) {
                        if (header.length == 1)
                            Header(
                                text = header,
                                modifier = Modifier.padding(horizontal = Padding.EXTRA_LARGE),
                                style = PlayerTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                color = secondary
                            )
                        else
                            Label2(
                                text = header,
                                modifier = Modifier
                                    .padding(top = Padding.LARGE)
                                    .padding(
                                        horizontal = Padding.LARGE,
                                    )
                                    .fillMaxWidth(0.45f),
                                color = secondary,
                                maxLines = 2,
                                fontWeight = FontWeight.SemiBold
                            )

                        Divider(
                            color = secondary.copy(Alpha.Divider),
                            modifier = Modifier.padding(
                                vertical = Padding.MEDIUM,
                                horizontal = Padding.LARGE
                            )
                        )
                    }
                }


                items(list, key = { it.id }) { audio ->
                    Track(
                        modifier = Modifier.combinedClickable(onClick = {
                            if (!selected.isEmpty())
                                toggleSelection(audio.id)
                            else
                                onRequestPlay(audio, true, messenger)
                        }, onLongClick = {
                            toggleSelection(audio.id)
                        }),
                        audio = audio,
                        isFavourite = favouriteList?.audios?.contains(audio.id) ?: false,
                        isSelected = selected.contains(audio.id)
                    )
                    if (list.last() != audio)
                        Divider()
                }
            }
        }
    }
}