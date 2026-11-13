package org.evsyukov.teleport.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.ActionStore
import org.evsyukov.teleport.data.AppCatalog
import org.evsyukov.teleport.data.AppEntry
import org.evsyukov.teleport.data.TeleportConfig

class SettingsViewModel(
    private val store: ActionStore,
    private val catalog: AppCatalog,
) : ViewModel() {

    val config: StateFlow<TeleportConfig> = store.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeleportConfig())

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps

    fun loadApps() {
        if (_apps.value.isNotEmpty() || _loadingApps.value) return
        viewModelScope.launch {
            _loadingApps.value = true
            _apps.value = catalog.launchableApps()
            _loadingApps.value = false
        }
    }

    fun addApp(packageName: String, label: String) = viewModelScope.launch {
        store.addAction(Action.appLaunch(packageName, label))
    }

    fun addLink(uri: String, label: String) = viewModelScope.launch {
        store.addAction(Action.openLink(uri, label))
    }

    fun remove(id: String) = viewModelScope.launch { store.removeAction(id) }

    fun move(id: String, toIndex: Int) = viewModelScope.launch { store.moveAction(id, toIndex) }

    fun setPrimary(id: String?) = viewModelScope.launch { store.setPrimary(id) }

    companion object {
        fun factory(store: ActionStore, catalog: AppCatalog) = viewModelFactory {
            initializer { SettingsViewModel(store, catalog) }
        }
    }
}
