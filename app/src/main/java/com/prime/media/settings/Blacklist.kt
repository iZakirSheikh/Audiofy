package com.prime.media.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.prime.media.common.Placeholder
import com.prime.media.common.util.PathUtils
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile

private const val TAG = "Blacklist"

@Composable
@NonRestartableComposable
private fun Item(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListTile(
        headline = { Label(text = title, fontWeight = FontWeight.Bold, style = com.zs.core_ui.AppTheme.typography.bodyMedium) },
        subtitle = { Label(text = subtitle) },
        leading = { Icon(imageVector = Icons.Outlined.FolderOff, contentDescription = null) },
        centerAlign = true,
        modifier = modifier,
        color = Color.Transparent,
        trailing = {
            IconButton(
                imageVector = Icons.Outlined.RemoveCircleOutline,
                onClick = onClick
            )
        },
    )
}

@Composable
private fun Blacklist(
    state: Blacklist,
    modifier: Modifier = Modifier
) {
    val list = state.values
    Crossfade(
        targetState = list.isNullOrEmpty(),
        modifier = modifier,
        label = "blacklist"
    ) { value ->
        when (value) {
            true -> Placeholder(
                title = "Empty",
                iconResId = R.raw.lt_empty_box,
                message = stringResource(R.string.blacklist_empty_desc)
            )
            // Show actual list
            else -> LazyColumn {
                list?.forEach { path ->
                    val name = PathUtils.name(path)
                    item {
                        val ctx = LocalContext.current
                        Item(title = name, subtitle = path, onClick = { state.unblock(path, ctx) })
                    }
                }
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun Toolbar(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Label(text = stringResource(id = R.string.pref_blacklist)) },
        modifier = modifier,
        actions = { IconButton(imageVector = Icons.Outlined.Close, onClick = onDismissRequest) },
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterListOff,
                contentDescription = null,
                modifier = Modifier.padding(ContentPadding.small)
            )
        },
    )
}

@Composable
@NonRestartableComposable
fun BlacklistDialog(
    expanded: Boolean,
    state: Blacklist,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = {
            Scaffold(
                topBar = { Toolbar(onDismissRequest = onDismissRequest) },
                content = {
                    Blacklist(state = state, modifier = Modifier.padding(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.60f) // different when width > height
                    .clip(com.zs.core_ui.AppTheme.shapes.compact),
            )
        }
    )
}