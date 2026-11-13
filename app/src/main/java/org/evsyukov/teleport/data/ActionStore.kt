package org.evsyukov.teleport.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

class ActionStore(private val dataStore: DataStore<Preferences>) {

    val config: Flow<TeleportConfig> = dataStore.data.map { decode(it) }

    suspend fun addAction(action: Action) = mutate { it.copy(actions = it.actions + action) }

    suspend fun removeAction(id: String) = mutate { cfg ->
        cfg.copy(
            primaryActionId = cfg.primaryActionId.takeIf { it != id },
            actions = cfg.actions.filterNot { it.id == id },
        )
    }

    suspend fun moveAction(id: String, toIndex: Int) = mutate { cfg ->
        val from = cfg.actions.indexOfFirst { it.id == id }
        if (from < 0 || toIndex !in cfg.actions.indices) return@mutate cfg
        val reordered = cfg.actions.toMutableList()
        reordered.add(toIndex, reordered.removeAt(from))
        cfg.copy(actions = reordered)
    }

    suspend fun setPrimary(id: String?) = mutate { cfg ->
        if (id != null && cfg.actions.none { it.id == id }) return@mutate cfg
        cfg.copy(primaryActionId = id)
    }

    private suspend fun mutate(transform: (TeleportConfig) -> TeleportConfig) {
        dataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(TeleportConfig.serializer(), transform(decode(prefs)))
        }
    }

    private fun decode(prefs: Preferences): TeleportConfig {
        val raw = prefs[KEY] ?: return TeleportConfig()
        return try {
            json.decodeFromString(TeleportConfig.serializer(), raw)
        } catch (e: SerializationException) {
            TeleportConfig()
        }
    }

    companion object {
        val KEY = stringPreferencesKey("config_json")

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun create(scope: CoroutineScope, file: File): ActionStore = ActionStore(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = { file },
            ),
        )
    }
}
