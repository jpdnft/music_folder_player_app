package com.jpd3.musicfolderplayer.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jpd3.musicfolderplayer.data.storage.FolderTraversal
import com.jpd3.musicfolderplayer.data.storage.RealMusicDocumentStore
import com.jpd3.musicfolderplayer.domain.model.TrackInfo
import com.jpd3.musicfolderplayer.util.FolderPathUtils

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
    var traversal by remember { mutableStateOf<FolderTraversal?>(null) }

    LaunchedEffect(rootUri, currentFolderUri) {
        val selectedUri = currentFolderUri ?: rootUri ?: return@LaunchedEffect
        traversal = documentStore.listFolder(selectedUri)
    }

    val currentUri = currentFolderUri ?: rootUri
    val breadcrumb = FolderPathUtils.buildBreadcrumb(rootUri, currentUri)
    val trackList = traversal?.audioFiles?.map { file ->
        TrackInfo(
            uri = file.uri,
            name = file.name,
            title = file.title,
            artist = file.artist,
            album = file.album,
            mimeType = file.mimeType
        )
    } ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Current folder",
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
            OutlinedButton(
                onClick = onChooseFolder,
                modifier = Modifier.weight(1f)
            ) {
                Text("Change folder")
            }
            Button(
                onClick = {
                    if (trackList.isNotEmpty()) {
                        onTrackSelected(trackList.first(), trackList)
                    } else {
                        navController.navigate(Screen.NowPlaying.route)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Play folder")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (traversal?.folders?.isNotEmpty() == true) {
                item {
                    Text("Folders", style = MaterialTheme.typography.titleSmall)
                }
                items(traversal!!.folders) { folder ->
                    Button(
                        onClick = {
                            onFolderSelected(folder.uri)
                            navController.navigate(Screen.FolderBrowser.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(folder.name)
                    }
                }
            }

            if (trackList.isNotEmpty()) {
                item {
                    Text(
                        "Tracks",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(trackList) { track ->
                    Button(
                        onClick = { onTrackSelected(track, trackList) },
                        modifier = Modifier.fillMaxWidth()
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
    }
}
