package com.prime.media

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.prime.media.core.FontFamily
import com.prime.media.core.NightMode
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.db.Playlists
import com.prime.media.core.playback.Remote
import com.primex.preferences.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val TAG = "Audiofy"

@HiltAndroidApp
class Audiofy : Application() {

    companion object {
        private val defaultMinTrackLimit = TimeUnit.MINUTES.toMillis(1)

        /**
         * Retrieves/Sets The [NightMode] Strategy
         */
        val NIGHT_MODE =
            stringPreferenceKey(
                "${TAG}_night_mode",
                NightMode.FOLLOW_SYSTEM,
                object : StringSaver<NightMode> {
                    override fun save(value: NightMode): String = value.name
                    override fun restore(value: String): NightMode = NightMode.valueOf(value)
                }
            )

        val FONT_FAMILY =
            stringPreferenceKey(
                TAG + "_font_family",
                FontFamily.PROVIDED,
                object : StringSaver<FontFamily> {
                    override fun save(value: FontFamily): String = value.name
                    override fun restore(value: String): FontFamily = FontFamily.valueOf(value)
                }
            )

        val FORCE_COLORIZE = booleanPreferenceKey(TAG + "_force_colorize", false)
        val COLOR_STATUS_BAR = booleanPreferenceKey(TAG + "_color_status_bar", false)
        val HIDE_STATUS_BAR = booleanPreferenceKey(TAG + "_hide_status_bar", false)
        val FONT_SCALE = floatPreferenceKey(TAG + "_font_scale", defaultValue = 1.0f)

        /**
         * The counter counts the number of times this app was launched.
         */
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")

        /**
         * The length/duration of the track in mills considered above which to include
         */
        val EXCLUDE_TRACK_DURATION =
            longPreferenceKey(TAG + "_min_duration_limit_of_track", defaultMinTrackLimit)
        val MAX_RECENT_PLAYLIST_SIZE =
            intPreferenceKey(TAG + "_max_recent_size", defaultValue = 20)

        /**
         * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
         */
        val MINI_PLAYER_HEIGHT = 68.dp

        /**
         * The link to PlayStore Market.
         */
        const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID

        /**
         * If PlayStore is not available in Users Phone. This will be used to redirect to the
         * WebPage of the app.
         */
        const val FALLBACK_GOOGLE_STORE =
            "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID

        /**
         * Package name for the Google Play Store. Value can be verified here:
         * https://developers.google.com/android/reference/com/google/android/gms/common/GooglePlayServicesUtil.html#GOOGLE_PLAY_STORE_PACKAGE
         */
        const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"
    }

    override fun onCreate() {
        super.onCreate()
        // initialize firebase
        FirebaseApp.initializeApp(this)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object Singleton {
    /**
     * Provides the Singleton Implementation of Preferences DataStore.
     */
    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) =
        Preferences(context, "Shared_Preferences")

    @Singleton
    @Provides
    fun playlists(@ApplicationContext context: Context) =
        Playlists(context)

    @Singleton
    @Provides
    fun resolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object Activity {
    @ActivityRetainedScoped
    @Provides
    fun remote(@ApplicationContext context: Context) = Remote(context)

    @ActivityRetainedScoped
    @Provides
    fun toaster() = Channel()
}