package org.evsyukov.teleport

import android.app.Application
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.evsyukov.teleport.data.ActionStore

class TeleportApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val store: ActionStore by lazy {
        ActionStore.create(scope, preferencesDataStoreFile("config"))
    }
}
