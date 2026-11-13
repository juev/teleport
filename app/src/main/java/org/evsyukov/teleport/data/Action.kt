package org.evsyukov.teleport.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed interface Action {
    val id: String
    val label: String

    @Serializable
    data class AppLaunch(
        override val id: String,
        override val label: String,
        val packageName: String,
    ) : Action

    @Serializable
    data class OpenLink(
        override val id: String,
        override val label: String,
        val uri: String,
    ) : Action

    companion object {
        fun appLaunch(packageName: String, label: String): Action =
            AppLaunch(UUID.randomUUID().toString(), label, packageName)

        fun openLink(uri: String, label: String): Action =
            OpenLink(UUID.randomUUID().toString(), label, uri)
    }
}

@Serializable
data class TeleportConfig(
    val primaryActionId: String? = null,
    val actions: List<Action> = emptyList(),
)
