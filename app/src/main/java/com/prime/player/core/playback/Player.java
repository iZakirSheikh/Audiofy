package com.prime.player.core.playback;

import android.media.MediaPlayer;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;




public class Player extends MediaPlayer implements UIHandler.Callback {

    public static final int POST_TRACK_PROGRESS = UIHandler.generateToken();
    private final UIHandler mUIHandler;
    private boolean isPreparing = false;
    private boolean mPlayWhenReady = false;
    private volatile boolean mInterruptDucking = false;
    private boolean mPrepared = false;
    private float mCurrDuckVolume = 1.0f;
    private EventListener mListener;
    private int mSeekToWhenReady = 0;


    public Player(@NonNull UIHandler handler) {
        super();
        mUIHandler = handler;
        handler.addCallback(this);
        setOnCompletionListener(mp -> {
            if (mListener != null) mListener.onComplete();
        });
        setOnPreparedListener(mp -> {
            isPreparing = false;
            mPrepared = true;
            if (mSeekToWhenReady > 0) {
                seekTo(mSeekToWhenReady);
                mSeekToWhenReady = 0;
            }
            if (mListener != null) mListener.onPrepared(mPlayWhenReady);
        });
        setOnErrorListener((mp, what, extra) -> mListener != null &&
                mListener.onError(what, extra));
    }

    public void registerEventListener(EventListener listener) {
        mListener = listener;
    }

    public void playWhenReady() {
        mPlayWhenReady = true;
    }

    @Override
    public void start() throws IllegalStateException {
        super.start();
        mUIHandler.sendEmptyMessage(POST_TRACK_PROGRESS);
        mPlayWhenReady = false;
    }

    @Override
    public void release() {
        mInterruptDucking = true;
        mPrepared = false;
        mPlayWhenReady = false;
        mListener = null;
        mCurrDuckVolume = 1.0f;
        super.release();
    }

    public boolean isGoingToPlay() {
        return mPlayWhenReady;
    }

    @Override
    public void reset() {
        mInterruptDucking = true;
        mPrepared = false;
        mPlayWhenReady = false;
        mCurrDuckVolume = 1.0f;
        super.reset();
    }

    public void fadeOut() {
        mInterruptDucking = true;
        new Thread(() -> {
            mInterruptDucking = false;
            while (!mInterruptDucking && mCurrDuckVolume > 0.2f) {
                mCurrDuckVolume -= 0.05f;
                setVolume(mCurrDuckVolume, mCurrDuckVolume);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void fadeIn() {
        mInterruptDucking = true;
        new Thread(() -> {
            mInterruptDucking = false;
            while (!mInterruptDucking && mCurrDuckVolume < 1.0f) {
                mCurrDuckVolume += 0.3f;
                setVolume(mCurrDuckVolume, mCurrDuckVolume);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void seekToWhenReady(final int pos) {
        mSeekToWhenReady = pos;
    }

    public boolean isPreparing() {
        return isPreparing;
    }

    public boolean isPrepared() {
        return mPrepared;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == POST_TRACK_PROGRESS) {
            if (mListener != null) {
                mListener.onProgress(getCurrentPosition());
                if (isPlaying()) mUIHandler.sendEmptyMessageAtTime(POST_TRACK_PROGRESS,
                        SystemClock.uptimeMillis() + 250);
                return true;
            }
        }
        return false;
    }

    public interface EventListener {
        void onComplete();

        void onPrepared(boolean startPlaying);

        boolean onError(int what, int extra);

        void onProgress(int progressMills);
    }
}
