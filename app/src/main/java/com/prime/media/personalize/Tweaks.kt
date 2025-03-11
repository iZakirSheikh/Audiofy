package com.prime.media.personalize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BorderStyle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.preference
import com.prime.media.settings.Settings
import com.primex.core.textResource
import com.primex.material2.DropDownMenuItem
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.primex.core.rememberVectorPainter as Painter


private val SPACE = Arrangement.spacedBy(ContentPadding.medium)

@Composable
fun Tweaks(
    state: PersonalizeViewState,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        verticalArrangement = SPACE,
        horizontalArrangement = SPACE,
        content = {
            val colorNavBar by preference(Settings.USE_ACCENT_IN_NAV_BAR)
            Tweak(
                colorNavBar,
                "Navbar Accent Color",
                icon = Icons.Outlined.ColorLens,
                onClick = { state[Settings.USE_ACCENT_IN_NAV_BAR] = !colorNavBar }
            )

            val elevatedArtwork by preference(Settings.ARTWORK_ELEVATED)
            Tweak(
                elevatedArtwork,
                "Elevated Artwork",
                icon = Icons.Outlined.Shower,
                onClick = { state[Settings.ARTWORK_ELEVATED] = !elevatedArtwork }
            )

            val borderedArtwork by preference(Settings.ARTWORK_BORDERED)
            Tweak(
                borderedArtwork,
                "Artwork Border",
                icon = Icons.Outlined.BorderStyle,
                onClick = { state[Settings.ARTWORK_BORDERED] = !borderedArtwork}
            )
        }
    )
}

private val TweakShape = RoundedCornerShape(20)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Tweak(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .widthIn(max = 130.dp)
            .aspectRatio(1.6f),
        shape = TweakShape,
        color = AppTheme.colors.background(3.dp),
        contentColor = AppTheme.colors.onBackground,
        content = {
            Column(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = SPACE) {
                    if (icon != null)
                        Icon(icon, contentDescription = title)
                    Label(title, style = AppTheme.typography.caption, modifier = Modifier.weight(1f))
                }
                Switch(checked, onCheckedChange = null)
            }
        }
    )
}