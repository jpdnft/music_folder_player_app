package com.jpd3.musicfolderplayer

import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaController
import com.jpd3.musicfolderplayer.playback.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PlaybackStateTest {
    private val folder = Uri.parse("content://music/tree/library/document/library%2FAlbum")
    private fun item(name: String) = musicItem(Uri.parse("$folder%2F$name.mp3"), name, folder,
        Uri.parse("$folder%2Fcover.jpg"), "Artist", "Album")

    @Test fun restartSnapshotPreservesQueueOrderSelectedTrackPositionAndMetadata() {
        val original = PlaybackSnapshot(listOf(item("Listen"), item("Silly")), 1, 42_123, true)
        val restored = PlaybackSnapshot.decode(original.encode())
        assertEquals(original.items.map { it.mediaId }, restored.items.map { it.mediaId })
        assertEquals(1, restored.index)
        assertEquals(42_123L, restored.position)
        assertTrue(restored.playWhenReady)
        assertEquals("Silly", restored.items[restored.index].mediaMetadata.title)
        assertEquals("Artist", restored.items[1].mediaMetadata.artist)
        assertEquals(folder.toString(), restored.items[1].mediaMetadata.extras?.getString(FOLDER_URI))
        assertEquals(original.items[1].mediaMetadata.artworkUri, restored.items[1].mediaMetadata.artworkUri)
    }

    @Test fun emptySnapshotDoesNotResurrectAnOldQueue() {
        val restored = PlaybackSnapshot.decode(PlaybackSnapshot(emptyList(), 0, 0).encode())
        assertTrue(restored.items.isEmpty())
    }

    @Test fun sessionUsesTheServicePlayerAndSnapshotFollowsExternalTrackChanges() {
        val lifecycle = Robolectric.buildService(MusicMediaSessionService::class.java).create()
        val service = lifecycle.get()
        try {
            val field = MusicMediaSessionService::class.java.getDeclaredField("player").apply { isAccessible = true }
            val player = field.get(service) as ExoPlayer
            val sessionField = MusicMediaSessionService::class.java.getDeclaredField("session").apply { isAccessible = true }
            val session = sessionField.get(service) as MediaLibraryService.MediaLibrarySession
            assertSame(player, session.player)
            assertEquals(C.USAGE_MEDIA, player.audioAttributes.usage)
            assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, player.audioAttributes.contentType)
            player.setMediaItems(listOf(item("Listen"), item("Silly")), 0, 0)
            // A session/car command changes the same player the service snapshots.
            session.player.seekTo(1, 19_000)
            val method = MusicMediaSessionService::class.java.getDeclaredMethod("liveSnapshot").apply { isAccessible = true }
            val snapshot = method.invoke(service) as PlaybackSnapshot
            assertEquals("Silly", snapshot.items[snapshot.index].mediaMetadata.title)
            assertEquals(19_000L, snapshot.position)
            assertFalse(player.playWhenReady)
        } finally { lifecycle.destroy() }
    }

    @Test fun connectingAnotherControllerPreservesQueueAndBothControllersFollowTrackChanges() {
        val lifecycle = Robolectric.buildService(MusicMediaSessionService::class.java).create()
        val service = lifecycle.get()
        val field = MusicMediaSessionService::class.java.getDeclaredField("session").apply { isAccessible = true }
        val session = field.get(service) as MediaLibraryService.MediaLibrarySession
        val phone = MediaController.Builder(service, session.token).buildAsync()
        var car: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(phone.isDone)
            phone.get().setMediaItems(listOf(item("Listen"), item("Silly")), 0, 25_000)
            shadowOf(Looper.getMainLooper()).idle()
            car = MediaController.Builder(service, session.token).buildAsync()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(car.isDone)
            assertEquals("Listen", car.get().mediaMetadata.title)
            assertEquals(25_000L, car.get().currentPosition)
            assertEquals(2, car.get().mediaItemCount)
            car.get().seekTo(1, 19_000)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Silly", phone.get().mediaMetadata.title)
            assertEquals(19_000L, phone.get().currentPosition)
            assertEquals(1, session.player.currentMediaItemIndex)
        } finally {
            car?.let(MediaController::releaseFuture)
            MediaController.releaseFuture(phone)
            lifecycle.destroy()
        }
    }
}
