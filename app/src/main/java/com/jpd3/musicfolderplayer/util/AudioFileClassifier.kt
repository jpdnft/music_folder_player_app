package com.jpd3.musicfolderplayer.util

import android.webkit.MimeTypeMap
import java.util.Locale

object AudioFileClassifier {
    private val supportedExtensions = setOf(
        "mp3", "flac", "aac", "m4a", "ogg", "oga", "opus", "wav", "aiff", "aif",
        "amr", "ac3", "eac3"
    )

    fun isSupportedAudioFile(name: String, mimeType: String? = null): Boolean {
        val normalized = name.lowercase(Locale.US)
        val extension = normalized.substringAfterLast('.', "")
        val mimeByExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        val loweredMime = mimeType?.lowercase(Locale.US)

        if (loweredMime != null && loweredMime.startsWith("audio/")) return true
        if (mimeByExtension != null && mimeByExtension.startsWith("audio/")) return true
        return extension in supportedExtensions
    }

    fun naturalComparator(a: String, b: String): Int {
        val normalizedA = a.lowercase(Locale.US)
        val normalizedB = b.lowercase(Locale.US)
        val regex = Regex("\\d+")
        val aMatches = regex.findAll(normalizedA).map { it.value }.toList()
        val bMatches = regex.findAll(normalizedB).map { it.value }.toList()
        val max = minOf(aMatches.size, bMatches.size)

        for (index in 0 until max) {
            val compare = aMatches[index].toLong().compareTo(bMatches[index].toLong())
            if (compare != 0) return compare
        }

        val lengthCompare = normalizedA.length.compareTo(normalizedB.length)
        if (lengthCompare != 0) return lengthCompare
        return normalizedA.compareTo(normalizedB)
    }
}
