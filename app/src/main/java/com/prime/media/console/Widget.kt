package com.prime.media.console

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import coil.imageLoader
import coil.request.ImageRequest
import com.prime.media.R
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.mediaUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "Widget"

private inline fun PendingActivity(ctx: Context): PendingIntent {
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
    return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
private inline fun PendingAction(ctx: Context, action: String): PendingIntent {
    val intent = Intent(action, null, ctx, Widget::class.java)
    return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}

/**
 * Runs code in [block] on UI Thread.
 * @return result from the block.
 */
private suspend inline fun <T> runOnUiThread(noinline block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

private const val ACTION_SEEK_BACK_MILLS = "action_seek_back_mills"
private const val ACTION_SEEK_NEXT = "action_seek_next"
private const val ACTION_TOGGLE = "action_toggle"

@AndroidEntryPoint
class Widget : AppWidgetProvider() {
    @Inject
    lateinit var remote: Remote
    // The current media item.
    private var current: MediaItem? = null
    // The artwork associated with current mediaItem.
    private var artwork: Bitmap? = null
    // create pending intents.
    /**
     * This fun updates current and fetches [artwork] of it.
     */
    private suspend fun loadArtwork(ctx: Context) {
        val old = current
        current = runOnUiThread { remote.current }
        // It is the same call
        if (old?.mediaUri == current?.mediaUri)
            return
        val uri = current?.mediaMetadata?.artworkUri
        val request = ImageRequest.Builder(ctx).error(R.drawable.default_art)
            .placeholder(R.drawable.default_art).data(uri).build()
        artwork = ctx.imageLoader.execute(request).drawable?.toBitmap()
    }

    /**
     * Returns only when app is running.
     * FixMe: Find Better alternative.
     */
    private suspend fun ensureRunning() {
        runOnUiThread { remote.loaded.first() }
    }

    /**
     * Update the Widget associated with [id]
     */
    private suspend fun AppWidgetManager.onUpdate(ctx: Context, id: Int) {
        // fetch_artwork maybe.
        loadArtwork(ctx)
        val view = RemoteViews(ctx.packageName, R.layout.widget_style_notification).apply {
            setOnClickPendingIntent(R.id.widget_seek_back_10, PendingAction(ctx, ACTION_SEEK_BACK_MILLS))
            setOnClickPendingIntent(R.id.widget_play_toggle, PendingAction(ctx, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.skip_to_next, PendingAction(ctx, ACTION_SEEK_NEXT))
            setOnClickPendingIntent(R.id.widget, PendingActivity(ctx))
            // config. other views
            val isPLaying = runOnUiThread { remote.isPlaying }
            setImageViewResource(
                R.id.widget_play_toggle,
                if (isPLaying) R.drawable.media3_notification_pause else R.drawable.media3_notification_play
            )
            // calculate elapsed time.
            val elapsed: Long = SystemClock.elapsedRealtime() - runOnUiThread { remote.position }
            setChronometer(R.id.widget_chronometer, elapsed, "%tH%tM:%tS", isPLaying)
            setImageViewBitmap(R.id.widget_artwork, artwork)
            setTextViewText(R.id.widget_title, current?.mediaMetadata?.title)
            setTextViewText(R.id.widget_subtitle, current?.mediaMetadata?.subtitle)
        }
        Log.d(TAG, "update $id")
        updateAppWidget(id, view)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, Ids: IntArray) {
        GlobalScope.launch {
            ensureRunning()
            for (id in Ids) mgr.onUpdate(ctx, id)
        }
    }

    override fun onEnabled(ctx: Context) {
        super.onEnabled(ctx)
        Toast.makeText(ctx, "Use the handles on the corners to resize the widget.", Toast.LENGTH_SHORT).show()
    }

    private suspend fun onAction(action: String){
        when(action){
            ACTION_SEEK_NEXT -> remote.skipToNext()
            ACTION_TOGGLE -> remote.togglePlay()
            ACTION_SEEK_BACK_MILLS -> {
                val position = remote.position
                val duration = remote.duration
                // don't do anything.
                if (position == C.TIME_UNSET || duration == C.TIME_UNSET)
                    return
                val mills = position + TimeUnit.SECONDS.toMillis(10)
                remote.seekTo(mills.coerceIn(0, duration))
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val action = intent?.action ?: return
        GlobalScope.launch {
            ensureRunning()
            runOnUiThread { onAction(action) }
        }
    }
}