package com.jpd3.musicfolderplayer

import android.net.Uri
import com.jpd3.musicfolderplayer.util.FolderPathUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderPathUtilsTest {
    @Test
    fun buildsBreadcrumbFromRootAndCurrentFolder() {
        val root = Uri.parse("content://tree/root")
        val current = Uri.parse("content://tree/root/artist/album")
        val breadcrumb = FolderPathUtils.buildBreadcrumb(root, current)
        assertEquals(listOf("artist", "album"), breadcrumb)
    }
}
