package com.jpd3.musicfolderplayer.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jpd3.musicfolderplayer.data.preferences.AppPreferences
import com.jpd3.musicfolderplayer.data.preferences.DataStoreAppPreferencesStore
import com.jpd3.musicfolderplayer.util.FolderPathUtils
import com.jpd3.musicfolderplayer.playback.MusicPlayerController
import com.jpd3.musicfolderplayer.ui.screens.FolderBrowserScreen
import com.jpd3.musicfolderplayer.ui.screens.NowPlayingScreen
import com.jpd3.musicfolderplayer.ui.screens.RecoveryScreen
import com.jpd3.musicfolderplayer.ui.screens.Screen
import com.jpd3.musicfolderplayer.ui.screens.SettingsScreen
import com.jpd3.musicfolderplayer.ui.screens.SetupScreen
import kotlinx.coroutines.launch

@Composable
fun MusicFolderPlayerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val preferencesStore = remember { DataStoreAppPreferencesStore(context) }
    val preferences by preferencesStore.preferences.collectAsState(initial = AppPreferences())
    val rootUri = preferences.rootTreeUri?.let(Uri::parse)
    val currentFolderUri = preferences.lastFolderUri?.let(Uri::parse)
    val scope = rememberCoroutineScope()
    val controller = remember { MusicPlayerController(context) }
    val playback by controller.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    val documentTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri == null) return@rememberLauncherForActivityResult

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (_: SecurityException) {
            // Some devices do not allow persisted access; the app still continues with the chosen folder.
        }

        scope.launch {
            preferencesStore.saveLibrary(treeUri.toString())
        }

        navController.navigate(Screen.FolderBrowser.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
        }
    }

    LaunchedEffect(rootUri) {
        if (rootUri != null && navController.currentDestination?.route == Screen.Setup.route) {
            navController.navigate(Screen.FolderBrowser.route) {
                popUpTo(Screen.Setup.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomPlayerBar(
                title = playback.title,
                subtitle = playback.album.ifBlank { playback.artist.ifBlank { "Local music" } },
                playing = playback.playing,
                onPlayPause = { controller.playPause() },
                onNext = { controller.nextTrack() },
                onPrevious = { controller.previousTrack() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (rootUri != null) Screen.FolderBrowser.route else Screen.Setup.route
            ) {
                composable(Screen.Setup.route) {
                    SetupScreen(
                        navController = navController,
                        onChooseFolder = { documentTreeLauncher.launch(null) }
                    )
                }
                composable(Screen.FolderBrowser.route) {
                    FolderBrowserScreen(
                        navController = navController,
                        rootUri = rootUri,
                        currentFolderUri = currentFolderUri,
                        onChooseFolder = { navController.navigate(Screen.Settings.route) },
                        onFolderSelected = { folderUri ->
                            scope.launch {
                                preferencesStore.updateLastFolderUri(folderUri.toString())
                            }
                        },
                        playbackReady = playback.connected,
                        onTrackSelected = { track, tracks, folder ->
                            controller.playFolderTrack(folder, tracks, track)
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        artworkUri = playback.artwork,
                        mediaId = playback.mediaId,
                        positionMs = playback.position,
                        durationMs = playback.duration,
                        seekable = playback.seekable,
                        onSeek = controller::seekTo,
                        playbackError = playback.error,
                        trackName = playback.title,
                        isPlaying = playback.playing,
                        onPlayPause = { controller.playPause() },
                        onPrevious = { controller.previousTrack() },
                        onNext = { controller.nextTrack() }
                    )
                }
                composable(Screen.Recovery.route) {
                    RecoveryScreen(navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        navController = navController,
                        libraryFolder = FolderPathUtils.buildBreadcrumb(rootUri, rootUri).last(),
                        onChooseLibrary = { documentTreeLauncher.launch(rootUri) },
                        onClearState = {
                            controller.clear()
                            scope.launch {
                                preferencesStore.clear()
                            }
                            navController.navigate(Screen.Setup.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
