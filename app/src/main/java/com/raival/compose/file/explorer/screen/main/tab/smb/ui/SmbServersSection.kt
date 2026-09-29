package com.raival.compose.file.explorer.screen.main.tab.smb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.screen.main.MainActivityManager
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.smb.holder.SmbFileHolder
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SmbServersSection(mainActivityManager: MainActivityManager) {
    val scope = rememberCoroutineScope()
    val servers = remember { mutableStateListOf<SmbServer>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<SmbServer?>(null) }
    var pendingDelete by remember { mutableStateOf<SmbServer?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun reload() {
        servers.clear()
        servers.addAll(globalClass.smbManager.getSavedServers())
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { reload() }
    }

    if (showAddDialog || showEditor) {
        AddEditSmbServerDialog(
            existing = editingServer,
            onDismiss = {
                showAddDialog = false
                showEditor = false
                editingServer = null
            },
            onSaved = {
                showAddDialog = false
                showEditor = false
                editingServer = null
                reload()
            }
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.smb_servers)) },
            text = { Text(stringResource(R.string.smb_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            globalClass.smbManager.credentialsStore.clear(target.id)
                            globalClass.smbManager.saveServers(
                                servers.filter { it.id != target.id }
                            )
                            globalClass.smbManager.disconnect(target.id)
                            withContext(Dispatchers.Main) {
                                pendingDelete = null
                                reload()
                            }
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp),
        text = stringResource(R.string.smb_servers),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (servers.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true }
                    .padding(16.dp),
                text = stringResource(R.string.no_smb_servers),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            servers.forEachIndexed { index, server ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val hasCred = globalClass.smbManager
                                        .hasUsableCredential(server)
                                    withContext(Dispatchers.Main) {
                                        if (!hasCred) {
                                            editingServer = server
                                            showEditor = true
                                            globalClass.showMsg(R.string.smb_missing_credentials)
                                        } else {
                                            mainActivityManager.replaceCurrentTabWith(
                                                FilesTab(
                                                    SmbFileHolder(server)
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                editingServer = server
                                showEditor = true
                            }
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = server.displayLabel)
                        Text(
                            text = "${server.username ?: "guest"}@${server.host}/${server.share}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            editingServer = server
                            showEditor = true
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                    }
                    IconButton(onClick = { pendingDelete = server }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = null)
                    }
                }
                if (index != servers.lastIndex) HorizontalDivider(thickness = 0.5.dp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAddDialog = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.padding(8.dp)
            )
            Text(text = stringResource(R.string.add_smb_server))
        }
    }
}