package com.jpd3.musicfolderplayer

import android.net.Uri
import com.jpd3.musicfolderplayer.domain.model.TrackInfo
import com.jpd3.musicfolderplayer.util.TrackSort
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackSortTest {
    @Test
    fun sortsByDiscThenTrackThenFilename() {
        val tracks = listOf(
            TrackInfo(Uri.parse("content://track/10"), "10 Song", discNumber = 1, trackNumber = 10),
            TrackInfo(Uri.parse("content://track/02"), "2 Song", discNumber = 1, trackNumber = 2),
            TrackInfo(Uri.parse("content://track/01"), "1 Song", discNumber = 1, trackNumber = 1),
            TrackInfo(Uri.parse("content://track/3"), "3 Song", discNumber = 1, trackNumber = 3)
        )

        val sorted = TrackSort.sortTracks(tracks)
        assertEquals("1 Song", sorted[0].name)
        assertEquals("2 Song", sorted[1].name)
        assertEquals("3 Song", sorted[2].name)
        assertEquals("10 Song", sorted[3].name)
    }
}
