package com.prime.player.common.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.player.Material
import com.prime.player.Padding
import com.primex.ui.ProvideTextStyle

private val PLACE_HOLDER_ICON_BOX_SIZE = 192.dp

@Composable
fun VPlaceholder(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    message: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit),
) {
    Column(
        modifier = Modifier
            // optional full max size
            .fillMaxSize()
            // add a padding normal
            .padding(horizontal = Padding.Large, vertical = Padding.Normal)
            .then(modifier),
        // The Placeholder will be Placed in the middle of the available space
        // both h/v
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // place icon if available
        if (icon != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = Padding.Normal)
                    //  .align(Alignment.CenterHorizontally)
                    .requiredSize(PLACE_HOLDER_ICON_BOX_SIZE),
                propagateMinConstraints = true
            ) {
                icon()
            }
        }

        // Place Title
        ProvideTextStyle(
            style = Material.typography.h4,
            content = title,
            textAlign = TextAlign.Center
        )

        //Message
        if (message != null) {
            Spacer(modifier = Modifier.padding(vertical = Padding.Medium))
            ProvideTextStyle(
                style = Material.typography.body2,
                alpha = ContentAlpha.medium,
                content = message,
                textAlign = TextAlign.Center
            )
        }

        //Action
        if (action != null) {
            Spacer(modifier = Modifier.padding(vertical = Padding.Large))
            action()
        }
    }
}

@Composable
fun HPlaceholder(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    message: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit),
) {
    Row(
        modifier = Modifier
            // optional full max size
            .fillMaxSize()
            // add a padding normal
            .padding(horizontal = Padding.Large, vertical = Padding.Normal)
            .then(modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.weight(0.15f))

        Column(
            modifier = Modifier
                .padding(end = Padding.Large)
                .weight(0.7f, fill = false)
        ) {

            // Place Title
            ProvideTextStyle(
                value = Material.typography.h4,
                content = title
            )

            //Message
            if (message != null) {
                Spacer(modifier = Modifier.padding(vertical = Padding.Medium))
                ProvideTextStyle(
                    style = Material.typography.body2,
                    alpha = ContentAlpha.medium,
                    content = message
                )
            }

            //Action
            if (action != null) {
                Spacer(modifier = Modifier.padding(top = Padding.Large))
                action()
            }
        }

        // place icon if available
        if (icon != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = Padding.Normal)
                    //  .align(Alignment.CenterHorizontally)
                    .requiredSize(PLACE_HOLDER_ICON_BOX_SIZE),
                propagateMinConstraints = true
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@Composable
fun Placeholder(
    modifier: Modifier = Modifier,
    vertical: Boolean,
    icon: @Composable (() -> Unit)? = null,
    message: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit),
) {
    when (vertical) {
        true -> VPlaceholder(modifier, icon, message, action, title)
        else -> HPlaceholder(modifier, icon, message, action, title)
    }
}


