package com.plcoding.spotifycloneyt.exoplayer.callbacks

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.plcoding.spotifycloneyt.exoplayer.MusicService

class MusicPlayerEventListener(
    // we want to be able to stop the foreground service from within this listener
    private val musicService: MusicService
) : Player.EventListener {
    // listener that has optional implementations

    // запускается когда изменяется состояние плеера
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        // если все подготовлено и готово, но не играет
        if(playbackState == Player.STATE_READY && !playWhenReady){
            // убираем service из foreground state -  но notification остается
            musicService.stopForeground(false)
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "An unknown error occured", Toast.LENGTH_LONG).show()
    }
}