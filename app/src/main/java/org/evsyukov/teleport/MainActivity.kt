package org.evsyukov.teleport

import android.app.Activity
import android.os.Bundle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.evsyukov.teleport.launch.ActionLauncher
import org.evsyukov.teleport.launch.LaunchDecision
import org.evsyukov.teleport.launch.SettingsReason
import org.evsyukov.teleport.settings.SettingsActivity

/**
 * The launch trampoline behind the launcher icon and the shortcut items whose
 * target cannot be launched directly (spec R1). It never draws: the theme is
 * Theme.NoDisplay, the activity starts what the user asked for and finishes
 * before the first frame.
 */
class MainActivity : Activity() {
    private val store by lazy { (application as TeleportApplication).store }
    private val launcher by lazy { ActionLauncher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Theme.NoDisplay kills the process when the activity reaches onResume
        // without having finished, so every path has to finish here.
        try {
            handleIntent()
        } finally {
            finish()
        }
    }

    private fun handleIntent() {
        // Resolve synchronously: an asynchronous read would let the trampoline
        // live long enough for the launcher's open transition to paint its task
        // as an opaque card (spec R1). The config is a small local file that
        // TeleportApplication keeps warm.
        val config = runBlocking { store.config.first() }
        when (val decision = LaunchDecision.resolve(intent, config)) {
            is LaunchDecision.Run ->
                if (!launcher.launch(decision.action)) {
                    openSettings(SettingsReason.UNAVAILABLE)
                }
            is LaunchDecision.Settings -> openSettings(decision.reason)
        }
    }

    private fun openSettings(reason: SettingsReason) {
        startActivity(SettingsActivity.intent(this, reason))
    }
}
