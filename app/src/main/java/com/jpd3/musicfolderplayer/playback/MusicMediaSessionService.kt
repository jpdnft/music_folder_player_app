package com.jpd3.musicfolderplayer.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

class MusicMediaSessionService : Service() {
    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        const val ACTION_PLAY = "com.jpd3.musicfolderplayer.action.PLAY"
        const val EXTRA_TRACK_NAME = "extra_track_name"

        @Volatile
        private var sharedPlayer: ExoPlayer? = null

        fun getSharedPlayer(context: Context): ExoPlayer {
            synchronized(this) {
                return sharedPlayer ?: ExoPlayer.Builder(context.applicationContext).build().also {
                    sharedPlayer = it
                }
            }
        }

        fun start(context: Context, trackName: String = "Music Folder Player") {
            val intent = Intent(context, MusicMediaSessionService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_TRACK_NAME, trackName)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = getSharedPlayer(this)
        mediaSession = MediaSession.Builder(this, player!!).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val trackName = intent?.getStringExtra(EXTRA_TRACK_NAME) ?: "Music Folder Player"
        startForeground(1, buildNotification(trackName))
        return START_STICKY
    }

    fun getPlayer(): ExoPlayer = player ?: throw IllegalStateException("Player not initialized")

    fun playMediaItem(mediaItem: MediaItem) {
        getPlayer().setMediaItem(mediaItem)
        getPlayer().prepare()
        getPlayer().play()
    }

    private fun buildNotification(trackName: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "music_folder_player_channel",
                "Music Folder Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "music_folder_player_channel")
            .setContentTitle("Music Folder Player")
            .setContentText(trackName)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicMediaSessionService = this@MusicMediaSessionService
    }
}
