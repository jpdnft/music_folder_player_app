package com.jpd3.musicfolderplayer.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

const val FOLDER_URI = "folder_uri"

fun musicItem(uri: Uri, title: String, folder: Uri, artwork: Uri? = null,
              artist: String = "", album: String = ""): MediaItem = MediaItem.Builder()
    .setMediaId(uri.toString()).setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
        .setAlbumTitle(album).setArtworkUri(artwork).setIsBrowsable(false).setIsPlayable(true)
        .setExtras(Bundle().apply { putString(FOLDER_URI, folder.toString()) }).build()).build()

data class PlaybackSnapshot(val items: List<MediaItem>, val index: Int, val position: Long,
                            val playWhenReady: Boolean = false) {
    fun encode(): String = JSONObject().put("index", index).put("position", position)
        .put("playWhenReady", playWhenReady)
        .put("items", JSONArray().apply {
            items.forEach { item -> put(JSONObject().put("uri", item.mediaId)
                .put("title", item.mediaMetadata.title?.toString().orEmpty())
                .put("artist", item.mediaMetadata.artist?.toString().orEmpty())
                .put("album", item.mediaMetadata.albumTitle?.toString().orEmpty())
                .put("folder", item.mediaMetadata.extras?.getString(FOLDER_URI).orEmpty())
                .put("artwork", item.mediaMetadata.artworkUri?.toString().orEmpty())) }
        }).toString()

    companion object {
        fun decode(value: String): PlaybackSnapshot {
            val json = JSONObject(value)
            val array = json.getJSONArray("items")
            val items = (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                musicItem(Uri.parse(item.getString("uri")), item.getString("title"),
                    Uri.parse(item.getString("folder")),
                    item.optString("artwork").takeIf { it.isNotBlank() }?.let(Uri::parse),
                    item.optString("artist"), item.optString("album"))
            }
            return PlaybackSnapshot(items, if (items.isEmpty()) 0 else
                json.optInt("index").coerceIn(items.indices), json.optLong("position").coerceAtLeast(0),
                json.optBoolean("playWhenReady"))
        }
    }
}
