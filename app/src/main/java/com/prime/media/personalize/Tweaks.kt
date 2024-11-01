package com.prime.media.personalize

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.prime.media.R
import com.prime.media.common.preference
import com.prime.media.settings.Settings
import com.primex.core.textResource
import com.primex.material2.DropDownMenuItem
import com.zs.core_ui.AppTheme
import com.primex.core.rememberVectorPainter as Painter


@Composable
fun Tweaks(
    state: PersonalizeViewState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val useAccent by preference(Settings.USE_ACCENT_IN_NAV_BAR)
        DropDownMenuItem(
            textResource(R.string.scr_personalize_accent_nav),
            onClick = { state[Settings.USE_ACCENT_IN_NAV_BAR] = !useAccent },
            icon = Painter(
                if (useAccent) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                tintColor = AppTheme.colors.accent
            ),
        )
    }
}