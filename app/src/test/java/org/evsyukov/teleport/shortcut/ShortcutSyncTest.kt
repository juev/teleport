package org.evsyukov.teleport.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.evsyukov.teleport.MainActivity
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.evsyukov.teleport.launch.LaunchKeys
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShortcutSyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sync = ShortcutSync(context)

    private fun config(n: Int) = TeleportConfig(
        actions = (1..n).map { Action.appLaunch("com.example.app$it", "App $it") },
    )

    private fun shortcuts() = ShortcutManagerCompat.getDynamicShortcuts(context)

    private fun resolveInfo(packageName: String) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
        }
    }

    private fun launchQuery(packageName: String) = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setPackage(packageName)

    private fun installLaunchable(packageName: String) =
        shadowOf(context.packageManager)
            .addResolveInfoForIntent(launchQuery(packageName), resolveInfo(packageName))

    private fun uninstall(packageName: String) =
        shadowOf(context.packageManager)
            .removeResolveInfosForIntent(launchQuery(packageName), packageName)

    private fun intentOf(action: Action): Intent =
        shortcuts().single { it.id == action.id }.intent

    @Test
    fun appLaunchShortcutGoesStraightToTheTarget() {
        val action = Action.appLaunch("com.example.a", "A")
        installLaunchable("com.example.a")

        sync.sync(TeleportConfig(actions = listOf(action)))

        val intent = intentOf(action)
        assertThat(intent.component?.packageName).isEqualTo("com.example.a")
        assertThat(intent.hasExtra(LaunchKeys.ACTION_ID)).isFalse()
    }

    @Test
    fun appLaunchWithoutTargetKeepsTheTrampolineIntent() {
        val action = Action.appLaunch("com.example.missing", "Missing")

        sync.sync(TeleportConfig(actions = listOf(action)))

        val intent = intentOf(action)
        assertThat(intent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(intent.getStringExtra(LaunchKeys.ACTION_ID)).isEqualTo(action.id)
    }

    @Test
    fun linkShortcutGoesStraightToItsHandler() {
        val uri = "https://example.com/path"
        val action = Action.openLink(uri, "Example")
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        shadowOf(context.packageManager)
            .addResolveInfoForIntent(view, resolveInfo("com.example.browser"))

        sync.sync(TeleportConfig(actions = listOf(action)))

        val intent = intentOf(action)
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data).isEqualTo(Uri.parse(uri))
        assertThat(intent.hasExtra(LaunchKeys.ACTION_ID)).isFalse()
    }

    @Test
    fun linkWithoutHandlerKeepsTheTrampolineIntent() {
        val action = Action.openLink("custom-scheme://nothing", "Nothing")

        sync.sync(TeleportConfig(actions = listOf(action)))

        val intent = intentOf(action)
        assertThat(intent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(intent.getStringExtra(LaunchKeys.ACTION_ID)).isEqualTo(action.id)
    }

    @Test
    fun targetRemovalMovesTheShortcutBackToTheTrampoline() {
        val action = Action.appLaunch("com.example.a", "A")
        val config = TeleportConfig(actions = listOf(action))
        installLaunchable("com.example.a")
        sync.sync(config)
        assertThat(intentOf(action).component?.packageName).isEqualTo("com.example.a")

        uninstall("com.example.a")
        sync.sync(config)

        val intent = intentOf(action)
        assertThat(intent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(intent.getStringExtra(LaunchKeys.ACTION_ID)).isEqualTo(action.id)
    }

    @Test
    fun publishesVisiblePrefixWithStableIdsAndLabels() {
        val cfg = config(6)
        sync.sync(cfg)
        val staticCount = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_MANIFEST).size
        val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        val expected = ShortcutSync.visibleActions(cfg.actions, max, staticCount)

        // Robolectric's shadow does not keep the rank order; on-device order is checked manually.
        assertThat(shortcuts().map { it.id }).containsExactlyElementsIn(expected.map { it.id })
        assertThat(shortcuts()).isNotEmpty()
        shortcuts().forEach { shortcut ->
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
        assertThat(shortcuts().map { it.id }).isEqualTo(replacement.actions.map { it.id })
    }

    @Test
    fun uninstalledTargetStillPublishesWithFallbackIcon() {
        val cfg = TeleportConfig(
            actions = listOf(Action.appLaunch("com.example.missing", "Missing")),
        )
        sync.sync(cfg)
        assertThat(shortcuts().map { it.id }).isEqualTo(cfg.actions.map { it.id })
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
