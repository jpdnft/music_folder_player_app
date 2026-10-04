package com.jpd3.musicfolderplayer

import android.net.Uri
import com.jpd3.musicfolderplayer.util.FolderPathUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import android.provider.DocumentsContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderPathUtilsTest {
    @Test
    fun buildsBreadcrumbFromRootAndCurrentFolder() {
        val root = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMUSIC")
        val current = DocumentsContract.buildDocumentUriUsingTree(root, "primary:MUSIC/Amos, Tori/Little Earthquakes")
        val breadcrumb = FolderPathUtils.buildBreadcrumb(root, current)
        assertEquals(listOf("MUSIC", "Amos, Tori", "Little Earthquakes"), breadcrumb)
    }

    @Test
    fun parentNavigationStopsAtSavedLibrary() {
        val root = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMUSIC")
        val album = DocumentsContract.buildDocumentUriUsingTree(root, "primary:MUSIC/Amos, Tori/Little Earthquakes")
        val artist = FolderPathUtils.parentFolder(root, album)
        assertEquals("primary:MUSIC/Amos, Tori", DocumentsContract.getDocumentId(artist!!))
        val library = FolderPathUtils.parentFolder(root, artist)
        assertEquals(listOf("MUSIC"), FolderPathUtils.buildBreadcrumb(root, library))
        assertNull(FolderPathUtils.parentFolder(root, library))
        assertNull(FolderPathUtils.parentFolder(root, root))
    }
}
