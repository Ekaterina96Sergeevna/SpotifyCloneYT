package com.plcoding.spotifycloneyt.exoplayer.callbacks

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.plcoding.spotifycloneyt.exoplayer.FirebaseMusicSource

// подготовить плеер для воспроизведения
class MusicPlaybackPreparer(
    // we need access in class FirebaseMusicSource
    private val firebaseMusicSource: FirebaseMusicSource,
    // lambda function that will just be called once our player is prepared
    private val playerPrepared: (MediaMetadataCompat?) -> Unit
    // if player is prepared we will call this function here
    // we just pass the media metadata compat object
) : MediaSessionConnector.PlaybackPreparer {

    override fun onCommand(
        player: Player,
        controlDispatcher: ControlDispatcher,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = false

    // возвращает actions которые плеер может делать (в нашем случае подготовить песню, воспроизвести)
    override fun getSupportedPrepareActions(): Long {
        // Long - kind of a flag
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
    }

    override fun onPrepare(playWhenReady: Boolean) = Unit

    // in this fun we'll prepare the specific song the user selected to play
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        // если песни загружены
        firebaseMusicSource.whenReady {
            // itemToPlay - song, which user chooses and now wants to play
            // находим эту песню в firebaseMusicSource
            val itemToPlay = firebaseMusicSource.songs.find { mediaId == it.description.mediaId }
            playerPrepared(itemToPlay) // передаем эту песню в лямбда выражение класса
        }
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

}