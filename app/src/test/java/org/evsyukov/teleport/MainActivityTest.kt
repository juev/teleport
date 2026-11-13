package org.evsyukov.teleport

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.TeleportConfig
import org.evsyukov.teleport.launch.LaunchKeys
import org.evsyukov.teleport.launch.SettingsReason
import org.evsyukov.teleport.settings.SettingsActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    private val app: TeleportApplication = ApplicationProvider.getApplicationContext()

    private fun seed(config: TeleportConfig) = runBlocking {
        config.actions.forEach { app.store.addAction(it) }
        app.store.setPrimary(config.primaryActionId)
    }

    private fun installLaunchable(packageName: String) {
        val query = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        val resolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.Main"
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(query, resolve)
    }

    @Test
    fun configuredPrimaryIsStartedAndTheTrampolineFinishes() {
        val action = Action.appLaunch("com.example.a", "A")
        seed(TeleportConfig(primaryActionId = action.id, actions = listOf(action)))
        installLaunchable("com.example.a")

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        assertThat(activity.isFinishing).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.packageName).isEqualTo("com.example.a")
    }

    @Test
    fun missingPrimaryOpensTheSettingsAndFinishes() {
        seed(TeleportConfig(actions = listOf(Action.appLaunch("com.example.a", "A"))))

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        assertThat(activity.isFinishing).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.className).isEqualTo(SettingsActivity::class.java.name)
        assertThat(started.getStringExtra(LaunchKeys.SETTINGS_REASON))
            .isEqualTo(SettingsReason.NO_PRIMARY.name)
    }

    @Test
    fun settingsShortcutOpensTheSettingsAndFinishes() {
        seed(TeleportConfig(actions = listOf(Action.appLaunch("com.example.a", "A"))))
        val intent = Intent(LaunchKeys.ACTION_OPEN_SETTINGS)

        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().get()

        assertThat(activity.isFinishing).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.className).isEqualTo(SettingsActivity::class.java.name)
        assertThat(started.getStringExtra(LaunchKeys.SETTINGS_REASON))
            .isEqualTo(SettingsReason.REQUESTED.name)
    }

    @Test
    fun unavailableTargetOpensTheSettingsWithTheReason() {
        val action = Action.appLaunch("com.example.missing", "Missing")
        seed(TeleportConfig(primaryActionId = action.id, actions = listOf(action)))

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        assertThat(activity.isFinishing).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.className).isEqualTo(SettingsActivity::class.java.name)
        assertThat(started.getStringExtra(LaunchKeys.SETTINGS_REASON))
            .isEqualTo(SettingsReason.UNAVAILABLE.name)
    }
}
