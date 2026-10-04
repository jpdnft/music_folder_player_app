package com.jpd3.musicfolderplayer.util

import android.net.Uri
import android.provider.DocumentsContract

object FolderPathUtils {
    fun buildBreadcrumb(root: Uri?, current: Uri?): List<String> {
        if (root == null || current == null) return listOf("Music")
        val rootId = DocumentsContract.getTreeDocumentId(root)
        val currentId = documentId(current)
        val name = rootId.substringAfter(':').substringAfterLast('/').ifBlank { "Music" }
        val relative = currentId.removePrefix(rootId).trim('/')
        return listOf(name) + relative.split('/').filter { it.isNotBlank() }
    }

    fun parentFolder(root: Uri?, current: Uri?): Uri? {
        if (root == null || current == null) return null
        val rootId = DocumentsContract.getTreeDocumentId(root)
        val currentId = documentId(current)
        if (currentId == rootId || !currentId.startsWith("$rootId/")) return null
        return DocumentsContract.buildDocumentUriUsingTree(root, currentId.substringBeforeLast('/'))
    }

    private fun documentId(uri: Uri): String =
        if (uri.pathSegments.contains("document")) DocumentsContract.getDocumentId(uri)
        else DocumentsContract.getTreeDocumentId(uri)
}
