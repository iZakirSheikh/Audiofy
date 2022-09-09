package com.prime.player.core.playback;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.util.Set;

import kotlin.jvm.Synchronized;


public class UIHandler extends Handler {


    private static int mCount = 0;
    private final Set<Callback> callbacks = new ArraySet<>();

    public UIHandler() {
        super(Looper.getMainLooper());
    }

    public synchronized static int generateToken() {
        return mCount++;
    }

    public void addCallback(Callback callback) {
        if (!callbacks.add(callback)) {
            throw new IllegalArgumentException("Callback " +
                    callback +
                    " already added in " +
                    callbacks);
        }
    }

    public void removeUpdateCallback(@NonNull Callback callback) {
        if (!callbacks.remove(callback)) {
            throw new IllegalArgumentException("Callback " +
                    callback +
                    " not found in " +
                    callbacks);
        }
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        for (Callback callback : callbacks) {
            if (callback.handleMessage(msg))
                // if handled by someone break it.
                break;
        }
    }

    public interface Callback {
        boolean handleMessage(@NonNull Message msg);
    }

    private static volatile UIHandler INSTANCE = null;

    @Synchronized
    public static UIHandler get() {
        if (INSTANCE == null) {
            INSTANCE = new UIHandler();
        }
        return INSTANCE;
    }
}
