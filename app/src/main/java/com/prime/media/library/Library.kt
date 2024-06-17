@file:Suppress("CrossfadeLabel", "FunctionName")
@file:OptIn(ExperimentalTextApi::class, ExperimentalTextApi::class)

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
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.material.icons.twotone.Settings
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
import com.prime.media.core.billing.Banner
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Artwork
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
import com.prime.media.settings.Settings
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

private val Colors.topBar
    @Composable inline get() = primary.blend(background, 0.96f)

private val Colors.border
    @Composable inline get() = BorderStroke(0.2.dp, primary.copy(0.3f))

private val TelegramIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://t.me/audiofy_support")
}

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
        modifier = modifier.clipToBounds()
    ) {
        // Background with image representation and gradient
        val id by state.carousel.collectAsState()
        val colors = Material.colors
        val gradient =
            Brush.verticalGradient(colors = listOf(Color.Transparent, colors.background))
        // Background
        val curtain =
            lerp(colors.topBar, Color.Transparent, fraction)
        Crossfade(
            targetState = Repository.toAlbumArtUri(id ?: 0),
            animationSpec = tween(4_000),
            modifier = Modifier
                .visualEffect(ImageBrush.NoiseBrush, alpha = 0.35f, true)
                .foreground(curtain)
                .parallax(0.2f)
                .layoutId(TopAppBarDefaults.LayoutIdBackground)
                .fillMaxSize(),
            content = { value ->
                Artwork(
                    modifier = Modifier
                        .foreground(Color.Black.copy(0.2f))
                        .fillMaxSize(),
                    data = value,
                )
            }
        )

        // FixMe - The gradient is also enlarged by parallax and hence this.
        Box(
            modifier = Modifier
                .alpha(lerp(0f, 1f, fraction))
                .foreground(gradient)
                .fillMaxSize()
        )

        // Navigation Icon.
        val contentColor = lerp(LocalContentColor.current, Color.White, fraction)
        val navController = LocalNavController.current
        IconButton(
            onClick = { navController.navigate(AboutUs.route) },
            painter = rememberVectorPainter(image = Icons.Outlined.Info),
            contentDescription = "about us",
            modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdNavIcon).pulsate(),
            tint = contentColor
        )

        // Actions  (Buy and settings)
        val provider = LocalSystemFacade.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdAction),
            content = {
                // Buy full version button.
                val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
                if (!purchase.purchased)
                    OutlinedButton(
                        label = textResource(R.string.library_ads),
                        onClick = { provider.launchBillingFlow(BuildConfig.IAP_NO_ADS) },
                        icon = painterResource(id = R.drawable.ic_remove_ads),
                        modifier = Modifier.scale(0.75f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = contentColor.copy(0.12f),
                            contentColor = contentColor
                        ),
                        shape = CircleShape
                    )
                // Support
                val ctx = LocalContext.current
                IconButton(
                    imageVector = Icons.Outlined.SupportAgent,
                    onClick = { ctx.startActivity(TelegramIntent) },
                    modifier = Modifier,
                    tint = contentColor
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

/**
 * Padding values with all sides set to 0.
 */
private val ZeroPadding = PaddingValues(0.dp)

private val LargeListItemArrangement = Arrangement.spacedBy(16.dp)
private val NormalRecentItemArrangement = Arrangement.spacedBy(8.dp)

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
    horizontalArrangement: Arrangement.Horizontal = NormalRecentItemArrangement,
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
                message = stringResource(id = R.string.loading)
            )
            // Empty state
            1 -> Placeholder(
                iconResId = R.raw.lt_empty_box,
                title = "",
                message = stringResource(id = R.string.empty)
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
    val colors = Material.colors
    val color =
        if (!Material.colors.isLight) colors.onBackground.copy(0.50f) else colors.primary.copy(0.5f)
    Box(
        modifier = modifier
            .clip(FolderShape) // Shape the shortcut like a folder
            // .background(colors.primary.copy(0.035f), FolderShape)
            .border(1.dp, color, FolderShape) // Light border
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
        horizontalArrangement = NormalRecentItemArrangement,
        verticalArrangement = NormalRecentItemArrangement,
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
 * The shape of the recent icon.
 */
private val RECENT_ICON_SHAPE = RoundedCornerShape(30)


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
                .size(66.dp) // Adjust size if needed
                .border(2.dp, Color.White, RECENT_ICON_SHAPE) // Add white border
                .shadow(ContentElevation.low, RECENT_ICON_SHAPE) // Add subtle shadow
                .background(Material.colors.surface)
        )

        // Label below the artwork with padding and styling
        Text(
            text = label,
            modifier = Modifier
                .padding(top = ContentPadding.medium)
                .width(80.dp),
            style = Material.typography.caption2,
            maxLines = 2, // Allow at most 2 lines for label
            textAlign = TextAlign.Center,
            minLines = 2
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
        horizontalArrangement = NormalRecentItemArrangement,
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
            .shadow(ContentElevation.low, Material.shapes.small2) // Light shadow
            .clickable(onClick = onClick) // Enable clicking
            .size(224.dp, 132.dp), // Set minimum size
        contentAlignment = Alignment.Center // Center content within the box
    ) {
        val colors = listOf(
            Material.colors.primary.blend(Color.Black, 0.3f), // Gradient start: transparent primary
         //   Color.Transparent, // Gradient middle: transparent
            Color.Transparent, // Gradient end: transparent
        )

        // Image with horizontal gradient overlay
        Artwork(
            data = imageUri,
            alignment = alignment,
            modifier = Modifier
                .visualEffect(ImageBrush.NoiseBrush, 0.3f, true)
                .foreground(Brush.horizontalGradient(colors)) // Apply transparent-to-primary gradient
                .background(Material.colors.surface)
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

/**
 * A Composable function that displays a list of newly added items.
 *
 * @param state The state of the library.
 * @param modifier The modifier to be applied to the list.
 * @param contentPadding The padding to be applied to the list.
 */
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
        horizontalArrangement = LargeListItemArrangement,
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
    text: CharSequence,
    modifier: Modifier = Modifier,
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


//private val StandardDensity = 2.7875001f
@Composable
fun Library(
    state: Library
) {
    val (width, height) = LocalWindowSize.current.value
    val isTwoPane = width > 700.dp
    // Define the scrollBehaviour to be used in topAppBar.
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Layout(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        // if this is a two-pane layout, the details will take 25% of total width (360dp - 500dp)
        offset = if (!isTwoPane) Dp.Unspecified else (width * 0.25f).coerceIn(360.dp, 500.dp),
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
            // What's new
            Header(
                modifier = Modifier.fillMaxWidth(),
                text = textResource(R.string.library_what_s_new),
                contentPadding = DefaultContentPadding
            )

            Promotions(
                padding = DefaultContentPadding,
            )
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
                    placementID = BuildConfig.PLACEMENT_BANNER_1,
                    modifier = Modifier.padding(DefaultContentPadding)
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



