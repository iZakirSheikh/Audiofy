package com.prime.media.permission

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.Route
import com.prime.media.settings.Settings
import com.primex.core.textResource
import com.primex.material2.OutlinedButton
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.Range
import com.google.accompanist.permissions.rememberMultiplePermissionsState as Permissions


object RoutePermission : Route

/**
 * Represents the permission screen
 * @see REQUIRED_PERMISSIONS
 * @see RoutePermission
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Permission() {
    val permission = Permissions(permissions = Settings.REQUIRED_PERMISSIONS)
    val (width, _) = LocalWindowSize.current
    com.prime.media.common.Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.permission_screen_title),
        message = textResource(R.string.permission_screen_desc),
        vertical = width == Range.Compact
    ) {
        OutlinedButton(
            onClick = { permission.launchMultiplePermissionRequest() },
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
            label = stringResource(R.string.allow),
            border = ButtonDefaults.outlinedBorder,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        )
    }

    if (!permission.allPermissionsGranted)
        return
    val facade = LocalSystemFacade.current
    DataUsageDisclosureDialog({ facade.restart(true)})
}
