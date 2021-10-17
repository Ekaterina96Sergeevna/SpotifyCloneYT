package com.plcoding.spotifycloneyt.di

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.plcoding.spotifycloneyt.data.remote.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class) // dependency inside ServiceModule live as long as ServiceComponent does
object ServiceModule {

    @ServiceScoped
    @Provides
    // allow dagger hilt to be able to inject this music database instance into FirebaseMusicSource class
    fun provideMusicDatabase() = MusicDatabase()


    @ServiceScoped // we use it only in Service, not in our whole application
    @Provides
    // we need call audio attributes which just save some meta information about our player
    // these don't need any dependencies to be created -  we don't need to pass any parameters
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @ServiceScoped
    @Provides
    // player that will play our music
    fun provideExoPlayer(
        // some dependencies to be created
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes // we said how create audioAttributes above
    ) = SimpleExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true) // pause our music player for example if user in headphones and so on
    }

    @ServiceScoped
    @Provides
    // this fun give us music source
    // UserAgent - our app
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ) = DefaultDataSourceFactory(context, Util.getUserAgent(context, "Spotify App"))
}