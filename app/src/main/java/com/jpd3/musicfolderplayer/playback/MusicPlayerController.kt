package com.jpd3.musicfolderplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.jpd3.musicfolderplayer.data.storage.RealMusicDocumentStore
import com.jpd3.musicfolderplayer.domain.model.TrackInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val title: String = "No track selected", val artist: String = "", val album: String = "",
    val artwork: Uri? = null, val mediaId: String? = null, val playing: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val seekable: Boolean = false,
    val connected: Boolean = false, val error: String? = null
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicPlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackState())
    val state = mutableState.asStateFlow()
    private var player: MediaController? = null
    private var selectionRevision = 0L
    private val connection = MediaController.Builder(appContext,
        SessionToken(appContext, ComponentName(appContext, MusicMediaSessionService::class.java)))
        .setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                player = null
                mutableState.value = PlaybackState(error = "Playback disconnected. Reopen the app to reconnect.")
            }
        }).buildAsync()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { refresh() }
    }

    init {
        connection.addListener({
            try {
                player = connection.get().also { it.addListener(listener) }
                refresh()
            } catch (_: Exception) {
                mutableState.value = PlaybackState(error = "Unable to connect to playback")
            }
        }, ContextCompat.getMainExecutor(appContext))
        scope.launch { while (isActive) { refresh(); delay(250) } }
    }

    private fun refresh() {
        val controller = player ?: return
        val metadata = controller.mediaMetadata
        mutableState.value = PlaybackState(
            title = metadata.title?.toString() ?: "No track selected",
            artist = metadata.artist?.toString().orEmpty(), album = metadata.albumTitle?.toString().orEmpty(),
            artwork = metadata.artworkUri, mediaId = controller.currentMediaItem?.mediaId,
            playing = controller.isPlaying, position = controller.currentPosition.coerceAtLeast(0),
            duration = controller.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0,
            seekable = controller.isCurrentMediaItemSeekable, connected = true,
            error = controller.playerError?.localizedMessage)
    }

    fun playFolderTrack(folder: Uri, tracks: List<TrackInfo>, selected: TrackInfo) {
        val controller = player ?: return
        val requestedRevision = ++selectionRevision
        scope.launch {
            val artwork = RealMusicDocumentStore(appContext).findArtwork(folder)
            if (player !== controller || selectionRevision != requestedRevision) return@launch
            val index = tracks.indexOfFirst { it.uri == selected.uri }
            if (index < 0) return@launch
            val items = tracks.map { musicItem(it.uri, it.title, folder, artwork, it.artist, it.album) }
            val sameQueue = controller.mediaItemCount == items.size && items.indices.all {
                controller.getMediaItemAt(it).mediaId == items[it].mediaId }
            if (sameQueue) controller.seekTo(index, 0L) else controller.setMediaItems(items, index, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun playPause() { player?.let {
        if (it.playWhenReady) it.pause() else {
            if (it.mediaItemCount > 0) it.prepare()
            it.play()
        }
    } }
    fun previousTrack() { player?.seekToPreviousMediaItem() }
    fun nextTrack() { player?.seekToNextMediaItem() }
    fun seekTo(position: Long) { player?.let { if (it.isCurrentMediaItemSeekable) it.seekTo(position.coerceIn(0, state.value.duration)) } }
    fun clear() { selectionRevision++; player?.let { it.pause(); it.clearMediaItems() } }
    fun release() {
        scope.cancel()
        player?.removeListener(listener)
        player = null
        MediaController.releaseFuture(connection)
    }
}
