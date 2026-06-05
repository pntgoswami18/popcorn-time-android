package com.popcorntime.android.data.torrent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the torrent engine alive when the user
 * backgrounds the app during streaming or seeding.
 */
@AndroidEntryPoint
class TorrentService : Service() {

    @Inject lateinit var torrentEngine: TorrentEngine

    companion object {
        const val CHANNEL_ID = "torrent_stream"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.popcorntime.android.STOP_TORRENT"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            torrentEngine.stopCurrent()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("Streaming…"))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        torrentEngine.stopCurrent()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Torrent Stream",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Active torrent download / stream" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Popcorn Time")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
