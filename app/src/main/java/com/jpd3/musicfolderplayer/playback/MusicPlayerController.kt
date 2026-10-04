package com.jpd3.musicfolderplayer.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.jpd3.musicfolderplayer.domain.model.TrackInfo

class MusicPlayerController(context: Context) {
    private val player: ExoPlayer = MusicMediaSessionService.getSharedPlayer(context)
    private val trackQueue = mutableListOf<TrackInfo>()
    private var currentIndex: Int = -1

    fun setQueue(tracks: List<TrackInfo>) {
        trackQueue.clear()
        trackQueue.addAll(tracks)
        if (trackQueue.isEmpty()) {
            currentIndex = -1
            player.clearMediaItems()
            return
        }

        currentIndex = 0
        val mediaItems = trackQueue.map { MediaItem.fromUri(it.uri) }
        player.setMediaItems(mediaItems, 0, 0L)
        player.prepare()
    }

    fun playTrack(track: TrackInfo) {
        if (trackQueue.isEmpty()) {
            setQueue(listOf(track))
            return
        }

        val nextIndex = trackQueue.indexOfFirst { it.uri == track.uri }
        if (nextIndex >= 0) {
            currentIndex = nextIndex
            player.seekTo(nextIndex, 0L)
            player.prepare()
        }
        player.play()
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else if (trackQueue.isNotEmpty()) {
            player.play()
        }
    }

    fun previousTrack() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            currentIndex = player.currentMediaItemIndex
        }
    }

    fun nextTrack() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            currentIndex = player.currentMediaItemIndex
        }
    }

    fun currentTrackTitle(): String = trackQueue.getOrNull(currentIndex)?.name ?: "Demo Track"

    fun isPlaying(): Boolean = player.isPlaying

    fun release() {
        // The shared ExoPlayer instance is owned by the service. The controller should not release it
        // here, or it will interrupt background playback for the rest of the app lifecycle.
    }
}
