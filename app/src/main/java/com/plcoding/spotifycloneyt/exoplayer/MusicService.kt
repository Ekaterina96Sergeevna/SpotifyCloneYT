package com.plcoding.spotifycloneyt.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlaybackPreparer
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerEventListener
import com.plcoding.spotifycloneyt.exoplayer.callbacks.MusicPlayerNotificationListener
import com.plcoding.spotifycloneyt.other.Constants.MEDIA_ROOT_ID
import com.plcoding.spotifycloneyt.other.Constants.NETWORK_ERROR
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
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

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object{
        var curSongDuration = 0L
        private set
        // we can only change the value from within the service
        // but we can read it from outside of the service
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }

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
            // если песня переключится лямбда будет вызвана
            // update current duration of the song that is playing
            curSongDuration = exoPlayer.duration

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
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            // если песня изменилась, нужно показать для текущей песни другое описание
            return firebaseMusicSource.songs[windowIndex].description
        }
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop() // stop playing
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener) // prevent any memory leaks here
        exoPlayer.release()
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // contains information that the browser service needs to send to the client when first connected
        return BrowserRoot(MEDIA_ROOT_ID, null)

    // id - для плейлиста в альбоме (в нашем случае 1 плейлист с 3 песнями)
    // пользователи могут подписаться на id плейлиста
    }

    override fun onLoadChildren(
        parentId: String, // id к которому подписался пользователь
        result: Result<MutableList<MediaBrowserCompat.MediaItem>> // соответствующий плейлист по id
    ) {
        // в нашем случае только 1 плейлист с id
        when(parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if(isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        // нам нужно проверить инициализировался ли наш плеер
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(firebaseMusicSource.songs, firebaseMusicSource.songs[0], false)
                            // false - чтобы автоматически не проигрывалась песня при открытие приложения
                            isPlayerInitialized = true
                        }
                    } else {
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                if(!resultsSent){
                    result.detach() // позднее еще раз попробует подписаться на root_id
                }
            }
        }
    }
}