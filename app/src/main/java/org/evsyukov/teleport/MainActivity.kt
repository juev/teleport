package org.evsyukov.teleport

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.evsyukov.teleport.data.AppCatalog
import org.evsyukov.teleport.launch.ActionLauncher
import org.evsyukov.teleport.launch.LaunchDecision
import org.evsyukov.teleport.launch.SettingsReason
import org.evsyukov.teleport.settings.SettingsScreen
import org.evsyukov.teleport.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val store by lazy { (application as TeleportApplication).store }
    private val launcher by lazy { ActionLauncher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
    }

    // singleTask: the launcher reuses the existing task on icon tap, so the
    // launch decision must also run when the intent arrives here (spec R1).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        lifecycleScope.launch {
            val config = store.config.first()
            when (val decision = LaunchDecision.resolve(intent, config)) {
                is LaunchDecision.Run ->
                    if (launcher.launch(decision.action)) finish()
                    else showSettings(SettingsReason.UNAVAILABLE)
                is LaunchDecision.Settings -> showSettings(decision.reason)
            }
        }
    }

    private fun showSettings(reason: SettingsReason) {
        val app = application as TeleportApplication
        setContent {
            MaterialTheme {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(app.store, AppCatalog(app)),
                )
                SettingsScreen(
                    reason = reason.takeIf { it != SettingsReason.REQUESTED },
                    viewModel = viewModel,
                )
            }
        }
    }
}
