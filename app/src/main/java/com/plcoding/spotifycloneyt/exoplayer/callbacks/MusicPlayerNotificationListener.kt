package com.plcoding.spotifycloneyt.exoplayer.callbacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.plcoding.spotifycloneyt.exoplayer.MusicService
import com.plcoding.spotifycloneyt.other.Constants.NOTIFICATION_ID

// управляем notification (показываем, убираем)
class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            // убираем service из foreground state, и убираем notification
            stopForeground(true)
            isForegroundService = false
            stopSelf() // stop service
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean // true - music run
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            // если музыка играет, но нет ForegroundService -> ставим его видимым пользователю
            if(ongoing && !isForegroundService){
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)
                )
                //launch the notification
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
            }
        }
    }
}