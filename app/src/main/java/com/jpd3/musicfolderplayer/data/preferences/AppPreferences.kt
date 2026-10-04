package com.jpd3.musicfolderplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "music_folder_player_prefs"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

data class AppPreferences(
    val rootTreeUri: String? = null,
    val lastFolderUri: String? = null,
    val queueFolderUri: String? = null,
    val currentTrackUri: String? = null,
    val currentTrackIndex: Int = 0,
    val playbackPositionMs: Long = 0L,
    val hasResumableQueue: Boolean = false,
    val isPlaybackActive: Boolean = false
)

interface AppPreferencesStore {
    val preferences: Flow<AppPreferences>
    suspend fun updateRootTreeUri(uri: String?)
    suspend fun updateLastFolderUri(uri: String?)
    suspend fun updateQueueFolderUri(uri: String?)
    suspend fun updateCurrentTrack(uri: String?, index: Int)
    suspend fun updatePlaybackPosition(positionMs: Long)
    suspend fun updateQueueState(hasResumableQueue: Boolean, playbackActive: Boolean)
    suspend fun clear() 
    suspend fun saveLibrary(uri: String)
    suspend fun savePlayback(folderUri: String?, trackUri: String, index: Int)
}

class DataStoreAppPreferencesStore(
    private val context: Context
) : AppPreferencesStore {
    override suspend fun saveLibrary(uri: String) {
        context.dataStore.edit {
            it[Keys.ROOT_TREE_URI] = uri
            it[Keys.LAST_FOLDER_URI] = uri
        }
    }

    override suspend fun savePlayback(folderUri: String?, trackUri: String, index: Int) {
        context.dataStore.edit {
            if (folderUri == null) it.remove(Keys.QUEUE_FOLDER_URI) else it[Keys.QUEUE_FOLDER_URI] = folderUri
            it[Keys.CURRENT_TRACK_URI] = trackUri
            it[Keys.CURRENT_TRACK_INDEX] = index.toString()
            it[Keys.HAS_RESUMABLE_QUEUE] = "true"
            it[Keys.IS_PLAYBACK_ACTIVE] = "true"
            it[Keys.PLAYBACK_POSITION_MS] = "0"
        }
    }
    private object Keys {
        val ROOT_TREE_URI = stringPreferencesKey("root_tree_uri")
        val LAST_FOLDER_URI = stringPreferencesKey("last_folder_uri")
        val QUEUE_FOLDER_URI = stringPreferencesKey("queue_folder_uri")
        val CURRENT_TRACK_URI = stringPreferencesKey("current_track_uri")
        val CURRENT_TRACK_INDEX = stringPreferencesKey("current_track_index")
        val PLAYBACK_POSITION_MS = stringPreferencesKey("playback_position_ms")
        val HAS_RESUMABLE_QUEUE = stringPreferencesKey("has_resumable_queue")
        val IS_PLAYBACK_ACTIVE = stringPreferencesKey("is_playback_active")
    }

    override val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            rootTreeUri = prefs[Keys.ROOT_TREE_URI],
            lastFolderUri = prefs[Keys.LAST_FOLDER_URI],
            queueFolderUri = prefs[Keys.QUEUE_FOLDER_URI],
            currentTrackUri = prefs[Keys.CURRENT_TRACK_URI],
            currentTrackIndex = prefs[Keys.CURRENT_TRACK_INDEX]?.toIntOrNull() ?: 0,
            playbackPositionMs = prefs[Keys.PLAYBACK_POSITION_MS]?.toLongOrNull() ?: 0L,
            hasResumableQueue = prefs[Keys.HAS_RESUMABLE_QUEUE]?.toBooleanStrictOrNull() ?: false,
            isPlaybackActive = prefs[Keys.IS_PLAYBACK_ACTIVE]?.toBooleanStrictOrNull() ?: false
        )
    }

    override suspend fun updateRootTreeUri(uri: String?) {
        context.dataStore.edit { it[Keys.ROOT_TREE_URI] = uri ?: "" }
    }

    override suspend fun updateLastFolderUri(uri: String?) {
        context.dataStore.edit { it[Keys.LAST_FOLDER_URI] = uri ?: "" }
    }

    override suspend fun updateQueueFolderUri(uri: String?) {
        context.dataStore.edit { it[Keys.QUEUE_FOLDER_URI] = uri ?: "" }
    }

    override suspend fun updateCurrentTrack(uri: String?, index: Int) {
        context.dataStore.edit {
            if (uri == null) it.remove(Keys.CURRENT_TRACK_URI) else it[Keys.CURRENT_TRACK_URI] = uri
            it[Keys.CURRENT_TRACK_INDEX] = index.toString()
        }
    }

    override suspend fun updatePlaybackPosition(positionMs: Long) {
        context.dataStore.edit { it[Keys.PLAYBACK_POSITION_MS] = positionMs.toString() }
    }

    override suspend fun updateQueueState(hasResumableQueue: Boolean, playbackActive: Boolean) {
        context.dataStore.edit {
            it[Keys.HAS_RESUMABLE_QUEUE] = hasResumableQueue.toString()
            it[Keys.IS_PLAYBACK_ACTIVE] = playbackActive.toString()
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
