package com.jpd3.musicfolderplayer.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.jpd3.musicfolderplayer.domain.model.FolderEntry
import com.jpd3.musicfolderplayer.util.AudioFileClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

interface MusicDocumentStore {
    suspend fun listFolder(uri: Uri): FolderTraversal
    suspend fun findArtwork(uri: Uri): Uri?
    suspend fun getRootFolder(uri: Uri): FolderTraversal
    suspend fun ensureReadable(uri: Uri): Boolean
}

data class FolderTraversal(
    val currentFolderName: String,
    val pathSegments: List<String>,
    val folders: List<FolderEntry.Folder>,
    val audioFiles: List<FolderEntry.AudioFile>
)

class RealMusicDocumentStore(private val context: Context) : MusicDocumentStore {
    private data class Document(val uri: Uri, val name: String, val mime: String?) {
        val isDirectory: Boolean get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
    }

    override suspend fun listFolder(uri: Uri): FolderTraversal = withContext(Dispatchers.IO) {
        val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Folder"
        // Fetch names and MIME types together, rather than querying each property of every file.
        val children = readChildren(uri).sortedBy { it.name.lowercase() }
        FolderTraversal(
            currentFolderName = name,
            pathSegments = listOf(name),
            folders = children.filter { it.isDirectory }.map {
                FolderEntry.Folder(uri = it.uri, name = it.name, childUri = it.uri)
            },
            audioFiles = children.filter { !it.isDirectory && AudioFileClassifier.isSupportedAudioFile(it.name, it.mime) }
                .map { FolderEntry.AudioFile(uri = it.uri, name = it.name, mimeType = it.mime) }
        )
    }

    override suspend fun findArtwork(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        readChildren(uri).filter { !it.isDirectory &&
            (it.mime?.startsWith("image/") == true || it.name.substringAfterLast('.', "").lowercase() in imageExtensions)
        }.minByOrNull { it.name.lowercase() }?.uri
    }

    private suspend fun readChildren(uri: Uri): List<Document> {
        coroutineContext.ensureActive()
        val id = if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(uri)
            else DocumentsContract.getTreeDocumentId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, id)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        return buildList {
            val cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
                ?: throw java.io.IOException("Unable to read folder")
            cursor.use {
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    add(Document(DocumentsContract.buildDocumentUriUsingTree(uri, it.getString(0)),
                        it.getString(1) ?: "Unnamed", it.getString(2)))
                }
            }
        }
    }

    override suspend fun getRootFolder(uri: Uri): FolderTraversal = listFolder(uri)

    override suspend fun ensureReadable(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        DocumentFile.fromTreeUri(context, uri)?.canRead() == true
    }

    private companion object {
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
    }
}
