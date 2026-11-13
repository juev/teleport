package org.evsyukov.teleport.launch

import android.content.Intent
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig

enum class SettingsReason { REQUESTED, NO_PRIMARY, UNAVAILABLE }

sealed interface LaunchDecision {
    data class Run(val action: Action) : LaunchDecision
    data class Settings(val reason: SettingsReason) : LaunchDecision

    companion object {
        fun resolve(intent: Intent, config: TeleportConfig): LaunchDecision {
            if (intent.action == LaunchKeys.ACTION_OPEN_SETTINGS) {
                return Settings(SettingsReason.REQUESTED)
            }
            val id = intent.getStringExtra(LaunchKeys.ACTION_ID) ?: config.primaryActionId
            val action = config.actions.firstOrNull { it.id == id }
            if (action != null) return Run(action)
            val reason = if (intent.hasExtra(LaunchKeys.ACTION_ID)) {
                SettingsReason.UNAVAILABLE
            } else {
                SettingsReason.NO_PRIMARY
            }
            return Settings(reason)
        }
    }
}
