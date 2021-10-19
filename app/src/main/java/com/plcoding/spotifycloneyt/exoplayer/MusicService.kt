package com.plcoding.spotifycloneyt.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlaybackPreparer
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerEventListener
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    // dagger hilt will automatically recognize that we want inject a variable here on object of this type
    // dagger hilt will look in our service module

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    // service runs on the main thread, so we would not blocking ui thread in our app
    // and for that we will have a coroutine scope here
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    // scope in which we will launch the coroutines, service scope will deal with the cancellation of the coroutines
    // when this service dies we'll sure that coroutines will be cancelled and not lead the memory leaks
    // CoroutineScope has properties of our main dispatcher and service job together -
    // that will just allow us to define such a custom service scope here

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    // variable in which we save if the service is currently foreground service or not
    var isForegroundService = false

    // ссылка на текущую песню
    private var curPlayingSong: MediaMetadataCompat? = null

    override fun onCreate() {
        super.onCreate()
        // we want to get the activity intent for our notification
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            // getLaunchIntentForPackage возвращает main activity
            // отложенный интент по нажатию на уведомление
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken // определяем токен для сервиса

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            //если песня переключится лямбда будет вызвана

        }

        // we want to create our playback prepare
        val musicPlaybackPrepare = MusicPlaybackPreparer(firebaseMusicSource) {
            // get the current mediaMetadataCompat object after the player is prepared
            // this lambda block will call every time the player chooses a new song
            curPlayingSong = it  //it - выбранная песня
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPrepare)
        mediaSessionConnector.setPlayer(exoPlayer)

        exoPlayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoPlayer)
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val curSongIndex = if(curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        // если нет текущей песни, подготавливаем первую из листа songs
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex, 0) // проигрывание песни сначала
        exoPlayer.playWhenReady = playNow

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {

    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

    }
}