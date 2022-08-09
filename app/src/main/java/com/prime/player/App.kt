package com.prime.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ElevationOverlay
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prime.player.core.AppDatabase
import com.primex.preferences.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class App : Application(), Configuration.Provider {


    override fun onCreate() {
        super.onCreate()
        DEFUALT_ALBUM_ART = BitmapFactory.decodeResource(resources, R.drawable.default_art)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Playing Music Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "audio_playback_channel"
        const val UPDATES_CHANNEL_ID = "content_updates"
        const val PLAYBACK_NOTIFICATION_ID = 1
        lateinit var DEFUALT_ALBUM_ART: Bitmap

        const val DEBUG = false
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}


@Module
@InstallIn(SingletonComponent::class)
object Singleton {
    /**
     * Provides the Singleton Implementation of Preferences DataStore.
     */
    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) = Preferences(context)

    @Singleton
    @Provides
    fun audios(@ApplicationContext context: Context) = AppDatabase.get(context).audios

    @Singleton
    @Provides
    fun playlists(@ApplicationContext context: Context) = AppDatabase.get(context).playlists

}