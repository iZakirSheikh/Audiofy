package com.prime.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

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
}