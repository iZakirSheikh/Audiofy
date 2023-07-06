package com.prime.media.impl

import android.content.ContentResolver
import android.content.Context
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.Playlists
import com.prime.media.core.playback.Remote
import com.primex.preferences.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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