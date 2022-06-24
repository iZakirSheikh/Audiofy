package com.prime.player.core.playback;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.prime.player.App.DEFUALT_ALBUM_ART;
import static com.prime.player.App.PLAYBACK_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.prime.player.App;
import com.prime.player.MainActivity;
import com.prime.player.R;
import com.prime.player.core.AudioRepo;
import com.prime.player.core.models.Audio;
import com.prime.player.utils.MediaUtilsKt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackService extends Service implements Player.EventListener {

    // playback intent
    public static final String ACTION_LOAD_LIST = "action_load_list";
    public static final String PARAM_LIST = "list";
    public static final String PARAM_SHUFFLE = "shuffle";
    public static final String PARAM_START_PLAYING = "start_playing";
    public static final String PARAM_FROM_INDEX = "from_index";
    public static final String PARAM_NAME = "name";

    // repeat mode constants
    public static final int REPEAT_MODE_NONE = 0;
    public static final int REPEAT_MODE_ALL = 1;
    public static final int REPEAT_MODE_THIS = 2;
    private static final String TAG = "PlaybackService";

    // notification intent constants
    public static final String ACTION_ADD_TO_FAV = TAG + "_action_add_to_fav";
    public static final String ACTION_SKIP_TO_NEXT = TAG + "_action_skip_to_next";
    public static final String ACTION_SKIP_TO_PREV = TAG + "_action_skip_to_prev";
    public static final String ACTION_PLAY_PAUSE = TAG + "_action_play_pause";
    public static final String ACTION_DELETE = TAG + "_action_delete";

    // playback handler constants
    private static final int RELEASE_WAKELOCK = 0;
    private static final int TRACK_ENDED = 1;
    private static final int PLAY_TRACK = 3;
    private static final int SET_TRACK_POSITION = 5;
    private static final int FOCUS_CHANGE = 6;
    private static final int PREPARE_NEXT = 4;
    private static final int PLAYER_HANDLER_ARG_INVALID = -5;
    private static final int DUCK = 7;
    private static final int UN_DUCK = 8;
    // notify change args
    private static final int TRACK_CHANGED = 9;
    private static final int QUEUE_CHANGED = 10;
    private static final int STATE_CHANGED = 11;

    private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;
    //save queue constants
    private static final String KEY_PREF_QUEUE = TAG + "_playing_queue";
    private static final String KEY_PLAYLIST_NAME = "Playlist Name : ";
    private static final String KEY_PREF_REPEAT_MODE = TAG + "_repeat_mode";
    private static final String KEY_PREF_POSITION = TAG + "_position";
    private static final String KEY_PREF_SHUFFLE = TAG + "_shuffle";
    private static final String KEY__PREF_TRACK_BOOKMARK = TAG + "_track_bookmark";
    private static final String KEY_PREF_TRACK_AUDIO_ID = TAG + "_audio_id";
    private static final String KEY_SHUFFLED_LIST = TAG + "_Shuffled List : ";
    private static final String KEY_ORIGINAL_LIST = TAG + "_Original List : ";
    // constants represents state of this service class
    private final List<Audio> mList = new ArrayList<>();
    private final List<Audio> mOriginalList = new ArrayList<>();
    int mPrevBookSaveTime = 0;
    // The service mutable fields
    private int mTrackPos;
    private int mNextTrackPos;
    private long mTrackID;
    private boolean mShuffle = false;
    private long mSleepAfter = -1;
    private boolean mPausedByTransientLossOfFocus;
    @RepeatMode
    private int mRepeatMode = REPEAT_MODE_NONE;
    private int mBookmark = 0;
    private String mPlaylistName;
    private final PlaybackBinder mPlaybackBinder = new PlaybackBinder();
    private Player mMediaPlayer;
    private AudioManager mAudioManager;
    // boolean represents weather the state of the service has changed
    // services state changes when it jumps from invalid to valid to next etc.
    // default value is false
    private boolean mNeedsRefresh = false;
    private MediaSessionCompat mMediaSessionCompat;
    private SharedPreferences mSharedPrefs;
    private PowerManager.WakeLock mWakeLock;
    private final IntentFilter
            mBecomingNoisyReceiverIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private boolean mNotHandledMetaChangedForCurrentTrack;
    private NotificationManager mNotificationManager;
    @Nullable
    private EventListener mListener;
    private AudioRepo mLibrary;
    private final AudioManager.OnAudioFocusChangeListener
            mAudioFocusListener = focusChange -> {
        handle(FOCUS_CHANGE, focusChange);
    };
    private final BroadcastReceiver mBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pause();
            }
        }
    };
    private UIHandler mUIHandler;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            // this can never be null
            switch (action) {
                case ACTION_ADD_TO_FAV:
                    toggleFav();
                    break;
                case ACTION_PLAY_PAUSE:
                    togglePlay();
                    break;
                case ACTION_SKIP_TO_PREV:
                    back(false);
                    break;
                case ACTION_SKIP_TO_NEXT:
                    playNextTrack(false);
                    break;
                case ACTION_DELETE:
                    // don't close service on cancel
                    //s.quit();
            }
        }
    };

    private static void shuffleList(@NonNull List<Audio> listToShuffle, final int current) {
        if (listToShuffle.isEmpty()) return;
        if (current >= 0) {
            Audio song = listToShuffle.remove(current);
            Collections.shuffle(listToShuffle);
            listToShuffle.add(0, song);
        } else {
            Collections.shuffle(listToShuffle);
        }
    }

    private void initMediaSession() {
        ComponentName
                mediaButtonReceiverComponentName =
                new ComponentName(getApplicationContext(), PlaybackService.class);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);

        PendingIntent
                mediaButtonReceiverPendingIntent =
                getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

        mMediaSessionCompat =
                new MediaSessionCompat(this,
                        TAG,
                        mediaButtonReceiverComponentName,
                        mediaButtonReceiverPendingIntent);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNextTrack(true);
            }

            @Override
            public void onSkipToPrevious() {
                back(true);
            }

            @Override
            public void onStop() {
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                return false;
            }
        });
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mMediaSessionCompat.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
    }

    private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager;
    }

    private void updateMediaSessionPlaybackState() {
        mMediaSessionCompat.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setActions(MEDIA_SESSION_ACTIONS)
                        .setState(mMediaPlayer.isPlaying() ?
                                        PlaybackStateCompat.STATE_PLAYING :
                                        PlaybackStateCompat.STATE_PAUSED,
                                mMediaPlayer.getCurrentPosition(), 1)
                        .build());
    }

    public void seekTo(int millis) {
        try {
            mMediaPlayer.seekTo(millis);
        } catch (Exception e) {
            //
        }
    }

    private void closeAudioEffectSession() {
        final Intent
                audioEffectsIntent =
                new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                mMediaPlayer.getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
    }

    private void updateMediaSessionMetaData() {
        final Audio audio = getCurrentTrack();
        if (audio.getId() == -1) {
            mMediaSessionCompat.setMetadata(null);
            return;
        }
        final MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        audio.getArtist() != null ? audio.getArtist().getName() : "Unknown")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                        audio.getArtist() != null ? audio.getArtist().getName() : "Unknown")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        audio.getAlbum() != null ? audio.getAlbum().getTitle() : "Unknown")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, audio.getTitle())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, audio.getDuration())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mTrackPos + 1)
                .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, audio.getYear())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metaData.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mList.size());
        }
        mMediaSessionCompat.setMetadata(metaData.build());
    }

    private boolean requestFocus() {
        return (getAudioManager().requestAudioFocus(mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    private void playPreviousTrack(boolean force) {
        playTrackAt(getPreviousPosition(force));
    }

    private int getPreviousPosition(boolean force) {
        int newPosition = mTrackPos - 1;
        switch (mRepeatMode) {
            case REPEAT_MODE_ALL:
                if (newPosition < 0) {
                    newPosition = mList.size() - 1;
                }
                break;
            case REPEAT_MODE_THIS:
                if (force) {
                    if (newPosition < 0) {
                        newPosition = mList.size() - 1;
                    }
                } else {
                    newPosition = mTrackPos;
                }
                break;
            case REPEAT_MODE_NONE:
                if (newPosition < 0) {
                    newPosition = 0;
                }
                break;
        }
        return newPosition;
    }

    public long getQueueDurationMillis(int position) {
        long duration = 0;
        for (int i = position + 1; i < mList.size(); i++)
            duration += mList.get(i).getDuration();
        return duration;
    }

    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    private MediaSessionCompat getMediaSession() {
        return mMediaSessionCompat;
    }

    private void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void acquireWakeLock(long milli) {
        mWakeLock.acquire(milli);
    }

    private boolean isLastTrack() {
        return mTrackPos == mList.size() - 1;
    }

    public Audio getCurrentTrack() {
        return getTrackAt(mTrackPos);
    }

    private Audio getTrackAt(int position) {
        if (position >= 0 && position < mList.size()) {
            return mList.get(position);
        } else {
            throw new IllegalArgumentException("This must shouldn't have happened!!");
        }
    }

    public void playNextTrack(boolean force) {
        playTrackAt(getNextPosition(force));
    }

    private int getNextPosition(boolean force) {
        int position = mTrackPos + 1;
        switch (mRepeatMode) {
            case REPEAT_MODE_ALL:
                if (isLastTrack()) {
                    position = 0;
                }
                break;
            case REPEAT_MODE_THIS:
                if (force) {
                    if (isLastTrack()) {
                        position = 0;
                    }
                } else {
                    position -= 1;
                }
                break;
            default:
            case REPEAT_MODE_NONE:
                if (isLastTrack()) {
                    position -= 1;
                }
                break;
        }
        return position;
    }

    public void playTrackAt(final int position) {
        // handle this on the handlers thread to avoid blocking the ui thread
        //playerHandler.removeMessages(PLAY_SONG);
        //playerHandler.obtainMessage(PLAY_SONG, position, 0).sendToTarget();
        handle(PLAY_TRACK, position);
    }

    private void prepare(final int position) {
        Audio track = getTrackAt(position);
        prepare(track);
    }

    private void prepare(@NonNull final Audio audio) {
        long trackId = audio.getId();
        mMediaPlayer.reset();
        try {
            if (trackId != -1) {
                Uri trackUri = MediaUtilsKt.getTrackUri(trackId);
                mMediaPlayer.setDataSource(this, trackUri);
            } else
                mMediaPlayer.setDataSource(audio.getPath());
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME,
                getPackageName());
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE,
                AudioEffect.CONTENT_TYPE_MUSIC);
        sendBroadcast(intent);
    }

    public void back(boolean force) {
        if (mMediaPlayer.getCurrentPosition() > 5000)
            seekTo(0);
        else
            playPreviousTrack(force);
    }

    public void togglePlay() {
        if (mMediaPlayer.isPlaying())
            pause();
        else
            play();
    }

    public void toggleFav() {
        mLibrary.toggleFav(mTrackID);
        handle(STATE_CHANGED);
    }

    private void pause() {
        try {
            mPausedByTransientLossOfFocus = false;
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                handle(STATE_CHANGED);
            }
        } catch (IllegalStateException e) {
            Log.i(TAG, "pause: " + e.getMessage());
        }
    }

    private void play() {
        try {
            if (requestFocus()) {
                if (!mMediaPlayer.isPlaying()) {
                    if (mMediaPlayer.isPrepared()) {
                        mMediaPlayer.start();
                        if (mNotHandledMetaChangedForCurrentTrack) {
                            handle(TRACK_CHANGED);
                            mNotHandledMetaChangedForCurrentTrack = false;
                        }
                        handle(STATE_CHANGED);
                        handle(UN_DUCK);
                    } else // if it is not prepared
                        handle(PLAY_TRACK, mTrackPos);
                }
            } else
                Toast.makeText(this,
                        getResources().getString(R.string.message_audio_focus_denied),
                        Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            Toast.makeText(this, "An unknown error occurred while playing this file", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "play: " + e.getMessage());
            // any exception occurs
            // move to next and decide what to do there.
            handle(TRACK_ENDED);
        }
    }


    private void saveQueueIfNeeded() {
        // remove any previously saved state
        if (mList.size() == 0)
            mSharedPrefs.edit().putString(KEY_PREF_QUEUE, null).apply();
        else {
            JSONObject parcel = new JSONObject();
            try {
                parcel.put(KEY_PLAYLIST_NAME, mPlaylistName);
                JSONArray originalList = new JSONArray();
                for (Audio audio : mOriginalList)
                    originalList.put(audio.getId());
                parcel.put(KEY_ORIGINAL_LIST, originalList);
                JSONArray shuffledList = new JSONArray();
                for (Audio audio : mList)
                    shuffledList.put(audio.getId());
                parcel.put(KEY_SHUFFLED_LIST, shuffledList);
            } catch (JSONException e) {
                // do nothing
            }
            String state = parcel.toString();
            mSharedPrefs.edit().putString(KEY_PREF_QUEUE, state).apply();
        }
    }

    private void restoreStateIfNeeded() {
        String queue = mSharedPrefs.getString(KEY_PREF_QUEUE, null);
        if (queue == null) // no saved state
            return;
        mShuffle = mSharedPrefs.getBoolean(KEY_PREF_SHUFFLE, false);
        mRepeatMode = mSharedPrefs.getInt(KEY_PREF_REPEAT_MODE, REPEAT_MODE_NONE);
        mTrackPos = mSharedPrefs.getInt(KEY_PREF_POSITION, 0);
        mBookmark = mSharedPrefs.getInt(KEY__PREF_TRACK_BOOKMARK, 0);
        mTrackID = mSharedPrefs.getLong(KEY_PREF_TRACK_AUDIO_ID, -1);
        try {
            JSONObject sJSON = new JSONObject(queue);
            mPlaylistName = sJSON.getString(KEY_PLAYLIST_NAME);
            JSONArray array = sJSON.getJSONArray(KEY_ORIGINAL_LIST);
            for (int i = 0; i < array.length(); i++) {
                int id = array.getInt(i);
                // my be the track has been deleted
                Audio audio = mLibrary.getAudioById(id);
                if (audio != null)
                    mOriginalList.add(audio);
            }
            //may be the original array is empty
            //just return without doing anything.
            if (mOriginalList.isEmpty())
                return;

            array = sJSON.getJSONArray(KEY_SHUFFLED_LIST);
            int index = -1;
            for (int i = 0; i < array.length(); i++) {
                int key = array.getInt(i);
                Audio audio = ListUtils.findU(mOriginalList, key, Audio::getId);
                // don;t add null values
                if (audio == null)
                    continue;
                if (mTrackID == key)
                    index = i;
                mList.add(audio);
            }
            if (index < 0) {
                // means the track has been deleted
                mBookmark = 0;
                handle(SET_TRACK_POSITION, 0);
            } else
                handle(SET_TRACK_POSITION, index);
            // restore before actual time
            mBookmark = mBookmark > 5 ? mBookmark - 5 : 0;
            if (mBookmark > 0)
                mMediaPlayer.seekToWhenReady(mBookmark);
            mNeedsRefresh = true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void unregisterListener() {
        mListener = null;
    }

    public void registerListener(@NonNull EventListener listener) {
        mListener = listener;
    }

    public void loadQueue(@NonNull List<Audio> list,
                          @NonNull final String name,
                          int startPosition,
                          boolean shuffle,
                          boolean startPlaying) {
        if (!list.isEmpty() && startPosition >= 0 && startPosition < list.size()) {
            // pause media player if it is still running
            if (mMediaPlayer.isPlaying())
                pause();
            // clear list and rest service state
            mList.clear();
            mOriginalList.clear();
            mOriginalList.addAll(list);
            mList.addAll(mOriginalList);
            mTrackPos = startPosition;
            setShuffle(shuffle);
            mPlaylistName = name;
            if (startPlaying)
                playTrackAt(mTrackPos);
            else
                handle(SET_TRACK_POSITION, mTrackPos);
            handle(QUEUE_CHANGED);
        }
    }


    public List<Audio> getPlayingQueue() {
        return mList;
    }

    public List<Audio> getModifiedList() {
        List<Audio> list = new ArrayList<>();
        // include only those files in this which are being played;
        for (int i = mTrackPos; i < mList.size(); i++) {
            list.add(mList.get(i));
        }
        return list;
    }

    public void setSleepTimer(long sleepAfterMills) {
        //cancel sleep timer
        if (sleepAfterMills == -1)
            mSleepAfter = -1L;
        else
            mSleepAfter = System.currentTimeMillis() + sleepAfterMills;
    }

    public void cycleRepeatMode() {
        switch (mRepeatMode) {
            case REPEAT_MODE_NONE:
                setRepeatMode(REPEAT_MODE_ALL);
                break;
            case REPEAT_MODE_ALL:
                setRepeatMode(REPEAT_MODE_THIS);
                break;
            case REPEAT_MODE_THIS:
                setRepeatMode(REPEAT_MODE_NONE);
                break;
        }
    }

    public void toggleShuffle() {
        setShuffle(!mShuffle);
    }

    private void setShuffle(boolean shuffle) {
        mShuffle = shuffle;
        if (mShuffle) {
            shuffleList(mList, mTrackPos);
            mTrackPos = 0;
        } else {
            long currentTrackId = getCurrentTrack().getId();
            mList.clear();
            mList.addAll(mOriginalList);
            int newPosition = 0;
            for (Audio audio : mList) {
                if (audio.getId() == currentTrackId) {
                    newPosition = mList.indexOf(audio);
                }
            }
            mTrackPos = newPosition;
        }
        mSharedPrefs.edit().putBoolean(KEY_PREF_SHUFFLE, mShuffle).apply();
        handle(QUEUE_CHANGED);
    }

    @RepeatMode
    public int getRepeatMode() {
        return mRepeatMode;
    }

    private void setRepeatMode(@RepeatMode final int repeatMode) {
        mRepeatMode = repeatMode;
        mSharedPrefs.edit().putInt(KEY_PREF_REPEAT_MODE, mRepeatMode).apply();
        handle(PREPARE_NEXT);
    }

    public boolean isShuffleEnabled() {
        return mShuffle;
    }

    public String getTitle() {
        return mPlaylistName;
    }

    public int getCurrentPosition() {
        return mTrackPos;
    }

    private Notification initNotification() {
        PendingIntent mSkipToPrev = getBroadcast(this,
                1,
                new Intent(ACTION_SKIP_TO_PREV), FLAG_UPDATE_CURRENT);
        PendingIntent mSkipToNext = getBroadcast(this,
                1,
                new Intent(ACTION_SKIP_TO_NEXT), FLAG_UPDATE_CURRENT);
        PendingIntent mPlayPause = getBroadcast(this,
                1,
                new Intent(ACTION_PLAY_PAUSE), FLAG_UPDATE_CURRENT);
        PendingIntent mAddToFav = getBroadcast(this,
                1,
                new Intent(ACTION_ADD_TO_FAV), FLAG_UPDATE_CURRENT);
        PendingIntent swipeToDeleteIntent = getBroadcast(this,
                1,
                new Intent(ACTION_DELETE), FLAG_UPDATE_CURRENT);
        PendingIntent
                appIntent =
                getActivity(this,
                        0,
                        new Intent(this, MainActivity.class),
                        FLAG_UPDATE_CURRENT);
        Audio audio = getCurrentTrack();
        Bitmap art = null;
        if (audio.getAlbum() != null)
            art = getAlbumArt(audio.getAlbum().getId());
        if (art == null)
            art = DEFUALT_ALBUM_ART;
        String title = audio.getTitle();
        SpannableStringBuilder builder = new SpannableStringBuilder(title);
        builder.setSpan(new StyleSpan(Typeface.BOLD),
                0,
                title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        final NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(this,
                        App.PLAYBACK_CHANNEL_ID);
        nBuilder.setOngoing(mMediaPlayer.isPlaying() || mMediaPlayer.isPreparing())
                .setLargeIcon(art)
                .setSubText(mPlaylistName)
                .setContentText(audio.getAlbum() != null
                        ? audio.getAlbum().getTitle()
                        : "Unknown")
                //.setContentText(mPlaylistName)
                .setContentTitle(builder)
                .setContentIntent(appIntent)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false)
                .setTimeoutAfter(30000)
                .setDeleteIntent(swipeToDeleteIntent)
                .setGroupSummary(false)
                .addAction(R.drawable.ic_skip_to_prev_filled,
                        "Skip to previous",
                        mSkipToPrev)
                .addAction((mMediaPlayer.isPlaying() || mMediaPlayer.isPreparing()) ?
                                R.drawable.ic_pause :
                                R.drawable.ic_play_arrow,
                        "Play/Pause",
                        mPlayPause)
                .addAction(R.drawable.ic_skip_to_next_filled,
                        "Skip to next",
                        mSkipToNext)
                .addAction(isFavourite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart,
                        "Add/Remove to Fav",
                        mAddToFav)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,
                                1,
                                2)
                        .setMediaSession(mMediaSessionCompat.getSessionToken()));
        Notification notification = nBuilder.build();
        mNotificationManager.notify(PLAYBACK_NOTIFICATION_ID, notification);
        return notification;
    }

    public boolean isFavourite() {
        return mLibrary.isFavourite(mTrackID);
    }

    private void handle(@What int what) {
        handle(what, PLAYER_HANDLER_ARG_INVALID);
    }

    private void handle(@What int what, int arg) {
        switch (what) {
            case DUCK:
                mMediaPlayer.fadeOut();
                break;
            case FOCUS_CHANGE:
                switch (arg) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (!mMediaPlayer.isPlaying() && mPausedByTransientLossOfFocus) {
                            play();
                            mPausedByTransientLossOfFocus = false;
                        }
                        handle(UN_DUCK, PLAYER_HANDLER_ARG_INVALID);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Lost focus for an unbounded amount of time: stop playback and release
                        // media playback
                        pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // Lost focus for a short time, but we have to stop
                        // playback. We don't release the media playback because playback
                        // is likely to resume
                        boolean wasPlaying = mMediaPlayer.isPlaying();
                        pause();
                        mPausedByTransientLossOfFocus = wasPlaying;
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // Lost focus for a short time, but it's ok to keep playing
                        // at an attenuated level
                        handle(DUCK, PLAYER_HANDLER_ARG_INVALID);
                        break;
                }
                break;
            case STATE_CHANGED:
                //refresh Notification
                Notification notification = initNotification();
                updateMediaSessionPlaybackState();
                boolean playing = mMediaPlayer.isPlaying() || mMediaPlayer.isGoingToPlay();
                if (mListener != null) {
                    boolean liked = isFavourite();
                    mListener.setFavourite(liked);
                    mListener.onPlayingStateChanged(playing);
                }
                // make this service foreground or background only if
                if (playing)
                    startForeground(PLAYBACK_NOTIFICATION_ID, notification);
                else {
                    // stop foreground but don't kill it
                    stopForeground(false);
                }
                break;
            case PLAY_TRACK:
                handle(SET_TRACK_POSITION, arg);
                mMediaPlayer.playWhenReady();
                break;
            case PREPARE_NEXT:
                mNextTrackPos = getNextPosition(false);
                Audio track = getTrackAt(mNextTrackPos);
                if (mListener != null) mListener.setNextTrack(track);
                break;
            case QUEUE_CHANGED:
                // because playing queue size might have changed
                updateMediaSessionMetaData();
                saveQueueIfNeeded();
                // if new queue size is greater than zero
                if (mList.size() > 0) handle(PREPARE_NEXT);
                break;
            case RELEASE_WAKELOCK:
                releaseWakeLock();
                break;
            case SET_TRACK_POSITION:
                // update track position
                mTrackPos = arg;
                // update current track id
                mTrackID = getCurrentTrack().getId();
                // prepare current track
                prepare(arg);
                // prepare next track
                handle(PREPARE_NEXT);
                mNotHandledMetaChangedForCurrentTrack = false;
                // notify change to listener
                handle(TRACK_CHANGED);
                mNeedsRefresh = true;
                mSharedPrefs.edit().putInt(KEY_PREF_POSITION, mTrackPos).apply();
                mSharedPrefs.edit().putLong(KEY_PREF_TRACK_AUDIO_ID, mTrackID).apply();
                break;
            case TRACK_CHANGED:
                notification = initNotification();
                mNotificationManager.notify(PLAYBACK_NOTIFICATION_ID, notification);
                updateMediaSessionMetaData();
                track = getCurrentTrack();
                //Save in history
                mLibrary.addToRecent(track.getId());
                if (mListener != null)
                    mListener.onTrackChanged(track, isFavourite());
                break;
            case TRACK_ENDED:
                if (mRepeatMode == REPEAT_MODE_NONE && isLastTrack()) {
                    // because it will result in pause
                    handle(STATE_CHANGED);
                    seekTo(0);
                } else playNextTrack(false);
                handle(RELEASE_WAKELOCK);
                break;
            case UN_DUCK:
                mMediaPlayer.fadeIn();
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLibrary = AudioRepo.get(this);
        UIHandler handler = UIHandler.get();
        mMediaPlayer = new Player(handler);
        mMediaPlayer.registerEventListener(this);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //register notification receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD_TO_FAV);
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_SKIP_TO_NEXT);
        filter.addAction(ACTION_SKIP_TO_PREV);
        filter.addAction(ACTION_DELETE);
        registerReceiver(mIntentReceiver, filter);
        registerReceiver(mBecomingNoisyReceiver, mBecomingNoisyReceiverIntentFilter);
        initMediaSession();
        mMediaSessionCompat.setActive(true);
        restoreStateIfNeeded();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                if (action.equals(ACTION_LOAD_LIST)) {
                    String parcel = intent.getStringExtra(PARAM_LIST);
                    try {
                        JSONArray raw = new JSONArray(parcel);
                        List<Audio> list = new ArrayList<>();

                        for (int i = 0; i < raw.length(); i++) {
                            Audio audio = mLibrary.getAudioById(raw.getLong(i));
                            if (audio != null)
                                list.add(audio);
                        }
                        boolean startPlaying = intent.getBooleanExtra(PARAM_START_PLAYING, false);
                        boolean shuffle = intent.getBooleanExtra(PARAM_SHUFFLE, false);
                        int pos = intent.getIntExtra(PARAM_FROM_INDEX, 0);
                        String name = intent.getStringExtra(PARAM_NAME);
                        loadQueue(list, name, pos, shuffle, startPlaying);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        pause();
        unregisterReceiver(mBecomingNoisyReceiver);
        unregisterReceiver(mIntentReceiver);
        mMediaSessionCompat.setActive(false);
        mNotificationManager.cancel(App.PLAYBACK_NOTIFICATION_ID);
        closeAudioEffectSession();
        getAudioManager().abandonAudioFocus(mAudioFocusListener);
        mMediaPlayer.release();
        mWakeLock.release();
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPlaybackBinder;
    }

    @Override
    public void onComplete() {
        acquireWakeLock(30000);
        handle(TRACK_ENDED);
    }

    @Override
    public void onPrepared(boolean startPlaying) {
        if (startPlaying)
            play();
    }

    @Override
    public boolean onError(int what, int extra) {
        if (mListener != null) mListener.onError(what, extra);
        else
            // make a simple toast
            Toast.makeText(this,
                    getResources().getString(R.string.unplayable_file),
                    Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onProgress(int progressMills) {
        mBookmark = progressMills;
        if (mPrevBookSaveTime > progressMills) mPrevBookSaveTime = 0;
        // save bookmark after every 5 seconds
        if (progressMills - mPrevBookSaveTime > 5000) {
            mPrevBookSaveTime = progressMills;
            mSharedPrefs.edit().putInt(KEY__PREF_TRACK_BOOKMARK, mBookmark).apply();
        }
        if (mListener != null) mListener.onProgress(progressMills);
        //save bookmark after every second

        if (mSleepAfter != -1) {
            // check if we should stop playing
            long currentTime = System.currentTimeMillis();
            // if sleep timer lies between it and +1000 pause it
            if (currentTime >= mSleepAfter) {
                // once paused if in background the service will automatically die out
                pause();
                // once used sleep timer is set back to invalid value
                mSleepAfter = -1;
            }
        }
    }

    public boolean needsRefresh() {
        boolean refresh = mNeedsRefresh;
        // after reading value
        // set it to false
        mNeedsRefresh = false;
        return refresh;
    }

    public Audio getNextTrack() {
        return getTrackAt(mNextTrackPos);
    }

    public int getBookmark() {
        return mBookmark;
    }

    public boolean isTimerActive() {
        return mSleepAfter != -1;
    }

    public Long getSleepAfter() {
        return mSleepAfter;
    }


    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public boolean isInitialized() {
        return mList.size() > 0;
    }

    @Nullable
    public Bitmap getAlbumArt(final long albumId) {
        Uri
                uri =
                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),
                        albumId);
        ParcelFileDescriptor descriptor;
        Bitmap art = null;
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            art = BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return art;
    }

    private static PendingIntent getBroadcast(Context context,
                                             int requestCode,
                                             Intent intent,
                                             int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getBroadcast(context, requestCode, intent, flags | PendingIntent.FLAG_IMMUTABLE);
        } else
            return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getActivity(context, requestCode, intent, flags | PendingIntent.FLAG_IMMUTABLE);
        } else
            return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    public interface EventListener {
        void onPlayingStateChanged(final boolean isPlaying);

        void onTrackChanged(@NonNull final Audio newTrack, final boolean isFavourite);

        void onProgress(final long mills);

        void onError(final int what, int extra);

        void setNextTrack(@NonNull final Audio nextTrack);

        void setFavourite(boolean favourite);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    @IntDef({RELEASE_WAKELOCK,
            TRACK_ENDED,
            PLAY_TRACK,
            SET_TRACK_POSITION,
            FOCUS_CHANGE,
            DUCK,
            PREPARE_NEXT,
            UN_DUCK,
            STATE_CHANGED,
            TRACK_CHANGED,
            QUEUE_CHANGED
    })
    private @interface What {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @IntDef({
            REPEAT_MODE_ALL,
            REPEAT_MODE_NONE,
            REPEAT_MODE_THIS
    })
    public @interface RepeatMode {
    }

    public class PlaybackBinder extends Binder {
        @NonNull
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}
