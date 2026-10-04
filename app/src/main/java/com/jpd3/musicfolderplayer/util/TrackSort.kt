package com.jpd3.musicfolderplayer.util

import com.jpd3.musicfolderplayer.domain.model.TrackInfo

object TrackSort {
    fun sortTracks(tracks: List<TrackInfo>): List<TrackInfo> {
        return tracks.sortedWith { left, right ->
            val discCompare = (left.discNumber ?: 0).compareTo(right.discNumber ?: 0)
            if (discCompare != 0) return@sortedWith discCompare
            val trackCompare = (left.trackNumber ?: Int.MAX_VALUE).compareTo(right.trackNumber ?: Int.MAX_VALUE)
            if (trackCompare != 0) return@sortedWith trackCompare
            AudioFileClassifier.naturalComparator(left.name, right.name)
        }
    }
}
