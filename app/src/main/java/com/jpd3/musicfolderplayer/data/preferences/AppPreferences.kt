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
    // Read-only legacy fields allow migration to the service-owned snapshot.
    val queueFolderUri: String? = null,
    val currentTrackUri: String? = null,
    val currentTrackIndex: Int = 0,
    val playbackPositionMs: Long = 0L,
    val hasResumableQueue: Boolean = false,
    val isPlaybackActive: Boolean = false,
    val playbackSnapshot: String? = null
)

interface AppPreferencesStore {
    val preferences: Flow<AppPreferences>
    suspend fun updateLastFolderUri(uri: String?)
    suspend fun clear() 
    suspend fun saveLibrary(uri: String)
}

class DataStoreAppPreferencesStore(
    private val context: Context
) : AppPreferencesStore {
    suspend fun saveSnapshot(snapshot: String) {
        context.dataStore.edit { it[Keys.PLAYBACK_SNAPSHOT] = snapshot }
    }
    override suspend fun saveLibrary(uri: String) {
        context.dataStore.edit {
            it[Keys.ROOT_TREE_URI] = uri
            it[Keys.LAST_FOLDER_URI] = uri
        }
    }

    private object Keys {
        val PLAYBACK_SNAPSHOT = stringPreferencesKey("playback_snapshot")
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
            isPlaybackActive = prefs[Keys.IS_PLAYBACK_ACTIVE]?.toBooleanStrictOrNull() ?: false,
            playbackSnapshot = prefs[Keys.PLAYBACK_SNAPSHOT]
        )
    }

    override suspend fun updateLastFolderUri(uri: String?) {
        context.dataStore.edit { if (uri == null) it.remove(Keys.LAST_FOLDER_URI) else it[Keys.LAST_FOLDER_URI] = uri }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
