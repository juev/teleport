package org.evsyukov.teleport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.evsyukov.teleport.launch.ActionLauncher
import org.evsyukov.teleport.launch.LaunchDecision
import org.evsyukov.teleport.launch.SettingsReason

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = (application as TeleportApplication).store
        val launcher = ActionLauncher(this)
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

    // Placeholder until the settings screen lands; the reason message is the contract (R2, R3).
    private fun showSettings(reason: SettingsReason) {
        val message = when (reason) {
            SettingsReason.REQUESTED -> R.string.settings_hint
            SettingsReason.NO_PRIMARY -> R.string.reason_no_primary
            SettingsReason.UNAVAILABLE -> R.string.reason_unavailable
        }
        setContent {
            MaterialTheme {
                Surface {
                    Text(stringResource(message))
                }
            }
        }
    }
}
