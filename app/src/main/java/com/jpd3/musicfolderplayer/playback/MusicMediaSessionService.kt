package com.jpd3.musicfolderplayer.playback

import android.app.PendingIntent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.jpd3.musicfolderplayer.data.preferences.DataStoreAppPreferencesStore
import com.jpd3.musicfolderplayer.data.storage.RealMusicDocumentStore
import com.jpd3.musicfolderplayer.util.FolderPathUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first

@androidx.annotation.OptIn(UnstableApi::class)
class MusicMediaSessionService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var preferences: DataStoreAppPreferencesStore
    private lateinit var documents: RealMusicDocumentStore
    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshots = Channel<String>(Channel.CONFLATED)
    private val restored = CompletableDeferred<Unit>()
    private var revision = 0L
    private var restoring = true

    override fun onCreate() {
        super.onCreate()
        preferences = DataStoreAppPreferencesStore(this)
        documents = RealMusicDocumentStore(this)
        player = ExoPlayer.Builder(this).setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                revision++
                if (!restoring) saveSnapshot()
            }
        })
        val activity = PendingIntent.getActivity(this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(activity).build()
        writerScope.launch {
            try { for (snapshot in snapshots) preferences.saveSnapshot(snapshot) }
            finally { writerScope.cancel() }
        }
        scope.launch {
            val initialRevision = revision
            try {
                val saved = preferences.preferences.first()
                val snapshot = saved.playbackSnapshot?.let(PlaybackSnapshot::decode)
                    ?: saved.queueFolderUri?.takeIf { saved.hasResumableQueue && it.isNotBlank() }?.let { folder ->
                        val items = folderTracks(Uri.parse(folder))
                        val index = items.indexOfFirst { it.mediaId == saved.currentTrackUri }.let {
                            if (it >= 0) it else saved.currentTrackIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)) }
                        PlaybackSnapshot(items, index, saved.playbackPositionMs)
                    }
                // Never let disk state replace a queue selected while storage was loading.
                if (revision == initialRevision && player.mediaItemCount == 0 && snapshot?.items?.isNotEmpty() == true) {
                    player.setMediaItems(snapshot.items, snapshot.index, snapshot.position)
                    // Restore paused. Only an explicit play command may start audio.
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Missing files or revoked folder access must not break live playback.
            } finally {
                restoring = false
                restored.complete(Unit)
                if (player.mediaItemCount > 0 || revision != initialRevision) saveSnapshot()
            }
        }
        scope.launch { while (isActive) { delay(2000); if (!restoring && player.isPlaying) saveSnapshot() } }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    private fun liveSnapshot() = PlaybackSnapshot((0 until player.mediaItemCount).map(player::getMediaItemAt),
        player.currentMediaItemIndex.coerceAtLeast(0), player.currentPosition.coerceAtLeast(0), player.playWhenReady)

    private fun saveSnapshot() { snapshots.trySend(liveSnapshot().encode()) }

    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        val job = scope.launch { try { result.set(block()) } catch (error: Exception) { result.setException(error) } }
        job.invokeOnCompletion { if (!result.isDone) result.cancel(false) }
        return result
    }

    private fun folderItem(uri: Uri, title: String) = MediaItem.Builder().setMediaId(uri.toString())
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false).build()).build()

    private suspend fun folderTracks(folder: Uri): List<MediaItem> {
        val listing = documents.listFolder(folder)
        val artwork = documents.findArtwork(folder)
        return listing.audioFiles.map { musicItem(it.uri, it.title, folder, artwork, it.artist, it.album) }
    }

    private suspend fun root(): Uri = preferences.preferences.first().rootTreeUri
        ?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: throw IllegalStateException("Choose a music library on the phone")

    private suspend fun checkedUri(id: String): Uri {
        val uri = Uri.parse(id)
        require(uri.scheme == "content" && uri.authority == root().authority &&
            android.provider.DocumentsContract.getTreeDocumentId(uri) == android.provider.DocumentsContract.getTreeDocumentId(root()))
        return uri
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onPlayerInteractionFinished(session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo, playerCommands: Player.Commands) {
            if (playerCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS) ||
                playerCommands.contains(Player.COMMAND_SET_MEDIA_ITEM)) {
                // Even clearing an already-empty player must invalidate an in-flight disk restore.
                revision++
                if (!restoring) saveSnapshot()
            }
        }

        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> =
            future { LibraryResult.ofItem(folderItem(root(), "Music folders"), params) }

        override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val folder = checkedUri(parentId)
            val listing = documents.listFolder(folder)
            val folders = listing.folders.map { folderItem(it.uri, it.name) }
            val tracks = if (FolderPathUtils.parentFolder(root(), folder) == null) emptyList() else folderTracks(folder)
            val all = folders + tracks
            val start = (page.toLong() * pageSize).coerceAtMost(all.size.toLong()).toInt()
            LibraryResult.ofItemList(all.subList(start, (start.toLong() + pageSize).coerceAtMost(all.size.toLong()).toInt()), params)
        }

        override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
            mediaId: String): ListenableFuture<LibraryResult<MediaItem>> = future {
            val uri = checkedUri(mediaId)
            if (androidx.documentfile.provider.DocumentFile.fromSingleUri(this@MusicMediaSessionService, uri)?.isDirectory == true || uri == root())
                LibraryResult.ofItem(folderItem(uri, "Music folders"), null)
            else {
                val folder = FolderPathUtils.parentFolder(root(), uri) ?: throw IllegalArgumentException("Invalid track")
                LibraryResult.ofItem(folderTracks(folder).first { it.mediaId == mediaId }, null)
            }
        }

        override fun onSetMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            // UI sends a complete queue. A car selection by ID resolves to that track's complete folder.
            if (mediaItems.all { it.localConfiguration != null }) {
                revision++
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            } else {
                val query = mediaItems.firstOrNull()?.requestMetadata?.searchQuery
                if (query != null) {
                    restored.await()
                    val live = liveSnapshot()
                    val index = if (query.isBlank()) live.index else live.items.indexOfFirst {
                        listOf(it.mediaMetadata.title, it.mediaMetadata.artist, it.mediaMetadata.albumTitle)
                            .any { value -> value?.contains(query, ignoreCase = true) == true }
                    }
                    require(index in live.items.indices) { "No matching track in the current queue" }
                    return@future MediaSession.MediaItemsWithStartPosition(live.items, index,
                        if (index == live.index) live.position else 0L)
                }
                revision++
                val selected = mediaItems.firstOrNull()?.mediaId ?: throw IllegalArgumentException("No track")
                val live = liveSnapshot()
                val liveIndex = live.items.indexOfFirst { it.mediaId == selected }
                if (mediaItems.size == 1 && liveIndex >= 0) {
                    // Resolving a cached ID uses the live queue, never a reconstructed/stale one.
                    val position = if (startPositionMs != C.TIME_UNSET) startPositionMs
                        else if (liveIndex == live.index) live.position else 0L
                    return@future MediaSession.MediaItemsWithStartPosition(live.items, liveIndex, position)
                }
                val initialRevision = revision
                val uri = checkedUri(selected)
                val folder = FolderPathUtils.parentFolder(root(), uri) ?: throw IllegalArgumentException("Invalid track")
                val items = folderTracks(folder)
                val index = items.indexOfFirst { it.mediaId == selected }
                require(index >= 0)
                check(revision == initialRevision) { "Playback changed while resolving the requested track" }
                MediaSession.MediaItemsWithStartPosition(items, index, startPositionMs)
            }
        }

        override fun onPlaybackResumption(mediaSession: MediaSession, controller: MediaSession.ControllerInfo):
            ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            restored.await()
            val snapshot = liveSnapshot()
            require(snapshot.items.isNotEmpty()) { "No saved queue" }
            MediaSession.MediaItemsWithStartPosition(snapshot.items, snapshot.index, snapshot.position)
        }
    }

    override fun onDestroy() {
        if (!restoring) saveSnapshot()
        // The serial writer drains the final snapshot without blocking the main thread.
        snapshots.close()
        scope.cancel()
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }
}
