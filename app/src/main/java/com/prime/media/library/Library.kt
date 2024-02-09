@file:Suppress("CrossfadeLabel", "FunctionName")
@file:OptIn(ExperimentalTextApi::class, ExperimentalTextApi::class)

package com.prime.media.library

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.load
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.common.io.Files.append
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.Banner
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.KenBurns
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.None
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.PreviewTheme
import com.prime.media.core.compose.Range
import com.prime.media.core.compose.purchase
import com.prime.media.core.compose.shape.CompactDisk
import com.prime.media.core.compose.shape.FolderShape
import com.prime.media.core.compose.size
import com.prime.media.core.db.Audio
import com.prime.media.core.db.Playlist
import com.prime.media.core.db.albumUri
import com.prime.media.core.playback.Playback
import com.prime.media.directory.GroupBy
import com.prime.media.directory.playlists.Members
import com.prime.media.directory.store.Albums
import com.prime.media.directory.store.Artists
import com.prime.media.directory.store.Audios
import com.prime.media.directory.store.Genres
import com.prime.media.impl.Repository
import com.prime.media.overlay
import com.prime.media.settings.Settings
import com.prime.media.surfaceColorAtElevation
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.lerp
import com.primex.core.rotateTransform
import com.primex.core.textResource
import com.primex.core.withSpanStyle
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

private const val TAG = "Library"

/**
 * Defines the typography style for the large top bar title.
 */
private val LargeTopBarTitle
    @Composable inline get() = Material.typography.h4.copy(lineHeight = 20.sp)

/**
 * Defines the typography style for the normal top bar title.
 */
private val NormalTopBarTitle
    @Composable inline get() = Material.typography.body1

/**
 * Composable function to display the app bar for the library screen.
 *
 * @param state: The current Library state containing information for the UI.
 * @param modifier: Optional modifier to apply to the top bar.
 * @param behaviour: Optional scroll behavior for the top bar.
 * @param insets: Window insets to consider for layout.
 */
@Composable
private fun CarousalAppBar(
    state: Library,
    modifier: Modifier = Modifier,
    behaviour: TopAppBarScrollBehavior? = null,
    insets: WindowInsets = WindowInsets.None
) {
    CollapsableTopBarLayout(
        height = 56.dp,
        maxHeight = 220.dp,
        insets = insets,
        scrollBehavior = behaviour,
        modifier = modifier
    ) {
        // Background with image representation and gradient
        val id by state.carousel.collectAsState()
        val colors = Material.colors
        val gradient =
            Brush.verticalGradient(colors = listOf(Color.Transparent, colors.background))

        // Background
        Crossfade(
            targetState = Repository.toAlbumArtUri(id ?: 0),
            animationSpec = tween(4_000),
            modifier = Modifier
                .fillMaxSize()
                .foreground(lerp(colors.surfaceColorAtElevation(2.dp), Color.Transparent, fraction))
                .foreground(gradient)
                .layoutId(TopAppBarDefaults.LayoutIdBackground),
            content = { value ->
                KenBurns(
                    modifier = Modifier,
                    view = {
                        load(value)
                        // Pause/resume animation based on collapse/expand state
                        if (fraction < 1.0) this.pause() else this.resume()
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                )
            }
        )

        // Navigation Icon.
        val provider = LocalSystemFacade.current
        IconButton(
            onClick = { provider.launchAppStore() },
            painter = rememberVectorPainter(image = Icons.Outlined.Info),
            contentDescription = "about us",
            modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdNavIcon)
        )

        // Actions  (Buy and settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdAction),
            content = {
                // Buy full version button.
                val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
                if (!purchase.purchased)
                    IconButton(
                        painter = painterResource(id = R.drawable.ic_remove_ads),
                        contentDescription = null,
                        onClick = { provider.launchBillingFlow(BuildConfig.IAP_NO_ADS) }
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

        // Title with smooth transition between sizes and positions
        Text(
            text = textResource(id = R.string.library_title),
            fontWeight = FontWeight.Light,
            style = lerp(NormalTopBarTitle, LargeTopBarTitle, fraction),
            modifier = Modifier
                .road(Alignment.CenterStart, Alignment.BottomStart)
                .layoutId(TopAppBarDefaults.LayoutIdCollapsable_title)
                .offset {
                    val dp = lerp(0.dp, 16.dp, fraction)
                    IntOffset(dp.roundToPx(), -dp.roundToPx())
                }
        )
    }
}

private val ZeroPadding = PaddingValues(0.dp)

private val DefaultListItemArrangement = Arrangement.spacedBy(ContentPadding.medium)

/**
 * Composable function displaying a lazily loaded list of items with loading and empty states.
 *
 * @param items: The list of items to display, or null to show loading state.
 * @param modifier: Optional modifier to apply to the entire list container.
 * @param key: Optional key function for items, defaults to using item identity.
 * @param listState: The state of the lazy list for scrolling and performance.
 * @param itemContent: Composable function that defines the content for each item.
 */
@Composable
private inline fun <T> StatefulLazyList(
    items: List<T>?,
    modifier: Modifier = Modifier,
    noinline key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = ZeroPadding,
    horizontalArrangement: Arrangement.Horizontal = DefaultListItemArrangement,
    listState: LazyListState = rememberLazyListState(),
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    // Determine the current state of the list (loading, empty, or content)
    val state = when {
        items == null -> 0 // Loading state
        items.isEmpty() -> 1 // Empty state
        else -> 2 // Show list content
    }

    // Use Crossroad to smoothly transition between states based on state value
    Crossfade(
        targetState = state,
        modifier = modifier
    ) { value ->
        when (value) {
            // Loading state
            0 -> Placeholder(
                iconResId = R.raw.lt_loading_bubbles,
                title = "",
                message = "Loading"
            )
            // Empty state
            1 -> Placeholder(
                iconResId = R.raw.lt_empty_box,
                title = "",
                message = "Oops!! empty."
            )
            // Show list content
            else -> {
                LazyRow(
                    state = listState,
                    contentPadding = contentPadding,
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ensure first item is visible by adding a spacer at the front
                    item(contentType = "library_list_spacer") {
                        Spacer(modifier = Modifier.width(0.dp))
                    }

                    // Display actual items using "items" function
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

/**
 * Composable function to create a clickable shortcut with an icon and label.
 *
 * @param icon: The ImageVector representing the shortcut's icon.
 * @param label: The CharSequence representing the shortcut's label.
 * @param onAction: The action to perform when the shortcut is clicked.
 * @param modifier: Optional modifier to apply to the shortcut's layout.
 */
@Composable
private fun Shortcut(
    icon: ImageVector,
    label: CharSequence,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Base container for the shortcut with styling and click handling
    Box(
        modifier = modifier
            .clip(FolderShape) // Shape the shortcut like a folder
            .border(1.dp, Material.colors.onBackground.copy(0.50f), FolderShape) // Light border
            .clickable(
                null,
                ripple(true, color = Material.colors.primary), // Ripple effect on click
                role = Role.Button, // Semantically indicate a button
                onClick = onAction // Trigger the action on click
            )
            .padding(horizontal = 8.dp, vertical = 8.dp) // Add internal padding
            .size(70.dp, 58.dp) // Set size (adjust factor if needed)
        // then modifier // Apply additional modifiers
    ) {
        // Icon at the top
        Icon(
            imageVector = icon,
            contentDescription = null, // Ensure a content description is provided elsewhere
            tint = Material.colors.primary,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Label at the bottom
        Label(
            text = label,
            style = Material.typography.caption,
            color = Material.colors.primary,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Shortcuts(
    modifier: Modifier = Modifier,
) {
    // FlowRow to arrange shortcuts horizontally with spacing
    FlowRow(
        modifier = modifier/*.scaledLayout(1.3f)*/,
        horizontalArrangement = DefaultListItemArrangement,
        verticalArrangement = DefaultListItemArrangement,
        content = {
            val navigator = LocalNavController.current

            // Removed commented-out "Folders" shortcut for now
            // Add it back when implemented or provide an explanation

            // Shortcut for Genres navigation
            Shortcut(
                onAction = { navigator.navigate(Genres.direction()) },
                icon = Icons.Outlined.Grain,
                label = textResource(id = R.string.genres),
            )

            // Shortcut for Audios navigation
            Shortcut(
                onAction = { navigator.navigate(Audios.direction(Audios.GET_EVERY)) },
                icon = Icons.Outlined.GraphicEq,
                label = textResource(id = R.string.audios),
            )

            // Shortcut for Artists navigation
            Shortcut(
                onAction = { navigator.navigate(Artists.direction()) },
                icon = Icons.Outlined.Person,
                label = textResource(id = R.string.artists),
            )

            // Shortcut for Albums navigation
            Shortcut(
                onAction = { navigator.navigate(Albums.direction()) },
                icon = Icons.Outlined.Album,
                label = textResource(id = R.string.albums),
            )

            // Shortcut for Favourite playlist navigation
            Shortcut(
                onAction = { navigator.navigate(Members.direction(Playback.PLAYLIST_FAVOURITE)) },
                icon = Icons.Outlined.FavoriteBorder,
                label = textResource(id = R.string.favourite),
            )
        }
    )
}

/**
 * Composable function to create a clickable recent item with artwork and label.
 *
 * @param label: The CharSequence representing the item's label.
 * @param onClick: The action to perform when the item is clicked.
 * @param modifier: Optional modifier to apply to the item's layout.
 * @param artworkUri: Optional URI for the item's artwork image.
 */
@Composable
private fun RecentItem(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null
) {
    // Column container for the recent item with styling and click handling
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(Material.shapes.small) // Apply rounded corners
            .clickable(onClick = onClick) // Enable clicking
        //.then(modifier) // Apply additional modifiers
    ) {
        // Artwork section with border and shadow
        Artwork(
            data = artworkUri,
            modifier = Modifier
                .size(60.dp) // Adjust size if needed
                .border(2.5.dp, Color.White, CompactDisk) // Add white border
                .shadow(ContentElevation.low, CompactDisk) // Add subtle shadow
        )

        // Label below the artwork with padding and styling
        Label(
            text = label,
            modifier = Modifier
                .padding(top = ContentPadding.medium)
                .width(75.dp),
            style = Material.typography.caption2,
            maxLines = 2, // Allow at most 2 lines for label
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Composable function that displays a list of recently played items.
 *
 * @param state: The Library state containing recent items and click handling logic.
 * @param modifier: Optional modifier to apply to the list container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentlyPlayedList(
    state: Library,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ZeroPadding
) {
    // Collect recently played items from the Library state
    val recents by state.recent.collectAsState(initial = null)

    // Display the list with loading, empty, and content states
    StatefulLazyList(
        items = recents,      // Provide the list of recent items
        key = { it.uri },    // Unique key for each item based on its URI
        modifier = modifier,  // Apply optional modifiers
        horizontalArrangement = DefaultListItemArrangement,
        contentPadding = contentPadding,
        itemContent = {      // Define how to display each item
            RecentItem(
                it.title,             // Use the item's title
                onClick = { state.onClickRecentFile(it.uri) },  // Trigger click action
                modifier = Modifier
                    .animateItemPlacement(),      // Animate item placement
                artworkUri = it.artwork,// Display artwork if available
            )
        }
    )
}

/**
 * Composable function to create a clickable newly added item with image, label, and play icon.
 *
 * @param label: The CharSequence representing the item's label.
 * @param onClick: The action to perform when the item is clicked.
 * @param modifier: Optional modifier to apply to the item's layout.
 * @param imageUri: Optional Uri for the item's image.
 * @param alignment: The alignment of the image within the item (default: Center).
 */
@Composable
private fun NewlyAddedItem(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUri: Uri? = null,
    alignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = modifier
            //.scale(0.96f) // Subtle zoom-in effect
            .shadow(ContentElevation.low, RoundedCornerShape(7)) // Light shadow
            .clickable(onClick = onClick) // Enable clicking
            .size(204.dp, 120.dp), // Set minimum size
        // .then(modifier), // Apply additional modifiers
        contentAlignment = Alignment.Center // Center content within the box
    ) {
        val colors = listOf(
            Material.colors.primary.blend(Color.Black, 0.5f), // Gradient start: transparent primary
            Color.Transparent, // Gradient middle: transparent
            Color.Transparent, // Gradient end: transparent
        )

        // Image with horizontal gradient overlay
        Artwork(
            data = imageUri,
            alignment = alignment,
            modifier = Modifier
                .foreground(Brush.horizontalGradient(colors)) // Apply transparent-to-primary gradient
                .matchParentSize() // Fill available space
        )

        // Label aligned to the left with padding and styling
        Label(
            text = label,
            modifier = Modifier
                .padding(horizontal = ContentPadding.large) // Add horizontal padding
                .fillMaxWidth(0.5f) // Take up half the available width
                .align(Alignment.CenterStart), // Align to the left
            style = Material.typography.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2, // Allow at most 2 lines for label
            color = Material.colors.onPrimary, // Use contrasting text color
        )

        // Play icon aligned to the right with padding and size
        Icon(
            imageVector = Icons.TwoTone.PlayCircle,
            contentDescription = null, // Provide content description for accessibility
            modifier = Modifier
                .align(Alignment.CenterEnd) // Align to the right
                .padding(horizontal = ContentPadding.large) // Add horizontal padding
                .size(40.dp, 40.dp), // Set icon size
            tint = Material.colors.onPrimary // Use contrasting color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewlyAddedList(
    state: Library,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ZeroPadding
) {
    // Collect newly added items from the Library state
    val audios by state.newlyAdded.collectAsState(initial = null)
    // Display the list with loading, empty, and content states
    StatefulLazyList(
        items = audios,
        key = { it.id },
        modifier = modifier,
        horizontalArrangement = DefaultListItemArrangement,
        contentPadding = contentPadding
    ) { item ->
        // Create newly added item with parallax-adjusted image alignment
        NewlyAddedItem(
            label = item.name,
            onClick = { state.onClickRecentAddedFile(item.id) },
            imageUri = item.albumUri,
            modifier = Modifier
                .animateItemPlacement(),
        )
    }
}

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
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: TextStyle = Material.typography.h5,
    onMoreClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = ZeroPadding
) {
    Row(
        modifier = modifier.then(Modifier.padding(contentPadding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        val color = LocalContentColor.current
        Text(
            style = style,
            //fontSize = 28.sp, // Not recommended; use style instead
            text = buildAnnotatedString {
                append(title)
                if (subtitle == null) return@buildAnnotatedString

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

        // More button (conditionally displayed)
        if (onMoreClick != null) {
            OutlinedButton(
                label = "More",
                onClick = onMoreClick,
                shape = CircleShape,
                modifier = Modifier.scale(0.8f),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                border = ButtonDefaults.outlinedBorder
            )
        }
    }
}

private val DefaultContentPadding = PaddingValues(
    horizontal = ContentPadding.large,
    vertical = ContentPadding.medium
)

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
            strategy = HorizontalTwoPaneStrategy(offset, false),
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
            }
        )
    }
}

private val StandardDensity = 2.7875001f
@Composable
fun Library(
    state: Library
) {
    val windowSize = LocalWindowSize.current
    val (wRange, hRange) = windowSize
    val (width, height) = windowSize.value
    val padding = DefaultContentPadding
    val isTwoPane = width > 700.dp
    val immersive = width < 500.dp
    val local = LocalDensity.current
    val density = when{
        // Big screen
        wRange > Range.xLarge && hRange > Range.Large -> local.density * 1.4f
        wRange > Range.Medium && hRange > Range.Medium -> local.density * 1.2f
        else -> local.density
    }
    //val new = lerp(density, density )
    Log.d(TAG, "Library: w:$width h:$height")
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    CompositionLocalProvider(
        LocalDensity provides Density(local.density, local.fontScale)
    ) {
        Layout(
            topBar = {
                CarousalAppBar(
                    state = state,
                    behaviour = topAppBarScrollBehavior,
                    modifier = if (immersive) Modifier
                    else Modifier
                        .padding(padding)
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
                    title = /*res.getString(R.string.library_recent)*/ "History",
                    subtitle = stringResource(R.string.library_recent_tag_line),
                    onMoreClick = { navigator.navigate(Members.direction(Playback.PLAYLIST_RECENT)) },
                    contentPadding = padding
                )
                RecentlyPlayedList(
                    state,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = padding
                )

                // Show Banner if not AdFree.
                val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
                if (!purchase.purchased)
                    Banner(
                        placementID = BuildConfig.PLACEMENT_BANNER_1,
                        modifier = Modifier.padding(padding)
                    )

                Header(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Fresh Arrivals",
                    subtitle = stringResource(R.string.library_recently_added_tag_line),
                    onMoreClick = {
                        navigator.navigate(
                            Audios.direction(
                                Audios.GET_EVERY,
                                order = GroupBy.DateModified,
                                ascending = false
                            )
                        )
                    },
                    contentPadding = padding
                )

                NewlyAddedList(
                    state,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = padding
                )
            },
            details = {
                val movable = remember {
                    movableContentOf {
                        // Shortcuts.
                        val background = if (isTwoPane) Modifier
                            .background(Material.colors.overlay.compositeOver(Material.colors.background))
                        else
                            Modifier
                        Header(
                            modifier = Modifier
                                .then(background)
                                .fillMaxWidth(),
                            title = stringResource(R.string.library_shortcuts),
                            subtitle = stringResource(R.string.library_shortcuts_tag_line),
                            style = if (isTwoPane) Material.typography.body1 else Material.typography.h5,
                            contentPadding = if (isTwoPane) PaddingValues(horizontal = ContentPadding.normal) else DefaultContentPadding
                        )
                        Shortcuts(
                            Modifier
                                .padding(ContentPadding.normal)
                                .fillMaxWidth(),
                        )
                    }
                }
                when (isTwoPane) {
                    false -> movable()
                    else -> Surface(
                        modifier = Modifier
                            .systemBarsPadding()
                            .padding(DefaultContentPadding),
                        // Use the outline color as the border stroke or null based on the lightness
                        // of the material colors
                        border = BorderStroke(0.2.dp, Material.colors.onBackground.copy(0.5f)),
                        // Use the overlay color or the background color based on the lightness of
                        // the material colors
                        color = Color.Transparent,
                        // Use the ContentShape as the shape of the surface
                        shape = RoundedCornerShape(7),
                        contentColor = Material.colors.onBackground,
                        content = {
                            Column(content = { movable() })
                        },
                    )
                }
            },
            modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            offset = if (!isTwoPane) Dp.Unspecified else (width * 0.25f).coerceIn(360.dp, 500.dp),
        )
    }
}

