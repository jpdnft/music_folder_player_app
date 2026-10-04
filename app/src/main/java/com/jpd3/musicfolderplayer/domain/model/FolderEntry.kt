package com.jpd3.musicfolderplayer.domain.model

import android.net.Uri

sealed class FolderEntry {
    data class Folder(
        val uri: Uri,
        val name: String,
        val childUri: Uri?
    ) : FolderEntry()

    data class AudioFile(
        val uri: Uri,
        val name: String,
        val title: String = name,
        val artist: String = "",
        val album: String = "",
        val discNumber: Int? = null,
        val trackNumber: Int? = null,
        val durationMs: Long = 0L,
        val mimeType: String? = null
    ) : FolderEntry()
}
