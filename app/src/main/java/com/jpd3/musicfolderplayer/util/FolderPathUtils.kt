package com.jpd3.musicfolderplayer.util

import android.net.Uri

object FolderPathUtils {
    fun buildBreadcrumb(root: Uri?, current: Uri?): List<String> {
        val segments = mutableListOf<String>()
        val currentPath = current?.pathSegments ?: emptyList()
        val rootPath = root?.pathSegments ?: emptyList()
        val relevant = currentPath.drop(rootPath.size)
        for (segment in relevant) {
            if (segment.isNotBlank()) segments += segment
        }
        return segments.ifEmpty { listOf("Music") }
    }
}
