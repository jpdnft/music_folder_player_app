package com.jpd3.musicfolderplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import com.jpd3.musicfolderplayer.ui.theme.PlayerButton as Button
import com.jpd3.musicfolderplayer.ui.theme.PlayerOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import android.net.Uri
import com.jpd3.musicfolderplayer.ui.Artwork
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NowPlayingScreen(
    artworkUri: Uri?,
    mediaId: String?,
    positionMs: Long,
    durationMs: Long,
    seekable: Boolean,
    playbackError: String?,
    onSeek: (Long) -> Unit,
    trackName: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var dragging by remember(mediaId) { androidx.compose.runtime.mutableStateOf(false) }
    var progress by remember(mediaId) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (artworkUri != null) Artwork(artworkUri)
        playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
        Text(
            text = trackName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(20.dp)
        )
        }

        Slider(
            value = if (dragging) progress else if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            onValueChange = { dragging = true; progress = it },
            onValueChangeFinished = { onSeek((progress * durationMs).toLong()); dragging = false },
            enabled = seekable && durationMs > 0, modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(if (dragging) (progress * durationMs).toLong() else positionMs))
            Text(formatTime(durationMs))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            OutlinedButton(onClick = { onSeek((positionMs - 10_000).coerceAtLeast(0)) }) {
                Icon(Icons.Default.FastRewind, contentDescription = "Back 10 seconds")
            }
            Button(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            OutlinedButton(onClick = { onSeek((positionMs + 10_000).coerceAtMost(durationMs)) }) {
                Icon(Icons.Default.FastForward, contentDescription = "Forward 10 seconds")
            }
            OutlinedButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }
    }
}

private fun formatTime(ms: Long): String = java.util.Locale.ROOT.let { locale ->
    String.format(locale, "%02d:%02d", ms / 60_000, (ms / 1000) % 60)
}
