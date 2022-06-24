package com.prime.player


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.prime.player.extended.*
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.flipNewInstall
import kotlinx.coroutines.launch


private const val TOTAL_PAGES = 4

@Composable
fun Intro() {

    var currentPage by androidx.compose.runtime.remember {
        mutableStateOf(0)
    }

    val colorBg by animateColorAsState(
        targetValue = when (currentPage) {
            0 -> Color.OrientRed
            1 -> Color.Amber
            2 -> Color.SkyBlue
            3 -> Color.MetroGreen
            else -> Color.MetroGreen
        },
        animationSpec = tween(durationMillis = Anim.DURATION_MEDIUM)
    )

    Frame(
        modifier = Modifier.fillMaxSize(),
        color = colorBg,
        contentColor = Color.White
    ) {
        //Content
        Column(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = currentPage,
                modifier = Modifier
                    .padding(Padding.LARGE)
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> Welcome()
                    1 -> Info1()
                    2 -> Info2()
                    3 -> Info3()
                }
            }

            //Controls
            Row(
                modifier = Modifier
                    .padding(horizontal = Padding.LARGE)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentPage -= 1
                }, enabled = currentPage > 0) {
                    Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = null)
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalIndicator(
                    count = TOTAL_PAGES,
                    current = currentPage,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(ContentAlpha.disabled),
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    currentPage += 1
                }, enabled = currentPage < TOTAL_PAGES - 1) {
                    Icon(imageVector = Icons.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun Welcome() {
    Column(modifier = Modifier.fillMaxSize()) {
        Content(
            imageVector = Icons.Outlined.Person,
            header = stringResource(id = R.string.welcome),
            description = stringResource(
                id = R.string.intro_welcome_msg
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun Info1() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))
        Content(
            imageVector = Icons.Outlined.LightMode,
            header = stringResource(R.string.light_dark_mode),
            description = stringResource(id = R.string.light_dark_summery)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun Info2() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))
        Content(
            imageVector = Icons.Outlined.Settings,
            header = stringResource(R.string.settings),
            description = stringResource(R.string.settings_summery)
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Info3() {
    val storagePermission =
        rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val rationale = memorize {
        PermissionRationale(state = storagePermission) {
            hide()
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.weight(1f))
        Content(
            imageVector = Icons.Outlined.SdStorage,
            header = stringResource(id = R.string.one_last_thing),
            description = stringResource(
                if (storagePermission.hasPermission)
                    R.string.storage_access_available
                else
                    R.string.one_last_thing_summery
            )
        )

        OutlinedButton(
            onClick = {
                when {
                    storagePermission.hasPermission -> scope.launch {
                        Preferences.get(context).flipNewInstall()
                    }
                    !storagePermission.permissionRequested -> storagePermission.launchPermissionRequest()
                    storagePermission.shouldShowRationale -> rationale.show()
                }
            },
            border = BorderStroke(1.5.dp, Color.White),
            modifier = Modifier
                .padding(top = Padding.EXTRA_LARGE)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White
            ),
        ) {
            Label(
                text = stringResource(
                    if (storagePermission.hasPermission) R.string.lets_go else R.string.allow_access
                )
            )
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = "permission",
                modifier = Modifier.padding(start = Padding.LARGE)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRationale(state: PermissionState, onDismissRequest: () -> Unit) {
    AlertDialog(
        title = stringResource(R.string.permission_rationale),
        message = stringResource(id = R.string.one_last_thing_summery_force)
    ) { ask ->
        if (ask)
            state.launchPermissionRequest()
        onDismissRequest()
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ColumnScope.Content(
    imageVector: ImageVector,
    header: String,
    description: String
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier
            .heightIn(max = 256.dp)
            .aspectRatio(1f)
            .weight(1f)
            .align(Alignment.CenterHorizontally)
    )

    AnimateVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(
            animationSpec = tween(Anim.DURATION_LONG)
        ),
        initiallyVisible = false
    ) {
        Text(
            text = header,
            style = PlayerTheme.typography.h3,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = Padding.MEDIUM)
        )
    }

    AnimateVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(
            animationSpec = tween(Anim.DURATION_LONG)
        ),
        initiallyVisible = false
    ) {
        Text(
            text = description,
            style = PlayerTheme.typography.body2,
        )
    }
}