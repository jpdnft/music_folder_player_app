package com.jpd3.musicfolderplayer.domain.model

import android.net.Uri

data class TrackInfo(
    val uri: Uri,
    val name: String,
    val title: String = name,
    val artist: String = "",
    val album: String = "",
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val durationMs: Long = 0L,
    val mimeType: String? = null
)
