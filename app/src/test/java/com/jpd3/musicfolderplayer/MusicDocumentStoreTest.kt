package com.jpd3.musicfolderplayer

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import org.robolectric.RuntimeEnvironment
import com.jpd3.musicfolderplayer.data.storage.RealMusicDocumentStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicDocumentStoreTest {
    private val provider = ListingProvider()
    private val root = Uri.parse("content://test.documents/tree/primary%3AMUSIC")
    private lateinit var store: RealMusicDocumentStore

    @Before
    fun setUp() {
        ShadowContentResolver.registerProviderInternal("test.documents", provider)
        store = RealMusicDocumentStore(RuntimeEnvironment.getApplication())
    }

    @Test
    fun hundredsOfAlbumsRequireOnlyNameAndListingQueries() = runTest {
        val listing = store.listFolder(root)
        assertEquals(500, listing.folders.size)
        assertEquals(listOf("01. Song.mp3"), listing.audioFiles.map { it.name })
        assertEquals(2, provider.queries.size)
        assertEquals(1, provider.queries.count { it.endsWith("/children") })
        assertTrue(listing.folders.all { it.artworkUri == null })
    }

    @Test
    fun artworkLookupReadsOnlyTheRequestedFolderAndAcceptsImageMimeTypes() = runTest {
        val album = Uri.parse("content://test.documents/tree/primary%3AMUSIC%2FAlbum")
        val artwork = store.findArtwork(album)
        assertEquals("primary:MUSIC/Album/cover", DocumentsContract.getDocumentId(artwork!!))
        assertEquals(1, provider.queries.size)
        assertTrue(provider.queries.single().contains("primary%3AMUSIC%2FAlbum/children"))
    }

    @Test
    fun libraryRootHidesStandaloneTracksAndThumbnailFolder() = runTest {
        val listing = store.getRootFolder(root)
        assertTrue(listing.audioFiles.isEmpty())
        assertFalse(listing.folders.any { it.name == ".thumbnails" })
        assertEquals(500, listing.folders.size)
    }

    @Test
    fun recursiveArtworkFindsAlbumImageButDirectLookupDoesNot() = runTest {
        val artist = Uri.parse("content://test.documents/tree/primary%3AMUSIC/document/primary%3AMUSIC%2FArtist")
        assertNull(store.findArtwork(artist))
        provider.queries.clear()
        val artwork = store.findArtwork(artist, recursive = true)
        assertEquals("primary:MUSIC/Album/cover", DocumentsContract.getDocumentId(artwork!!))
        assertEquals(3, provider.queries.size)
        assertFalse(provider.queries.any { it.contains(".thumbnails/children") })
    }

    @Test
    fun recursiveArtworkReturnsNullWhenDescendantsHaveNoImages() = runTest {
        val empty = Uri.parse("content://test.documents/tree/primary%3AMUSIC/document/primary%3AMUSIC%2FEmpty")
        assertNull(store.findArtwork(empty, recursive = true))
    }

    class ListingProvider : ContentProvider() {
        val queries = mutableListOf<String>()
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            queries += uri.toString()
            val columns = projection ?: arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            return MatrixCursor(columns).apply {
                fun row(id: String, name: String, mime: String) {
                    addRow(columns.map { when (it) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> id
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> name
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> mime
                        else -> null
                    } }.toTypedArray())
                }
                if (!uri.toString().endsWith("/children")) {
                    row("primary:MUSIC", "MUSIC", DocumentsContract.Document.MIME_TYPE_DIR)
                } else if (uri.toString().contains("%2FArtist/children")) {
                    row("primary:MUSIC/.thumbnails", ".thumbnails", DocumentsContract.Document.MIME_TYPE_DIR)
                    row("primary:MUSIC/Empty", "A-empty", DocumentsContract.Document.MIME_TYPE_DIR)
                    row("primary:MUSIC/Album", "B-album", DocumentsContract.Document.MIME_TYPE_DIR)
                } else if (uri.toString().contains("%2FEmpty/children")) {
                    row("primary:MUSIC/Empty/song", "song.mp3", "audio/mpeg")
                } else if (uri.toString().contains("%2FAlbum/children")) {
                    row("primary:MUSIC/Album/song", "song.mp3", "audio/mpeg")
                    row("primary:MUSIC/Album/cover", "cover", "image/jpeg")
                    row("primary:MUSIC/Album/subfolder", "nested.png", DocumentsContract.Document.MIME_TYPE_DIR)
                } else {
                    row("primary:MUSIC/.thumbnails", ".thumbnails", DocumentsContract.Document.MIME_TYPE_DIR)
                    repeat(500) { row("primary:MUSIC/Album$it", "Album$it", DocumentsContract.Document.MIME_TYPE_DIR) }
                    row("primary:MUSIC/song", "01. Song.mp3", "audio/mpeg")
                    row("primary:MUSIC/notes", "notes.txt", "text/plain")
                }
            }
        }
        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}
