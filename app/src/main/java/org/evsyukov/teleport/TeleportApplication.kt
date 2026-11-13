package org.evsyukov.teleport

import android.app.Application
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.evsyukov.teleport.data.ActionStore
import org.evsyukov.teleport.shortcut.ShortcutSync

class TeleportApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val shortcuts by lazy { ShortcutSync(this) }

    val store: ActionStore by lazy {
        ActionStore.create(scope, preferencesDataStoreFile("config"))
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch { store.config.collect { shortcuts.sync(it) } }
    }

    /**
     * Republish the shortcuts from the stored configuration, e.g. after a
     * target disappeared and its direct intent no longer resolves.
     */
    fun resyncShortcuts() {
        scope.launch { shortcuts.sync(store.config.first()) }
    }
}
