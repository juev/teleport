package org.evsyukov.teleport.launch

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchDecisionTest {
    private val a = Action.appLaunch("com.example.a", "A")
    private val b = Action.openLink("https://example.com/", "Example")
    private val config = TeleportConfig(primaryActionId = a.id, actions = listOf(a, b))

    @Test
    fun iconTapRunsPrimary() {
        assertThat(LaunchDecision.resolve(Intent(), config)).isEqualTo(LaunchDecision.Run(a))
    }

    @Test
    fun iconTapWithoutPrimaryOpensSettings() {
        assertThat(LaunchDecision.resolve(Intent(), TeleportConfig(actions = listOf(a))))
            .isEqualTo(LaunchDecision.Settings(SettingsReason.NO_PRIMARY))
    }

    @Test
    fun iconTapWithDanglingPrimaryOpensSettings() {
        val cfg = TeleportConfig(primaryActionId = "gone", actions = listOf(a))
        assertThat(LaunchDecision.resolve(Intent(), cfg))
            .isEqualTo(LaunchDecision.Settings(SettingsReason.NO_PRIMARY))
    }

    @Test
    fun shortcutRunsItsAction() {
        val intent = Intent().putExtra(LaunchKeys.ACTION_ID, b.id)
        assertThat(LaunchDecision.resolve(intent, config)).isEqualTo(LaunchDecision.Run(b))
    }

    @Test
    fun shortcutWithUnknownActionOpensSettingsWithUnavailable() {
        val intent = Intent().putExtra(LaunchKeys.ACTION_ID, "gone")
        assertThat(LaunchDecision.resolve(intent, config))
            .isEqualTo(LaunchDecision.Settings(SettingsReason.UNAVAILABLE))
    }

    @Test
    fun settingsShortcutOpensSettings() {
        val intent = Intent().putExtra(LaunchKeys.OPEN_SETTINGS, true)
        assertThat(LaunchDecision.resolve(intent, config))
            .isEqualTo(LaunchDecision.Settings(SettingsReason.REQUESTED))
    }

    @Test
    fun settingsShortcutWinsOverActionId() {
        val intent = Intent()
            .putExtra(LaunchKeys.OPEN_SETTINGS, true)
            .putExtra(LaunchKeys.ACTION_ID, b.id)
        assertThat(LaunchDecision.resolve(intent, config))
            .isEqualTo(LaunchDecision.Settings(SettingsReason.REQUESTED))
    }
}
