package com.prime.player.directory

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.core.compose.Placeholder
import com.primex.core.*
import com.primex.ui.*

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
fun Action(
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
    resolver: DirectoryViewModel<T>,
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
            if (actions.size > 1) {
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
                        for (i in 1 until actions.size) {
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
 * It is only shown when the selected items count in the [DirectoryViewModel] is greater than 0.
 * * The navigation button is a cross that clears the selection.
 * * The count displays the number of selected items.
 * * The `action1` and `action2` are the first 2 actions from the actions list in [DirectoryViewModel].
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
    resolver: DirectoryViewModel<T>,
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
                            Action(value = action) { onAction(action) }
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
 * than 0 in [DirectoryViewModel].
 */
@Composable
private fun <T : Any> TopAppBar(
    resolver: DirectoryViewModel<T>,
    onAction: (action: Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showActionBar by remember { derivedStateOf { resolver.selected.isNotEmpty() } }
    val isSimple = resolver.meta?.isSimple ?: true
    // show/ hide action bar
    Crossfade(
        targetState = showActionBar,
        modifier = modifier
    ) { show ->
        when (show) {
            true -> ActionBar(resolver, onAction = onAction)
            // show spacer of zero size when not simple.
            else -> if (isSimple) Toolbar(
                resolver,
                onAction = onAction
            ) else Spacer(modifier = Modifier)
        }
    }
}

/**
 * Item header.
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
                    .padding(horizontal = ContentPadding.large),
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
                    .padding(top = ContentPadding.large, bottom = ContentPadding.medium)
                    .padding(horizontal = ContentPadding.normal)
            )
        }
    }
}

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
private fun Metadata(
    value: MetaData,
    modifier: Modifier = Modifier
) {
    //TODO: Built in future versions.
}

// recyclable items.
private const val CONTENT_TYPE_HEADER = "_header"
private const val CONTENT_TYPE_LIST_META = "_list_meta"
private const val CONTENT_TYPE_LIST_ITEM = "_list_item"
private const val CONTENT_TYPE_BANNER = "_banner_ad"

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
    resolver: DirectoryViewModel<T>,
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
 * @param resolver The [DirectoryViewModel] for the list.
 * @param cells The number of cells per row.
 * @param key A key function to extract a unique identifier from the item.
 * @param itemContent The composable function that represents each item in the list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> List(
    modifier: Modifier = Modifier,
    resolver: DirectoryViewModel<T>,
    cells: GridCells,
    key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
        item("pined_spacer", span = fullLineSpan) {
            Spacer(modifier = Modifier.padding(ContentPadding.small))
        }

        // The search bar; callable.
        item(
            "search_view",
            contentType = "search_view",
            span = fullLineSpan,
        ) {
            val filter by resolver.filter.collectAsState()
            val show = filter.second != null
            // animate visibility based on show is
            AnimatedVisibility(show) {
                SearchBar(
                    resolver = resolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = ContentPadding.large,
                            vertical = ContentPadding.medium,
                        ),
                )
            }
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
 * @see [DirectoryViewModel]
 */
@Composable
fun <T : Any> Directory(
    viewModel: DirectoryViewModel<T>,
    cells: GridCells,
    key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAction: (action: Action) -> Unit,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit,
) {
    // collapse if expanded and
    // back button is clicked.
    BackHandler(viewModel.selected.isNotEmpty() || viewModel.focused.isNotBlank()) {
        if (viewModel.focused.isNotBlank())
            viewModel.focused = ""
        if (viewModel.selected.isNotEmpty())
            viewModel.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                viewModel,
                onAction,
                Modifier
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
                key = key
            )
        }
    )
}


