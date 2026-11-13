package org.evsyukov.teleport.settings

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.evsyukov.teleport.R
import org.evsyukov.teleport.data.Action
import org.evsyukov.teleport.data.AppEntry
import org.evsyukov.teleport.launch.SettingsReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(reason: SettingsReason?, viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (reason == SettingsReason.NO_PRIMARY || reason == SettingsReason.UNAVAILABLE) {
                val text = if (reason == SettingsReason.NO_PRIMARY) {
                    R.string.reason_no_primary
                } else {
                    R.string.reason_unavailable
                }
                Text(
                    stringResource(text),
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            val primary = config.actions.firstOrNull { it.id == config.primaryActionId }
            Text(
                stringResource(R.string.primary_hint),
                Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                primary?.label ?: stringResource(R.string.primary_none),
                Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = {
                    viewModel.loadApps()
                    showAppPicker = true
                }) { Text(stringResource(R.string.add_app)) }
                Button(onClick = { showLinkDialog = true }) {
                    Text(stringResource(R.string.add_link))
                }
            }

            if (config.actions.isEmpty()) {
                Text(stringResource(R.string.no_actions), Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(config.actions, key = { _, action -> action.id }) { index, action ->
                    ActionRow(
                        action = action,
                        isPrimary = action.id == config.primaryActionId,
                        canMoveUp = index > 0,
                        canMoveDown = index < config.actions.lastIndex,
                        onPrimary = { viewModel.setPrimary(action.id) },
                        onUp = { viewModel.move(action.id, index - 1) },
                        onDown = { viewModel.move(action.id, index + 1) },
                        onRemove = { viewModel.remove(action.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            viewModel = viewModel,
            onDismiss = { showAppPicker = false },
            onPick = { entry ->
                viewModel.addApp(entry.packageName, entry.label)
                showAppPicker = false
            },
        )
    }
    if (showLinkDialog) {
        LinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { uri, label ->
                viewModel.addLink(uri, label)
                showLinkDialog = false
            },
        )
    }
}

@Composable
private fun ActionRow(
    action: Action,
    isPrimary: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPrimary: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(action, Modifier.size(40.dp))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                action.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (action) {
                    is Action.AppLaunch -> action.packageName
                    is Action.OpenLink -> action.uri
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(selected = isPrimary, onClick = onPrimary)
        IconButton(onClick = onUp, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.action_up),
            )
        }
        IconButton(onClick = onDown, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.action_down),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
        }
    }
}

@Composable
private fun ActionIcon(action: Action, modifier: Modifier = Modifier) {
    when (action) {
        is Action.OpenLink -> Image(painterResource(R.drawable.ic_shortcut_link), null, modifier)
        is Action.AppLaunch -> {
            val context = LocalContext.current
            var bitmap by remember(action.packageName) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(action.packageName) {
                bitmap = withContext(Dispatchers.IO) {
                    try {
                        context.packageManager.getApplicationIcon(action.packageName)
                            .toBitmap().asImageBitmap()
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                }
            }
            val loaded = bitmap
            if (loaded != null) {
                Image(loaded, null, modifier)
            } else {
                Image(painterResource(R.drawable.ic_shortcut_link), null, modifier)
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onPick: (AppEntry) -> Unit,
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loading by viewModel.loadingApps.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_apps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    )
                }
                val filtered = apps.filter { it.label.contains(query, ignoreCase = true) }
                LazyColumn {
                    items(filtered, key = { it.packageName }) { entry ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entry) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(entry.icon, null, Modifier.size(40.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    entry.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var uri by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_link)) },
        text = {
            Column {
                TextField(
                    value = uri,
                    onValueChange = {
                        uri = it
                        error = false
                    },
                    label = { Text(stringResource(R.string.link_uri)) },
                    isError = error,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(
                        stringResource(R.string.link_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.link_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = uri.trim()
                val parsed = Uri.parse(trimmed)
                if (trimmed.isEmpty() || parsed.scheme.isNullOrBlank()) {
                    error = true
                } else {
                    onConfirm(trimmed, label.trim().ifEmpty { parsed.host ?: trimmed })
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
