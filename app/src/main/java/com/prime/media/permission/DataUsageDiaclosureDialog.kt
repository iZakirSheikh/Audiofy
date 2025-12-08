package com.prime.media.permission

import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Approval
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.settings.AppConfig
import com.prime.media.settings.Settings
import com.primex.core.textResource
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.OutlinedButton
import com.primex.material2.Text
import com.zs.core_ui.AlertDialog2
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.Range

@Composable
fun DataUsageDisclosureDialog(onDismissRequest: () -> Unit) {
    val facade = LocalSystemFacade.current
    val (width, _) = LocalWindowSize.current
    AlertDialog2(
        onDismissRequest = onDismissRequest,
        title = {
            Text(textResource(R.string.permission_scr_data_usage_disclosure_title))
        },
        actions = {
            IconButton(
                Icons.Outlined.Close,
                onClick = onDismissRequest
            )
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
        navigationIcon = {
            Icon(
                Icons.Outlined.Approval,
                contentDescription = null
            )
        },
        gravity = if (width == Range.Compact) Gravity.BOTTOM else Gravity.CENTER,
        content = {
            Text(
                textResource(R.string.permission_scr_data_usage_disclosure_msg)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.small),
                modifier = Modifier.align(
                    Alignment.End
                )
            ) {
                OutlinedButton("Decline", onClick = {
                    AppConfig.isQueryingAppPackagesAllowed = false
                    facade.setPreference(Settings.KEY_APP_CONFIG, AppConfig.stringify())
                    facade.restart(true)
                })
                Button("I Agree", onClick = {
                    AppConfig.isQueryingAppPackagesAllowed = true
                    facade.setPreference(Settings.KEY_APP_CONFIG, AppConfig.stringify())
                    facade.restart(true)
                })
            }
        }
    )
}