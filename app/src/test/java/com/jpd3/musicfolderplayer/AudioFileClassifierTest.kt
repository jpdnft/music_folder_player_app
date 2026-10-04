package com.jpd3.musicfolderplayer

import com.jpd3.musicfolderplayer.util.AudioFileClassifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioFileClassifierTest {
    @Test
    fun detectsSupportedExtensions() {
        assertTrue(AudioFileClassifier.isSupportedAudioFile("track.mp3"))
        assertTrue(AudioFileClassifier.isSupportedAudioFile("album.flac"))
        assertTrue(AudioFileClassifier.isSupportedAudioFile("song.aac"))
        assertTrue(AudioFileClassifier.isSupportedAudioFile("demo.ogg"))
        assertTrue(AudioFileClassifier.isSupportedAudioFile("mix.wav"))
    }

    @Test
    fun ignoresUnsupportedFiles() {
        assertFalse(AudioFileClassifier.isSupportedAudioFile("notes.txt"))
        assertFalse(AudioFileClassifier.isSupportedAudioFile("cover.jpg"))
    }

    @Test
    fun naturalOrderingPlacesTwoBeforeTen() {
        val names = listOf("10 Song", "2 Song", "1 Song")
        val sorted = names.sortedWith { a, b -> AudioFileClassifier.naturalComparator(a, b) }
        assertTrue(sorted[0].endsWith("Song") && sorted[0].startsWith("1"))
        assertTrue(sorted[1].startsWith("2"))
        assertTrue(sorted[2].startsWith("10"))
    }
}
