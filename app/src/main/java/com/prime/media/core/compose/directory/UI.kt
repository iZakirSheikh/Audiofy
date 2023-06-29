package com.prime.media.core.compose.directory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.prime.media.*
import com.prime.media.R
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.util.DateUtil
import com.prime.media.core.compose.*
import com.prime.media.core.util.formatAsRelativeTimeSpan
import com.prime.media.impl.Mapped
import com.prime.media.impl.isSimple
import com.primex.core.*
import com.primex.material2.*
import com.primex.material2.neumorphic.NeumorphicTopAppBar

/**
 * The visual representation of the [Action] as a DropDownMenu item.
 *
 * @param value The action to display.
 * @param modifier A modifier to apply to the action composable.
 * @param checked Indicates whether the action is checked or not.
 * @param enabled Indicates whether the action is enabled or not.
 * @param onAction A function that is called when the action is performed.
 *
 * Usage:
 * ```
 * DropDownMenu(
 *     toggle = { /* toggle menu */ },
 *     dropDownModifier = Modifier.padding(16.dp),
 *     expanded = state.expanded,
 *     dropDownContent = {
 *         Action(
 *             value = Action.Download,
 *             checked = state.checked,
 *             onAction = { /* handle action */ }
 *         )
 *         // other actions
 *     }
 * )
 * ```
 *
 * @author Zakir Sheikh
 * @since 2.0
 */
@Composable
@NonRestartableComposable
private fun Action(
    value: Action,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = !checked,
    onAction: () -> Unit
) {
    val color = if (checked) Theme.colors.primary else LocalContentColor.current
    CompositionLocalProvider(
        LocalContentColor provides color,
        LocalContentAlpha provides ContentAlpha.high
    ) {
        DropDownMenuItem(
            title = stringResource(value = value.title),
            onClick = onAction,
            leading = rememberVectorPainter(value.icon),
            enabled = enabled,
            modifier = Modifier
                .sizeIn(minWidth = 180.dp)
                .then(
                    if (checked)
                        Modifier.drawBehind {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        color.copy(Theme.CONTAINER_COLOR_ALPHA),
                                        Color.Transparent
                                    )
                                )
                            )
                            drawRect(color = color, size = size.copy(width = 4.dp.toPx()))
                        }
                    else
                        Modifier
                )
                .then(modifier)
        )
    }
}

private val TopBarShape = CircleShape

/**
 * A composable that represents the toolbar for the directory.
 *
 * The toolbar shows the following actions:
 * 1. Navigation button (back button)
 * 2. Directory name
 * 3. Search view toggle
 * 4. View type toggle
 * 5. Group by
 * 6. More actions as defined in the `mActions` of the `viewModel`
 *
 * @param resolver The view model for the directory.
 * @param onAction A function that is called when an action is performed.
 * @param modifier A modifier to apply to the toolbar composable.
 *
 * Usage:
 * ```
 * Toolbar(
 *     resolver = viewModel,
 *     onAction = { action -> /* handle action */ },
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 *
 * @author Zakir Sheikh
 * @since 2.0
 */
@Composable
private fun <T : Any> Toolbar(
    resolver: Directory<T>,
    onAction: (action: Action) -> Unit,
    modifier: Modifier = Modifier
) {
    //shown only if list meta is simple.
    val title = stringResource(value = resolver.meta?.title) ?: AnnotatedString("")
    // the actual content
    NeumorphicTopAppBar(
        shape = TopBarShape,
        elevation = ContentElevation.low,
        modifier = modifier.padding(top = ContentPadding.medium),
        lightShadowColor = Theme.colors.lightShadowColor,
        darkShadowColor = Theme.colors.darkShadowColor,
        // The label must not fill width
        // this will surely make the look and feel of the app bad.
        title = {
            Label(
                text = title,
                modifier = Modifier.padding(end = ContentPadding.normal),
                maxLines = 2
            )
        },

        // The Toolbar will surely require navigate to back.
        navigationIcon = {
            val navigator = LocalNavController.current
            IconButton(
                // remove focus else navigateUp
                onClick = {
                    if (resolver.focused.isNotBlank())
                        resolver.focused = ""
                    else
                        navigator.navigateUp()
                },
                imageVector = Icons.Outlined.ReplyAll,
                contentDescription = null
            )
        },
        // paint the main actions in the toolbar
        // check which should be disabled if selected > 0
        actions = {
            // search
            // toggles on and off the search
            // the search turns on off as soon as you push null or empty string in it.
            val filter by resolver.filter.collectAsState()
            IconButton(
                onClick = { resolver.filter(if (filter.second == null) "" else null) },
                imageVector = if (filter.second == null) Icons.Outlined.Search else Icons.Outlined.SearchOff,
                contentDescription = null,
                enabled = true
            )
            // viewType
            // toggle between the different viewTypes.
            // maybe show message in toggleViewType corresponding messages.
            val viewType = resolver.viewType
            IconButton(
                onClick = { resolver.toggleViewType() },
                imageVector = viewType.icon,
                contentDescription = null,
                enabled = true
            )

            // SortBy
            var showOrderMenu by rememberState(initial = false)
            IconButton(onClick = { showOrderMenu = true }) {
                Icon(imageVector = Icons.Outlined.Sort, contentDescription = null)
                val actions = resolver.orders
                DropdownMenu(
                    expanded = showOrderMenu,
                    onDismissRequest = { showOrderMenu = false }
                ) {
                    // ascending descending logic
                    val ascending = filter.third
                    CompositionLocalProvider(
                        LocalContentColor provides Theme.colors.primary
                    ) {
                        DropDownMenuItem(
                            title = "Ascending",
                            onClick = {
                                resolver.filter(ascending = !ascending); showOrderMenu = false
                            },
                            leading = rememberVectorPainter(
                                image = if (ascending) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked
                            ),
                        )
                    }

                    Divider()
                    // The different sort-orders.
                    // TODO: Highlight the current selected action.
                    // also disable the click on current if it is selected.
                    actions.forEach {
                        val checked = filter.first.id == it.id
                        Action(value = it, checked = checked) {
                            resolver.filter(order = it); showOrderMenu = false
                        }
                    }
                }
            }

            // The other actions/Main actions.
            // main actions excluding first as it will be shown as fab
            // only show if it is > 1 because fab exclusion.
            val actions = resolver.mActions
            val from = if (resolver.meta?.isSimple != false) 1 else 3
            if (actions.size > from) {
                var showActionMenu by rememberState(initial = false)
                IconButton(onClick = { showActionMenu = true }) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                    // more options show in both cases if something is selected or not.
                    // if nothing is selected given condition will perform the action on all items
                    // else the selected ones.
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        for (i in from until actions.size) {
                            val action = actions[i]
                            // don't include null.
                            if (action != null)
                                Action(action) {
                                    onAction(action)
                                    showActionMenu = false
                                }
                        }
                    }
                }
            }
        }
    )
}

/**
 * A composable that represents the action bar for the directory.
 *
 * The action bar shows the actions available for the selected items in the directory.
 * It is only shown when the selected items count in the [Directory] is greater than 0.
 * * The navigation button is a cross that clears the selection.
 * * The count displays the number of selected items.
 * * The `action1` and `action2` are the first 2 actions from the actions list in [Directory].
 * * and more is only shown when actions.size > 2
 *
 * @param resolver The view model for the directory.
 * @param modifier A modifier to apply to the action bar composable.
 * @param onAction A function that is called when an action is performed.
 *
 * Usage:
 * ```
 * ActionBar(
 *     resolver = viewModel,
 *     onAction = { action -> /* handle action */ },
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 *
 * @author Zakir Sheikh
 * @since 2.0
 */
@Composable
private fun <T : Any> ActionBar(
    resolver: Directory<T>,
    modifier: Modifier = Modifier,
    onAction: (action: Action) -> Unit
) {
    NeumorphicTopAppBar(
        title = {
            val count = resolver.selected.size
            Label(text = "$count selected")
        },
        modifier = modifier.padding(top = ContentPadding.medium),
        elevation = ContentElevation.low,
        shape = TopBarShape,
        contentColor = Theme.colors.primary,

        lightShadowColor = Theme.colors.lightShadowColor,
        darkShadowColor = Theme.colors.darkShadowColor,
        // here the navigation icon is the clear button.
        // clear selection if selected > 0
        navigationIcon = {
            IconButton(
                onClick = { resolver.clear() },
                imageVector = Icons.Outlined.Close,
                contentDescription = null
            )
        },
        // the action that needs to be shown
        // when selection is > 0
        actions = {
            val actions = resolver.actions
            //first action.
            val first = actions.getOrNull(0)
            if (first != null)
                IconButton(
                    onClick = { onAction(first) },
                    imageVector = first.icon,
                    contentDescription = null
                )
            // second action.
            val second = actions.getOrNull(1)
            if (second != null)
                IconButton(
                    onClick = { onAction(second) },
                    imageVector = second.icon,
                    contentDescription = null,
                )
            // The remain actions.
            // only show if it is > 2
            if (actions.size > 2) {
                var showActionMenu by rememberState(initial = false)
                IconButton(onClick = { showActionMenu = true }) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                    // more options show in both cases if something is selected or not.
                    // if nothing is selected given condition will perform the action on all items
                    // else the selected ones.
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        for (i in 2 until actions.size) {
                            val action = actions[i]
                            // don't include null.
                            Action(value = action) { onAction(action); showActionMenu = false }
                        }
                    }
                }
            }
        }
    )
}

/**
 * A composable that represents the top app bar for the directory.
 *
 * The top app bar displays either the toolbar or the action bar based on whether selected is greater
 * than 0 in [Directory].
 */
@Composable
private fun <T : Any> TopAppBar(
    resolver: Directory<T>,
    onAction: (action: Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showActionBar by remember { derivedStateOf { resolver.selected.isNotEmpty() } }
    // show/ hide action bar
    Crossfade(
        targetState = showActionBar,
        modifier = modifier
    ) { show ->
        when (show) {
            true -> ActionBar(resolver, onAction = onAction)
            else -> Toolbar(resolver, onAction = onAction)
        }
    }
}

/**
 * Item header.
 * //TODO: Handle padding in parent composable.
 */
@Composable
private fun Header(
    value: Text,
    modifier: Modifier = Modifier
) {
    val title = stringResource(value = value)
    if (title.isBlank())
        return Spacer(modifier = modifier)

    val color = LocalContentColor.current
    Crossfade(
        targetState = title.length == 1,
        modifier = Modifier
            .padding(bottom = ContentPadding.normal)
            .drawHorizontalDivider(color = color)
            .fillMaxWidth()
            .then(modifier)
    ) { single ->
        when (single) {
            // draw a single char/line header
            // in case the length of the title string is 1
            true -> Header(
                text = title,
                style = Theme.typography.h3,
                fontWeight = FontWeight.Normal,
                color = color,
                modifier = Modifier
                    .padding(top = ContentPadding.normal)
                    .padding(horizontal = ContentPadding.xLarge),
            )
            // draw a multiline line header
            // in case the length of the title string is 1
            else -> Label(
                text = title,
                color = color,
                maxLines = 2,
                fontWeight = FontWeight.Normal,
                style = Theme.typography.h4,
                modifier = Modifier
                    // don't fill whole line.
                    .fillMaxWidth(0.7f)
                    .padding(top = ContentPadding.xLarge, bottom = ContentPadding.medium)
                    .padding(horizontal = ContentPadding.normal)
            )
        }
    }
}

private val HeaderArtWorkShape = RoundedCornerShape(20)

/**
 * A composable that display the [value] of the list of directory.
 *
 * @param value The metadata for an item in the directory.
 * @param modifier A modifier to apply to the metadata composable.
 *
 * Usage:
 * ```
 * Metadata(
 *     value = itemMetadata,
 *     modifier = Modifier.padding(8.dp)
 * )
 * ```
 */
@Composable
private fun <T : Any> Metadata(
    resolver: Directory<T>,
    onPerformAction: (action: Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    // return spacer is meta is null
    val meta = resolver.meta ?: return Spacer(modifier = modifier)
    // What is the meta?
    // A MetaData provides additional info regarding the directory.
    // The 2nd two slots of mActions are filled by this.
    // TODO: Future versions might animate between vertical/horizontal.
    ConstraintLayout(modifier) {
        val (Artwork, Title, Cardinality, Date, Action1, Action2, Divider) = createRefs()

        // create the chain
        // This will determine the size of the MetaData Composable.
        constrain(
            ref = createVerticalChain(Artwork, Action1, chainStyle = ChainStyle.Packed),
            constrainBlock = {
                // Divider will act as the center anchor of Play Button
                top.linkTo(parent.top, ContentPadding.normal)
                bottom.linkTo(parent.bottom, ContentPadding.xLarge)
            }
        )


        // Artwork.
        // Because this composable only is hown when artwork isn't null; so
        Image(
            data = meta.artwork ?: "",
            modifier = Modifier
                .shadow(ContentElevation.high, HeaderArtWorkShape)
                .constrainAs(Artwork) {
                    start.linkTo(parent.start, ContentPadding.normal)
                    width = Dimension.value(76.dp)
                    height = Dimension.ratio("0.61")
                }
        )

        // Title
        // since meta is Text hence annotated string can be used to populate subtitle.
        Header(
            text = stringResource(value = meta.title),
            style = Theme.typography.h4,
            maxLines = 2,
            textAlign = TextAlign.Start,

            modifier = Modifier.constrainAs(Title) {
                start.linkTo(Artwork.end, ContentPadding.normal)
                end.linkTo(parent.end, ContentPadding.normal)
                top.linkTo(Artwork.top)
                width = Dimension.fillToConstraints
            }
        )

        // line 3 of details
        constrain(
            ref = createHorizontalChain(
                Cardinality,
                Divider,
                Date,
                chainStyle = ChainStyle.SpreadInside
            ),
            constrainBlock = {
                start.linkTo(Artwork.end, ContentPadding.normal)
                end.linkTo(parent.end, ContentPadding.normal)
            }
        )

        //Tracks
        val count = meta.cardinality
        val color = LocalContentColor.current
        Text(
            modifier = Modifier.constrainAs(Cardinality) {
                top.linkTo(Title.bottom, ContentPadding.normal)
                width = Dimension.percent(0.20f)
            },
            text = buildAnnotatedString {
                append("$count\n")
                withStyle(
                    SpanStyle(
                        color = color.copy(ContentAlpha.medium),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        baselineShift = BaselineShift(-0.7f)
                    )
                ) {
                    append("Files")
                }
            },
            textAlign = TextAlign.Center,
            style = Theme.typography.h6,
            fontWeight = FontWeight.SemiBold,
        )

        //Divider 2
        Divider(
            modifier = Modifier.constrainAs(Divider) {
                height = Dimension.value(56.dp)
                top.linkTo(Cardinality.top, -ContentPadding.small)
                width = Dimension.value(1.dp)
            }
        )

        val date =
            if (meta.dateModified == -1L) "N/A" else DateUtil.formatAsRelativeTimeSpan(meta.dateModified)
        Text(
            text = buildAnnotatedString {
                append("$date\n")
                withStyle(
                    SpanStyle(
                        color = color.copy(ContentAlpha.medium),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        baselineShift = BaselineShift(-0.7f)
                    )
                ) {
                    append("Last Updated")
                }
            },
            textAlign = TextAlign.Center,
            style = Theme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.constrainAs(Date) {
                top.linkTo(Cardinality.top)
                width = Dimension.fillToConstraints
            },
        )

        val actions = resolver.mActions
        val second = actions.getOrNull(2)
        if (second != null)
            OutlinedButton(
                label = stringResource(value = second.title),
                onClick = { onPerformAction(second) },
                leading = rememberVectorPainter(image = second.icon),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                border = ButtonDefaults.outlinedBorder,
                modifier = Modifier.constrainAs(Action2) {
                    bottom.linkTo(Action1.bottom)
                    end.linkTo(Action1.start, ContentPadding.small)
                    start.linkTo(Artwork.start, ContentPadding.medium)
                    width = Dimension.fillToConstraints
                },
                contentPadding = PaddingValues(11.dp),
                shape = Theme.shapes.small2
            )

        val first = actions.getOrNull(1)
        if (first != null)
            Button(
                label = stringResource(value = first.title),
                onClick = { onPerformAction(first) },
                leading = rememberVectorPainter(image = first.icon),
                modifier = Modifier
                    .padding(top = ContentPadding.xLarge)
                    .constrainAs(Action1) {
                        end.linkTo(parent.end, ContentPadding.xLarge)
                        if (second != null)
                            start.linkTo(Action2.end, ContentPadding.small)
                        else
                            start.linkTo(Artwork.start, ContentPadding.medium)
                        width = Dimension.fillToConstraints
                    },
                contentPadding = PaddingValues(9.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 0.dp
                ),
                shape = Theme.shapes.small2
            )
    }
}

// recyclable items.
private const val CONTENT_TYPE_HEADER = "_header"
private const val CONTENT_TYPE_LIST_META = "_list_meta"
private const val CONTENT_TYPE_LIST_ITEM = "_list_item"
private const val CONTENT_TYPE_BANNER = "_banner_ad"
private const val CONTENT_TYPE_SEARCH_VIEW = "_search_view"
private const val CONTENT_TYPE_PINNED_SPACER = "_pinned_spacer"

/**
 * SearchBar
 *
 * The SearchBar is shown when the toggle button on the Toolbar is activated. It is used to filter the items in the Directory.
 * The value from the SearchBar is directly emitted into the filter StateFlow.
 * It displays a search icon and a clear button and is built using the OutlineTextField component from Compose UI.
 *
 * @author Zakir Sheikh
 * @since 2.0.0
 */
@Composable
private fun <T : Any> SearchBar(
    resolver: Directory<T>,
    modifier: Modifier = Modifier
) {
    val query = resolver.filter.collectAsState().value.second
    OutlinedTextField(
        value = query ?: "",
        onValueChange = { resolver.filter(it) },
        modifier = modifier,
        leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null) },
        placeholder = { Label(text = "Type here to search!!") },
        label = { Label(text = "Search") },
        shape = Theme.shapes.small2,
        trailingIcon = {
            IconButton(onClick = { resolver.filter(if (!query.isNullOrBlank()) "" else null) }) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
            }
        },
    )
}

private val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) =
    { GridItemSpan(maxLineSpan) }

@OptIn(ExperimentalFoundationApi::class)
private inline fun <T> LazyGridScope.content(
    items: Mapped<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    // actual list
    items.forEach { (header, list) ->
        //emit  list header
        item(
            key = header.raw,
            contentType = CONTENT_TYPE_HEADER,
            span = fullLineSpan,
            content = {
                Header(
                    value = header,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        )

        // emit list of items.
        items(
            list,
            key = if (key == null) null else { item -> key(item) },
            contentType = { CONTENT_TYPE_LIST_ITEM },
            itemContent = { item ->
                itemContent(item)
            }
        )
    }
}

/**
 * A composable that displays a list of items as grid of cells.
 *
 * @param modifier The modifier to be applied to the list.
 * @param resolver The [Directory] for the list.
 * @param cells The number of cells per row.
 * @param key A key function to extract a unique identifier from the item.
 * @param itemContent The composable function that represents each item in the list.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> List(
    modifier: Modifier = Modifier,
    resolver: Directory<T>,
    cells: GridCells,
    key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAction: (action: Action) -> Unit,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    //TODO: Currently we are only representing the items. However, this logic will be moved
    //outside in the future.
    //Currently, we only support mapped data, but we aim to support paged data in the future."

    val data by resolver.data.collectAsState(initial = null) as State<Mapped<T>?>

    // The data can be in following cases:
    // case 1: data is null; means the initial loading state.
    // case 2: data is empty: means empty literary
    // case 3: plot the list.
    // also fade between the states.
    val state by remember {
        derivedStateOf {
            when {
                data == null -> 0 // initial loading
                data.isNullOrEmpty() -> 1// empty state
                else -> 2// normal state.
            }
        }
    }

    // plot the list
    val map = data ?: emptyMap()
    LazyVerticalGrid(
        columns = cells,
        modifier = modifier,
        contentPadding = LocalWindowPadding.current + contentPadding
    ) {
        // used to pin the list to top.
        // such data search bar is not opened in hiding.
        item(
            contentType = CONTENT_TYPE_PINNED_SPACER,
            span = fullLineSpan
        ) {
            Spacer(modifier = Modifier.padding(ContentPadding.small))
        }

        // list meta
        // it is debatable weather to use if inside or outside.
        item(
            contentType = CONTENT_TYPE_LIST_META,
            span = fullLineSpan
        ) {
            if (resolver.meta?.isSimple == false)
                Metadata(resolver = resolver, onPerformAction = onAction)
        }

        // Search Node
        item(
            contentType = CONTENT_TYPE_SEARCH_VIEW,
            span = fullLineSpan,
        ) {
            val filter by resolver.filter.collectAsState()
            val show = filter.second != null
            // animate visibility based on show is
            AnimatedVisibility(
                show,
                content = {
                    SearchBar(
                        resolver = resolver,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = ContentPadding.xLarge,
                                vertical = ContentPadding.medium,
                            ),
                    )
                },
            )
        }

        // actual content
        content(map, key, itemContent)

        // show placeholders as required.
        item(span = fullLineSpan, contentType = "list_state") {
            when (state) {
                0 -> Placeholder(
                    title = "Loading",
                    iconResId = R.raw.lt_loading_dots_blue,
                    modifier = Modifier.wrapContentHeight()
                )
                1 -> Placeholder(
                    title = "Oops Empty!!",
                    iconResId = R.raw.lt_empty_box,
                    modifier = Modifier.wrapContentHeight()
                )
            }
        }
    }
}

/**
 * Represents the abstract directory.
 *
 * @param viewModel The view model associated with the directory.
 * @param cells The number of cells in the grid of the directory.
 * @param key A function that returns a unique key for each item in the directory.
 * @param onAction A function that is called when an action occurs.
 * @param itemContent A composable function that displays the content of each item in the directory.
 *
 * The reasons for abstracting the directory are as follows:
 * 1. To ensure that common features such as the toolbar, search query, order header, actions, etc.
 * are present in each directory.
 * 2. To ensure that a single type is represented by a single directory, making the logic simple and
 * easy. This also ensures that a single layout needs to be implemented among different directories,
 * with only the layout of the individual items needing to be changed.
 *
 * The directory supports the following features:
 *
 * - Toolbar
 * - ActionBar shown when selected items count is greater than 0
 * - SearchView: Shown when the query is non-null.
 * - Selection of ViewTypes.
 *
 * Note: The directory is only available in portrait mode. The data must be provided as either
 * a paged list (upcoming) or a map of `Text` to a list of `T`.
 *
 * Usage:
 * ```
 * Directory(
 *     viewModel = myViewModel,
 *     cells = GridCells.Square,
 *     key = { item -> item.id },
 *     onAction = { action -> /* handle action */ },
 *     itemContent = { item -> /* display item content */ }
 * )
 * ```
 *
 * @author Zakir Sheikh
 * @since 2.0
 *
 * @see [Directory]
 */
@Composable
fun <T : Any> Directory(
    viewModel: Directory<T>,
    cells: GridCells,
    key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAction: (action: Action) -> Unit,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit,
) {
    // collapse if expanded and
    // back button is clicked.
    // TODO: Maybe check if filter is null or not.
    BackHandler(viewModel.selected.isNotEmpty() || viewModel.focused.isNotBlank()) {
        if (viewModel.focused.isNotBlank())
            viewModel.focused = ""
        else if (viewModel.selected.isNotEmpty())
            viewModel.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                resolver = viewModel,
                onAction = onAction,
                modifier = Modifier
                    .statusBarsPadding()
                    .drawHorizontalDivider(color = Theme.colors.onSurface)
                    .padding(bottom = ContentPadding.medium),
            )
        },
        // mainAction
        floatingActionButton = {
            // main action
            val action = viewModel.mActions.firstOrNull()
            if (action != null) {
                // actual content
                FloatingActionButton(
                    onClick = { onAction(action) },
                    shape = RoundedCornerShape(30),
                    modifier = Modifier.padding(LocalWindowPadding.current),
                    content = {
                        // the icon of the Fab
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null
                        )
                    }
                )
            }
        },
        content = {
            List(
                resolver = viewModel,
                cells = cells,
                modifier = Modifier.padding(it),
                contentPadding = contentPadding,
                itemContent = itemContent,
                key = key,
                onAction = onAction
            )
        }
    )
}