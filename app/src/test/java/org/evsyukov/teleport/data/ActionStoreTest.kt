package org.evsyukov.teleport.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ActionStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String = "config.preferences_pb") = File(tmp.root, name)

    private fun store(scope: CoroutineScope, file: File = file()) =
        ActionStore.create(scope, file)

    @Test
    fun emptyStoreHasEmptyConfig() = runTest {
        store(backgroundScope).config.test {
            assertThat(awaitItem()).isEqualTo(TeleportConfig())
        }
    }

    @Test
    fun addedActionsAreKeptInInsertionOrder() = runTest {
        val store = store(backgroundScope)
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.openLink("https://example.com/", "Example")
        store.addAction(a)
        store.addAction(b)
        assertThat(store.config.first().actions).containsExactly(a, b).inOrder()
    }

    @Test
    fun actionsPersistAcrossReopen() = runTest {
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.openLink("https://example.com/", "Example")
        val scope1 = CoroutineScope(Dispatchers.IO + Job())
        val first = ActionStore.create(scope1, file())
        first.addAction(a)
        first.addAction(b)
        scope1.cancel()

        val copy = file("copy.preferences_pb")
        file().copyTo(copy)
        assertThat(store(backgroundScope, copy).config.first())
            .isEqualTo(TeleportConfig(actions = listOf(a, b)))
    }

    @Test
    fun removingThePrimaryActionClearsThePrimary() = runTest {
        val store = store(backgroundScope)
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.appLaunch("com.example.b", "B")
        store.addAction(a)
        store.addAction(b)
        store.setPrimary(a.id)
        store.removeAction(a.id)
        assertThat(store.config.first()).isEqualTo(TeleportConfig(actions = listOf(b)))
    }

    @Test
    fun moveReordersActions() = runTest {
        val store = store(backgroundScope)
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.appLaunch("com.example.b", "B")
        val c = Action.appLaunch("com.example.c", "C")
        store.addAction(a)
        store.addAction(b)
        store.addAction(c)
        store.moveAction(c.id, 0)
        assertThat(store.config.first().actions).containsExactly(c, a, b).inOrder()
        store.moveAction(c.id, 2)
        assertThat(store.config.first().actions).containsExactly(a, b, c).inOrder()
    }

    @Test
    fun moveWithUnknownIdOrOutOfRangeIndexChangesNothing() = runTest {
        val store = store(backgroundScope)
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.appLaunch("com.example.b", "B")
        store.addAction(a)
        store.addAction(b)
        store.moveAction("missing", 0)
        store.moveAction(a.id, 2)
        store.moveAction(a.id, -1)
        assertThat(store.config.first().actions).containsExactly(a, b).inOrder()
    }

    @Test
    fun setPrimarySetsClearsAndIgnoresUnknownIds() = runTest {
        val store = store(backgroundScope)
        val a = Action.appLaunch("com.example.a", "A")
        store.addAction(a)
        store.setPrimary("missing")
        assertThat(store.config.first().primaryActionId).isNull()
        store.setPrimary(a.id)
        assertThat(store.config.first().primaryActionId).isEqualTo(a.id)
        store.setPrimary(null)
        assertThat(store.config.first().primaryActionId).isNull()
    }

    @Test
    fun corruptJsonFallsBackToDefaultConfig() = runTest {
        val scope1 = CoroutineScope(Dispatchers.IO + Job())
        val raw = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file() })
        raw.edit { it[ActionStore.KEY] = "not-json" }
        scope1.cancel()
        store(backgroundScope).config.test {
            assertThat(awaitItem()).isEqualTo(TeleportConfig())
        }
    }

    @Test
    fun corruptFileFallsBackToDefaultConfig() = runTest {
        file().writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
        store(backgroundScope).config.test {
            assertThat(awaitItem()).isEqualTo(TeleportConfig())
        }
    }

    @Test
    fun factoryGeneratesDistinctNonEmptyIds() {
        val a = Action.appLaunch("com.example.a", "A")
        val b = Action.appLaunch("com.example.a", "A")
        assertThat(a.id).isNotEmpty()
        assertThat(a.id).isNotEqualTo(b.id)
    }
}
