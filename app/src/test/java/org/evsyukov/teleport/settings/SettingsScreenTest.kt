package org.evsyukov.teleport.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.evsyukov.teleport.R
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.ActionStore
import org.evsyukov.teleport.data.AppCatalog
import org.evsyukov.teleport.launch.SettingsReason
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private lateinit var store: ActionStore
    private lateinit var vm: SettingsViewModel

    @Before
    fun setUp() {
        store = ActionStore.create(scope, File(tmp.root, "config.preferences_pb"))
        vm = SettingsViewModel(store, AppCatalog(context))
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun showScreen(reason: SettingsReason? = null) {
        compose.setContent {
            MaterialTheme {
                SettingsScreen(reason = reason, viewModel = vm)
            }
        }
    }

    @Test
    fun showsTheReasonBanner() {
        showScreen(SettingsReason.NO_PRIMARY)
        compose.onNodeWithText(context.getString(R.string.reason_no_primary)).assertExists()
    }

    @Test
    fun listsConfiguredActions() {
        runBlocking { store.addAction(Action.appLaunch("com.example.a", "App A")) }
        showScreen()
        compose.waitUntil(5_000) { vm.config.value.actions.isNotEmpty() }
        compose.onNodeWithText("App A").assertExists()
        compose.onNodeWithText("com.example.a").assertExists()
    }

    @Test
    fun linkDialogRejectsSchemelessInput() {
        showScreen()
        compose.onNodeWithText(context.getString(R.string.add_link)).performClick()
        compose.onNodeWithText(context.getString(R.string.link_uri)).performTextInput("example.com")
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.onNodeWithText(context.getString(R.string.link_invalid)).assertExists()
        assertThat(vm.config.value.actions).isEmpty()
    }

    @Test
    fun linkDialogAcceptsUrlAndDefaultsLabelToHost() {
        showScreen()
        compose.onNodeWithText(context.getString(R.string.add_link)).performClick()
        compose.onNodeWithText(context.getString(R.string.link_uri))
            .performTextInput("https://example.com/path")
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.waitUntil(5_000) { vm.config.value.actions.isNotEmpty() }
        val action = vm.config.value.actions.single() as Action.OpenLink
        assertThat(action.uri).isEqualTo("https://example.com/path")
        assertThat(action.label).isEqualTo("example.com")
    }
}
