package com.prime.media.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.common.Registry as Re
import com.prime.media.old.common.Artwork
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding

@Composable
fun App(
    value: App,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppTheme.shapes.compact)
            .clickable(onClick = onClick)
            .padding(ContentPadding.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Artwork(
            data = value.second,
            modifier = Modifier
                .shadow(4.dp, Re.mapKeyToShape(BuildConfig.IAP_ARTWORK_SHAPE_SQUIRCLE), true)
                .size(60.dp)
        )

        Label(
            text = value.first,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.caption,
            modifier = Modifier.width(56.dp)
        )
    }
}