package org.evsyukov.teleport.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.evsyukov.teleport.TeleportApplication
import org.evsyukov.teleport.data.AppCatalog
import org.evsyukov.teleport.launch.LaunchKeys
import org.evsyukov.teleport.launch.SettingsReason

/**
 * The settings screen, in a task of its own: the trampoline has to stay a
 * separate, disposable task, otherwise the launcher reuses the task with the
 * settings snapshot for the icon tap (spec R1 forbids that snapshot).
 */
class SettingsActivity : ComponentActivity() {
    private val reason = MutableStateFlow(SettingsReason.REQUESTED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reason.value = intent.reason()
        val app = application as TeleportApplication
        setContent {
            MaterialTheme {
                val current by reason.collectAsStateWithLifecycle()
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(app.store, AppCatalog(app)),
                )
                SettingsScreen(
                    reason = current.takeIf { it != SettingsReason.REQUESTED },
                    viewModel = viewModel,
                )
            }
        }
    }

    // singleTask: asking for the settings while they are open brings the same
    // instance to the front and only updates the message above the list.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reason.value = intent.reason()
    }

    companion object {
        fun intent(context: Context, reason: SettingsReason): Intent =
            Intent(context, SettingsActivity::class.java)
                // Its own task via the settings taskAffinity: a finished
                // trampoline must not take the screen with it, and the launcher
                // must not reuse the settings task as the trampoline.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(LaunchKeys.SETTINGS_REASON, reason.name)
    }
}

private fun Intent.reason(): SettingsReason =
    getStringExtra(LaunchKeys.SETTINGS_REASON)
        ?.let { name -> runCatching { SettingsReason.valueOf(name) }.getOrNull() }
        ?: SettingsReason.REQUESTED
