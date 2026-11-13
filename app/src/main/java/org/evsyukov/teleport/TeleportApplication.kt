package org.evsyukov.teleport

import android.app.Application
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.evsyukov.teleport.data.ActionStore
import org.evsyukov.teleport.shortcut.ShortcutSync

class TeleportApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val store: ActionStore by lazy {
        ActionStore.create(scope, preferencesDataStoreFile("config"))
    }

    override fun onCreate() {
        super.onCreate()
        val sync = ShortcutSync(this)
        scope.launch { store.config.collect { sync.sync(it) } }
    }
}
