package org.evsyukov.teleport.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.ActionStore
import org.evsyukov.teleport.data.AppCatalog
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private lateinit var store: ActionStore
    private lateinit var vm: SettingsViewModel
    private lateinit var collectScope: CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = ActionStore.create(storeScope, File(tmp.root, "config.preferences_pb"))
        vm = SettingsViewModel(store, AppCatalog(context))
        collectScope = CoroutineScope(UnconfinedTestDispatcher())
        collectScope.launch { vm.config.collect {} }
    }

    @After
    fun tearDown() {
        collectScope.cancel()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun addAppShowsInConfig() {
        vm.addApp("com.example.a", "A")
        val action = vm.config.value.actions.single()
        assertThat(action).isInstanceOf(Action.AppLaunch::class.java)
        assertThat((action as Action.AppLaunch).packageName).isEqualTo("com.example.a")
        assertThat(action.label).isEqualTo("A")
    }

    @Test
    fun addLinkShowsInConfig() {
        vm.addLink("https://example.com/", "Example")
        val action = vm.config.value.actions.single()
        assertThat(action).isInstanceOf(Action.OpenLink::class.java)
        assertThat((action as Action.OpenLink).uri).isEqualTo("https://example.com/")
        assertThat(action.label).isEqualTo("Example")
    }

    @Test
    fun removeDeletesTheAction() {
        vm.addApp("com.example.a", "A")
        val id = vm.config.value.actions.single().id
        vm.remove(id)
        assertThat(vm.config.value.actions).isEmpty()
    }

    @Test
    fun setPrimaryMarksTheAction() {
        vm.addApp("com.example.a", "A")
        val id = vm.config.value.actions.single().id
        vm.setPrimary(id)
        assertThat(vm.config.value.primaryActionId).isEqualTo(id)
    }

    @Test
    fun moveReordersActions() {
        vm.addApp("com.example.a", "A")
        vm.addApp("com.example.b", "B")
        val second = vm.config.value.actions[1]
        vm.move(second.id, 0)
        assertThat(vm.config.value.actions.map { it.label }).containsExactly("B", "A").inOrder()
    }

    @Test
    fun loadAppsListsLaunchableAppsSortedExceptSelf() = runBlocking {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            query,
            listOf(resolve("com.example.beta", "Beta"), resolve("com.example.alpha", "Alpha"),
                resolve(context.packageName, "Teleport")),
        )
        vm.apps.test {
            assertThat(awaitItem()).isEmpty()
            vm.loadApps()
            val loaded = awaitItem()
            assertThat(loaded.map { it.label }).containsExactly("Alpha", "Beta").inOrder()
            assertThat(loaded.map { it.packageName }).doesNotContain(context.packageName)
        }
    }

    private fun resolve(packageName: String, label: String) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
        }
        this.labelRes = 0
        nonLocalizedLabel = label
    }
}
