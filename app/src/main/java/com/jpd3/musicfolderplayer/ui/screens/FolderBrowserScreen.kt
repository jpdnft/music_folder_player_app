package com.jpd3.musicfolderplayer.ui.screens

import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.jpd3.musicfolderplayer.ui.theme.PlayerButton as Button
import androidx.compose.material3.MaterialTheme
import com.jpd3.musicfolderplayer.ui.theme.PlayerOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jpd3.musicfolderplayer.data.storage.FolderTraversal
import com.jpd3.musicfolderplayer.data.storage.RealMusicDocumentStore
import com.jpd3.musicfolderplayer.domain.model.TrackInfo
import com.jpd3.musicfolderplayer.util.FolderPathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun FolderBrowserScreen(
    navController: NavController,
    rootUri: Uri?,
    currentFolderUri: Uri?,
    onChooseFolder: () -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onTrackSelected: (TrackInfo, List<TrackInfo>) -> Unit
) {
    val context = LocalContext.current
    val documentStore = remember { RealMusicDocumentStore(context.applicationContext) }
    val artworkLoads = remember { Semaphore(2) }
    var traversal by remember(rootUri, currentFolderUri) { mutableStateOf<FolderTraversal?>(null) }
    var loadFailed by remember(rootUri, currentFolderUri) { mutableStateOf(false) }

    LaunchedEffect(rootUri, currentFolderUri) {
        traversal = null
        val selectedUri = currentFolderUri ?: rootUri ?: return@LaunchedEffect
        try {
            traversal = documentStore.listFolder(selectedUri)
        } catch (_: java.io.IOException) {
            loadFailed = true
        } catch (_: SecurityException) {
            loadFailed = true
        }
    }

    val currentUri = currentFolderUri ?: rootUri
    val breadcrumb = FolderPathUtils.buildBreadcrumb(rootUri, currentUri)
    val parentUri = FolderPathUtils.parentFolder(rootUri, currentUri)
    BackHandler(enabled = parentUri != null) { parentUri?.let(onFolderSelected) }
    val trackList = remember(traversal) { traversal?.audioFiles?.map { file ->
        TrackInfo(
            uri = file.uri,
            name = file.name,
            title = file.title,
            artist = file.artist,
            album = file.album,
            mimeType = file.mimeType
        )
    } ?: emptyList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse folders",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = traversal?.currentFolderName ?: "Music Library",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
        Text(
            text = breadcrumb.joinToString(" / ").ifEmpty { "Music" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (parentUri != null) {
                OutlinedButton(onClick = { onFolderSelected(parentUri) }) { Text("Up one folder") }
            }
            OutlinedButton(
                onClick = onChooseFolder,
                modifier = Modifier.weight(1f)
            ) {
                Text("Library settings")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loadFailed) {
                item { Text("Unable to read this folder. Check your library settings.") }
            } else if (traversal == null) {
                item { Text("Loading folders...") }
            }
            if (traversal?.folders?.isNotEmpty() == true) {
                item {
                    Text("Open a folder to browse its albums or tracks", style = MaterialTheme.typography.bodyMedium)
                }
                items(traversal!!.folders, key = { it.uri.toString() }) { folder ->
                    OutlinedButton(
                        onClick = {
                            onFolderSelected(folder.uri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FolderArtwork(folder.uri, documentStore, artworkLoads)
                            Text(folder.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (trackList.isNotEmpty()) {
                item {
                    Text(
                        "Files in ${traversal?.currentFolderName}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(trackList, key = { it.uri.toString() }) { track ->
                    OutlinedButton(
                        onClick = { onTrackSelected(track, trackList) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(track.name)
                    }
                }
            }

            if ((traversal?.folders?.isEmpty() == true) && trackList.isEmpty()) {
                item {
                    Text("No music or folders found in this location.")
                }
            }
        }
        Button(
            onClick = { trackList.firstOrNull()?.let { onTrackSelected(it, trackList) } },
            enabled = trackList.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Play this Folder")
        }
    }
}

@Composable
private fun FolderArtwork(folderUri: Uri, documentStore: RealMusicDocumentStore, loads: Semaphore) {
    val resolver = LocalContext.current.contentResolver
    var artwork by remember(folderUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(folderUri) {
        artwork = loads.withPermit { withContext(Dispatchers.IO) {
            try {
                val uri = documentStore.findArtwork(folderUri) ?: return@withContext null
                ensureActive()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                while (bounds.outWidth / options.inSampleSize > 256 || bounds.outHeight / options.inSampleSize > 256) {
                    options.inSampleSize *= 2
                }
                resolver.openInputStream(uri)?.use {
                    ensureActive()
                    BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
                }
            } catch (_: java.io.IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        } }
    }
    artwork?.let {
        Image(
            bitmap = it, contentDescription = null,
            modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop
        )
    }
}
