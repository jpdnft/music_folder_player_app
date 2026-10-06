package com.jpd3.musicfolderplayer.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Composable
fun Artwork(uri: Uri?, size: Dp = 192.dp, initial: String? = null) {
    val resolver = LocalContext.current.contentResolver
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            if (uri == null) return@withContext null
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                val limit = if (size.value <= 64) 256 else 1024
                while (bounds.outWidth / options.inSampleSize > limit || bounds.outHeight / options.inSampleSize > limit)
                    options.inSampleSize *= 2
                ensureActive()
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
            } catch (_: java.io.IOException) { null }
              catch (_: SecurityException) { null }
        }
    }
    if (bitmap != null) Image(bitmap = bitmap!!, contentDescription = "Folder artwork",
        modifier = Modifier.size(size), contentScale = ContentScale.Crop)
    else if (initial != null) Surface(modifier = Modifier.size(size), shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Box(contentAlignment = Alignment.Center) { Text(initial.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium) }
    }
}
