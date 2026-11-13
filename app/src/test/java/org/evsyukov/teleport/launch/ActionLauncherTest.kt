package org.evsyukov.teleport.launch

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.evsyukov.teleport.data.Action
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ActionLauncherTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val launcher = ActionLauncher(activity)

    @Test
    fun unknownPackageReturnsFalse() {
        assertThat(launcher.launch(Action.appLaunch("com.example.missing", "Missing"))).isFalse()
    }

    @Test
    fun installedAppIsStarted() {
        val query = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage("com.example.a")
        val resolve = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.a"
                name = "com.example.a.Main"
            }
        }
        shadowOf(activity.packageManager).addResolveInfoForIntent(query, resolve)

        assertThat(launcher.launch(Action.appLaunch("com.example.a", "A"))).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.packageName).isEqualTo("com.example.a")
    }

    @Test
    fun linkIsOpenedWithViewIntent() {
        assertThat(launcher.launch(Action.openLink("https://example.com/", "Example"))).isTrue()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(started.data).isEqualTo(Uri.parse("https://example.com/"))
    }

    @Test
    fun linkWithoutHandlerReturnsFalse() {
        // Robolectric never throws ActivityNotFoundException; simulate the platform failure.
        val throwing = object : ContextWrapper(activity) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException("no handler")
            }
        }
        assertThat(ActionLauncher(throwing).launch(Action.openLink("custom-scheme://thing", "X")))
            .isFalse()
    }
}
