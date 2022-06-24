package com.prime.player.extended.managers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prime.player.R
import com.prime.player.extended.*
import com.prime.player.preferences.Preferences
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit


/**
 * An Update is a pair of [Release] on recommended available [Market].
 */
typealias Update = Pair<Release, Market>

/**
 *  A notifier class for updates.
 *  @param delay: if the user requested delay, The maximum delay allowed.
 */

class UpdateNotifier(
    private val delay: Long = DEFAULT_3_HOUR_DELAY,
    private val context: Context,
    private val listener: IUpdate
) {

    private val prefs = Preferences.get(context)

    /**
     * The time when user requested delay update. i.e., when he pressed remind me later.
     * default value -1
     */
    private var whenUpdateDelayed: Long
        get() = with(prefs) {
            getLong(KEY_WHEN_UPDATE_DELAYED, -1).collectBlocking()
        }
        set(value) {
            runBlocking {
                prefs.setLong(KEY_WHEN_UPDATE_DELAYED, value)
            }
        }

    /**
     * Saves the time in [KEY_WHEN_UPDATE_DELAYED], when user pressed **Remind me later**
     */
    fun onRequestAskLater() {
        whenUpdateDelayed = System.currentTimeMillis()
    }


    fun toMarket(market: Market) {
        val intent = market.intent
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // In case error try fallback.
            val fallback = MarketLink.fallbackUrl(market = market)
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(fallback)
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            Log.e(TAG, "UpdateDialog: Activity not found")
        }
    }

    suspend fun checkForUpdates(byUser: Boolean) {
        val should =
            byUser || whenUpdateDelayed == -1L || (System.currentTimeMillis() - whenUpdateDelayed > delay)
        if (!should)
            return
        try {
            val release = IUpdateNotifier.get().getFromWeb()
            //check if the market is available
            if (release.version <= context.installedVersion) {
                // means it is not automatic
                if (byUser) listener.otherwise("The app is already updated to latest version.")
                return
            }

            //TODO: Report error to Analytics
            val markets = release.markets
            if (markets.isEmpty()) return

            // The first priority I working and installed.
            var available: Market? = null
            for (market in markets) {
                // find corresponding pkg.
                val pkg = MarketPackage.map(market)
                // check if marked is installed and enabled
                val isAval = context.isAppInstalled(pkg)
                if (isAval) {
                    //init first link and report
                    available = market
                    break
                }
            }

            available?.let {
                listener.onUpdateAvailable(Update(release, it))
            } ?: kotlin.run {
                if (byUser)
                    listener.otherwise("It seems you don't have the Market installed. Please Install Google Play Store.")
            }
        } catch (e: Exception) {
            // Some sort of Exception has occurred. maybe network issue, maybe parse error JSON.
            Log.e(TAG, "checkForUpdates: ${e.cause}")
            if (byUser)
                listener.otherwise("Error while checking for updates.")
        }
    }


    companion object {
        val DEFAULT_3_HOUR_DELAY = TimeUnit.HOURS.toMillis(3)
        private const val TAG = "UpdateNotifier"
        private const val KEY_WHEN_UPDATE_DELAYED = TAG + "_update_delay_mills"
    }
}

interface IUpdate {
    fun onUpdateAvailable(new: Update)

    fun otherwise(msg: String)
}


private interface IUpdateNotifier {
    @GET("apps/android/rhythm/current_release.json")
    suspend fun getFromWeb(): Release

    companion object {
        private const val BASE_URL = "https://zikrt.github.io/site/"

        fun get(): IUpdateNotifier {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()
            return retrofit.create(IUpdateNotifier::class.java)
        }
    }
}


@Composable
fun UpdateNotifier.Dialog(update: Update, onDismissRequest: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Frame(shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.padding(Padding.LARGE)) {
                Header(
                    text = "Update Rhythm?",
                )

                val release = update.first
                Caption(text = "version: ${release.versionName}")

                Text(
                    text = stringResource(id = R.string.new_update_msg),
                    color = LocalContentColor.current.copy(ContentAlpha.medium),
                    modifier = Modifier.padding(vertical = Padding.LARGE),
                    fontWeight = FontWeight.Medium
                )

                WhatsNew(notes = release.notes)

                //buttons
                Row(
                    modifier = Modifier
                        .padding(vertical = Padding.LARGE)
                        .align(Alignment.End)
                ) {

                    TextButton(onClick = { onRequestAskLater(); onDismissRequest() }
                    ) {
                        Text(text = "LATER")
                    }

                    Button(
                        onClick = { toMarket(update.second); onDismissRequest() },
                        modifier = Modifier.padding(start = Padding.LARGE),
                        shape = RectangleShape,
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
                    ) {
                        Text(text = "UPDATE")
                    }
                }

                //

                // logo
                Divider()
                Row(
                    modifier = Modifier.padding(top = Padding.LARGE),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shop,
                        contentDescription = null,
                        modifier = Modifier.size(33.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    Label(
                        text = update.second.title,
                        color = LocalContentColor.current.copy(ContentAlpha.medium),
                        modifier = Modifier.padding(start = Padding.LARGE),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatsNew(notes: String) {
    val primary = MaterialTheme.colors.primary
    Frame(
        color = primary,
        contentColor = primary,
        modifier = Modifier.padding(top = Padding.MEDIUM)
    ) {
        Column(
            modifier = Modifier
                .padding(start = Padding.MEDIUM)
                .background(
                    primary
                        .copy(0.1f)
                        .compositeOver(MaterialTheme.colors.surface)
                )
        ) {
            Label(
                text = "What's new?",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = Padding.LARGE, top = Padding.MEDIUM)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = Padding.LARGE, vertical = Padding.SMALL)
                    .fillMaxWidth(),
                text = notes,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

val LocalUpdateNotifier = staticCompositionLocalOf<UpdateNotifier> {
    error("No Local update notifier defined!!")
}

