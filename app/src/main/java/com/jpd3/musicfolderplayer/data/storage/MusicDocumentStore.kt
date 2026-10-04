package com.jpd3.musicfolderplayer.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jpd3.musicfolderplayer.domain.model.FolderEntry
import com.jpd3.musicfolderplayer.util.AudioFileClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MusicDocumentStore {
    suspend fun listFolder(uri: Uri): FolderTraversal
    suspend fun getRootFolder(uri: Uri): FolderTraversal
    suspend fun ensureReadable(uri: Uri): Boolean
}

data class FolderTraversal(
    val currentFolderName: String,
    val pathSegments: List<String>,
    val folders: List<FolderEntry.Folder>,
    val audioFiles: List<FolderEntry.AudioFile>
)

class RealMusicDocumentStore(
    private val context: Context
) : MusicDocumentStore {
    override suspend fun listFolder(uri: Uri): FolderTraversal = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext FolderTraversal(
            currentFolderName = "Unavailable",
            pathSegments = emptyList(),
            folders = emptyList(),
            audioFiles = emptyList()
        )

        val documents = root.listFiles().orEmpty()
        val children = documents.sortedBy { it.name?.lowercase() ?: "" }
        val folders = children.filter { it.isDirectory }.map {
            FolderEntry.Folder(uri = it.uri, name = it.name ?: "Folder", childUri = it.uri)
        }
        val audioFiles = children.filterNot { it.isDirectory }.filter { item ->
            val name = item.name ?: return@filter false
            val mime = item.type
            AudioFileClassifier.isSupportedAudioFile(name, mime)
        }.map { item ->
            FolderEntry.AudioFile(
                uri = item.uri,
                name = item.name ?: "Track",
                title = item.name ?: "Track",
                mimeType = item.type
            )
        }

        FolderTraversal(
            currentFolderName = root.name ?: "Folder",
            pathSegments = listOf(root.name ?: "Folder"),
            folders = folders,
            audioFiles = audioFiles
        )
    }

    override suspend fun getRootFolder(uri: Uri): FolderTraversal = listFolder(uri)

    override suspend fun ensureReadable(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        DocumentFile.fromTreeUri(context, uri) != null
    }
}
