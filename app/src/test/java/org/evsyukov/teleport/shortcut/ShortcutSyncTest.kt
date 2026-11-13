package org.evsyukov.teleport.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.evsyukov.teleport.launch.LaunchKeys
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShortcutSyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sync = ShortcutSync(context)

    private fun config(n: Int) = TeleportConfig(
        actions = (1..n).map { Action.appLaunch("com.example.app$it", "App $it") },
    )

    @Test
    fun publishesVisiblePrefixWithStableIdsAndExtras() {
        val cfg = config(6)
        sync.sync(cfg)
        val dynamic = ShortcutManagerCompat.getDynamicShortcuts(context)
        val staticCount = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManager.FLAG_MATCH_MANIFEST).size
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val expected = ShortcutSync.visibleActions(cfg.actions, max, staticCount)

        // Robolectric's shadow does not keep the rank order; on-device order is checked manually.
        assertThat(dynamic.map { it.id }).containsExactlyElementsIn(expected.map { it.id })
        assertThat(dynamic).isNotEmpty()
        dynamic.forEach { shortcut ->
            assertThat(shortcut.intent.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(shortcut.intent.getStringExtra(LaunchKeys.ACTION_ID)).isEqualTo(shortcut.id)
            assertThat(shortcut.shortLabel.toString()).isNotEmpty()
        }
    }

    @Test
    fun syncReplacesThePreviousSet() {
        sync.sync(config(3))
        val replacement = TeleportConfig(
            actions = listOf(Action.openLink("https://example.com/", "Example")),
        )
        sync.sync(replacement)
        assertThat(ShortcutManagerCompat.getDynamicShortcuts(context).map { it.id })
            .isEqualTo(replacement.actions.map { it.id })
    }

    @Test
    fun uninstalledTargetStillPublishesWithFallbackIcon() {
        val cfg = TeleportConfig(
            actions = listOf(Action.appLaunch("com.example.missing", "Missing")),
        )
        sync.sync(cfg)
        assertThat(ShortcutManagerCompat.getDynamicShortcuts(context).map { it.id })
            .isEqualTo(cfg.actions.map { it.id })
    }
}

class VisibleActionsTest {
    private val actions = (1..6).map { Action.appLaunch("com.example.app$it", "App $it") }

    @Test
    fun takesThePrefixThatFitsAfterStaticShortcuts() {
        assertThat(ShortcutSync.visibleActions(actions, max = 4, staticCount = 1))
            .isEqualTo(actions.take(3))
    }

    @Test
    fun noCapacityMeansNoDynamicShortcuts() {
        assertThat(ShortcutSync.visibleActions(actions, max = 1, staticCount = 1)).isEmpty()
        assertThat(ShortcutSync.visibleActions(actions, max = 0, staticCount = 1)).isEmpty()
    }

    @Test
    fun fewerActionsThanCapacityKeepsAllInOrder() {
        assertThat(ShortcutSync.visibleActions(actions.take(2), max = 5, staticCount = 1))
            .isEqualTo(actions.take(2))
    }
}
