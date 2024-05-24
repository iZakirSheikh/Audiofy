package com.prime.media.library

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.purchase
import com.prime.media.small2
import com.primex.core.composableOrNull
import com.primex.core.stringResource
import com.primex.core.textResource
import com.primex.material2.IconButton
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.TextButton

private const val TAG = "Promotions"

private val PromotionShape = RoundedCornerShape(10)

@Composable
private inline fun Promotion(
    message: CharSequence,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    noinline buttons: @Composable() (() -> Unit)? = null
) {
    var collapsed by remember { mutableStateOf(true) }
    ListTile(
        headline = {
            Text(
                text = message,
                style = Material.typography.body1,
                maxLines = if (collapsed) 4 else 10,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leading = composableOrNull(icon != null) {
            Icon(
                imageVector = icon ?: Icons.Outlined.Info,
                contentDescription = null,
                tint = Material.colors.primary,
                modifier = Modifier.padding(vertical = ContentPadding.small)
            )
        },
        footer = buttons,
        trailing = {
            val degrees by animateFloatAsState(if (collapsed) 0f else 180f)
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer {
                    rotationZ = degrees
                }
            )
        },
        modifier = modifier
            .border(ButtonDefaults.outlinedBorder, PromotionShape)
            .clip(PromotionShape)
            .clickable { collapsed = !collapsed }
            .scale(0.92f)
            .animateContentSize(),
        shape = PromotionShape,
        color = Color.Transparent,
        onColor = Material.colors.onSurface,
    )
}

private val FeedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:helpline.prime.zs@gmail.com")
    putExtra(Intent.EXTRA_SUBJECT, "Feedback/Suggestion for Audiofy")
}
private val GitHubIssuesPage = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy/issues")
}
private val TelegramIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://t.me/audiofy_support")
}
private val GithubIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy")
}

@Composable
private inline fun JoinUs(
    modifier: Modifier = Modifier
) {
    Promotion(
        message = textResource(id = R.string.msg_library_join_us),
        icon = Icons.Outlined.SupportAgent,
        buttons = {
            Row(
                modifier = Modifier
                    .scale(0.80f)
                    .fillMaxWidth()
                    .offset(x = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium, Alignment.End),
            ) {
                val ctx = LocalContext.current
                IconButton(
                    imageVector = Icons.Outlined.AlternateEmail,
                    onClick = { ctx.startActivity(FeedbackIntent) },
                    modifier = Modifier
                    //      .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.DataObject,
                    onClick = { ctx.startActivity(GithubIntent) },
                    modifier = Modifier
                    //    .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.BugReport,
                    onClick = { ctx.startActivity(GitHubIssuesPage) },
                    modifier = Modifier
                    //   .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
                IconButton(
                    imageVector = Icons.Outlined.SupportAgent,
                    onClick = { ctx.startActivity(TelegramIntent) },
                    modifier = Modifier
                    //  .border(ButtonDefaults.outlinedBorder, CircleShape)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private inline fun RateUs(modifier: Modifier = Modifier) {
    val ctx = LocalSystemFacade.current
    Promotion(
        message = textResource(id = R.string.msg_library_rate_us),
        icon = Icons.Outlined.RateReview,
        buttons = {
            com.primex.material2.TextButton(
                label = textResource(id = R.string.rate_us).toString().uppercase(),
                onClick = { ctx.launchAppStore() },
                colors = ButtonDefaults.textButtonColors()
            )
        },
        modifier = modifier
    )
}

@Composable
private inline fun RemoveAds(
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    val purchase by purchase(BuildConfig.IAP_NO_ADS)
    Promotion(
        message = textResource(id = R.string.msg_ad_free_experience),
        icon = Icons.Outlined.RemoveCircleOutline,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    label = textResource(id = R.string.library_ads),
                    onClick = { facade.launchBillingFlow(BuildConfig.IAP_NO_ADS) },
                    colors = ButtonDefaults.textButtonColors(),
                    shape = Material.shapes.small2,
                    enabled = !purchase.purchased
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun BuyMeACoffee(modifier: Modifier = Modifier) {
    val facade = LocalSystemFacade.current
    val purchase by purchase(BuildConfig.IAP_BUY_ME_COFFEE)
    Promotion(
        message =  textResource(id = R.string.msg_library_buy_me_a_coffee),
        icon = Icons.Outlined.Coffee,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    label = stringResource(id = R.string.buy_me_a_coffee).uppercase(),
                    onClick = { facade.launchBillingFlow(BuildConfig.IAP_BUY_ME_COFFEE) },
                    colors = ButtonDefaults.textButtonColors(),
                    shape = Material.shapes.small2,
                    // This is always enabled; since a use can buy as many as they want and as many
                    // times as they want.
                   // enabled = !purchase.purchased
                )
            }
        },
        modifier = modifier
    )
}

private val ON_DEMAND_MODULE_CODEX = "codex"
private val CODEX_INSTALL_REQUEST = SplitInstallRequest.newBuilder().addModule(ON_DEMAND_MODULE_CODEX).build()
@Composable
private fun InstallCodex(modifier: Modifier = Modifier) {
    val facade = LocalSystemFacade.current
    val purchase by purchase(BuildConfig.IAP_CODEX)

    val context = LocalContext.current
    val manager = remember { SplitInstallManagerFactory.create(context) }

    val installed = remember {
        manager.installedModules.contains(ON_DEMAND_MODULE_CODEX)
    }

    val onClick = {
        // if the user has not purchased the feature; direct him to purchase the package.
        if (!purchase.purchased)
            facade.launchBillingFlow(BuildConfig.IAP_CODEX)
        else
            // TODO - Add success listener to this.
            manager.startInstall(CODEX_INSTALL_REQUEST)
        Unit
    }

    Promotion(
        message = textResource(id = R.string.msg_library_install_codex),
        icon = Icons.Outlined.NewReleases,
        modifier = modifier,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    label = stringResource(id = R.string.install).uppercase(),
                    onClick = onClick,
                    colors = ButtonDefaults.textButtonColors(),
                    shape = Material.shapes.small2,
                    enabled = !purchase.purchased || !installed
                )
            }
        }
    )
}

private const val PROMOTIONS_COUNT = 5

@Composable
fun Promotions(
    modifier: Modifier = Modifier,
    padding: PaddingValues
) {
    HorizontalPager(
        state = rememberPagerState { PROMOTIONS_COUNT },
        modifier = modifier,
        contentPadding = padding,
        pageSpacing = ContentPadding.normal
    ) { number ->
        when (number) {
            0 -> InstallCodex()
            1 -> BuyMeACoffee()
            2 -> RateUs()
            3 -> JoinUs()
            4 -> RemoveAds()
        }
    }
}
