package com.raival.compose.file.explorer.screen.main.tab.smb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbAuthType
import com.raival.compose.file.explorer.screen.main.tab.smb.model.SmbServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun AddEditSmbServerDialog(
    existing: SmbServer? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dialogId = remember { existing?.id ?: UUID.randomUUID().toString() }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var host by remember { mutableStateOf(existing?.host ?: "") }
    var share by remember { mutableStateOf(existing?.share ?: "") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(existing?.authType ?: SmbAuthType.GUEST) }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    fun buildProbe(): SmbServer = SmbServer(
        id = dialogId,
        name = name,
        host = host,
        share = share,
        username = username.ifBlank { null },
        authType = authType
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existing == null) R.string.add_smb_server else R.string.edit_smb_server
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.smb_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(stringResource(R.string.smb_host)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = share,
                    onValueChange = { share = it.trim() },
                    label = { Text(stringResource(R.string.smb_share)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    label = { Text(stringResource(R.string.smb_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.smb_auth_method),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = authType == SmbAuthType.GUEST,
                        onClick = { authType = SmbAuthType.GUEST },
                        label = { Text(stringResource(R.string.smb_auth_guest)) }
                    )
                    FilterChip(
                        selected = authType == SmbAuthType.USER,
                        onClick = { authType = SmbAuthType.USER },
                        label = { Text(stringResource(R.string.smb_auth_user)) }
                    )
                }
                if (authType == SmbAuthType.USER) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                stringResource(R.string.smb_password) +
                                    if (existing != null && existing.authType == SmbAuthType.USER) " *" else ""
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = if (existing != null && existing.authType == SmbAuthType.USER) {
                            { Text("* leave blank to keep saved password") }
                        } else null
                    )
                }
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                testResult?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = {
                        if (host.isBlank() || share.isBlank()) {
                            error = globalClass.getString(R.string.smb_invalid_input)
                            return@OutlinedButton
                        }
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val probe = buildProbe()
                            val pw = password.ifBlank {
                                globalClass.smbManager.credentialsStore.getPassword(probe.id) ?: ""
                            }
                            val result = globalClass.smbManager.testConnection(probe, password = pw)
                            withContext(Dispatchers.Main) {
                                testing = false
                                testResult = if (result.isSuccess) {
                                    globalClass.getString(R.string.smb_connection_ok)
                                } else {
                                    globalClass.getString(
                                        R.string.smb_connection_failed,
                                        result.exceptionOrNull()?.message
                                            ?: globalClass.getString(R.string.unknown)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.smb_testing))
                    } else {
                        Text(stringResource(R.string.smb_test_connection))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (host.isBlank() || share.isBlank()) {
                        error = globalClass.getString(R.string.smb_invalid_input)
                        return@Button
                    }
                    val needsPassword = authType == SmbAuthType.USER &&
                        (existing == null || existing.authType != SmbAuthType.USER)
                    if (needsPassword && password.isBlank()) {
                        error = globalClass.getString(R.string.smb_password_required)
                        return@Button
                    }
                    scope.launch(Dispatchers.IO) {
                        val servers = globalClass.smbManager.getSavedServers().toMutableList()
                        val server = buildProbe()
                        val idx = servers.indexOfFirst { it.id == dialogId }
                        if (idx >= 0) servers[idx] = server else servers.add(server)
                        globalClass.smbManager.saveServers(servers)
                        if (authType == SmbAuthType.USER && password.isNotBlank()) {
                            globalClass.smbManager.credentialsStore.savePassword(dialogId, password)
                        }
                        globalClass.smbManager.disconnect(dialogId)
                        withContext(Dispatchers.Main) { onSaved() }
                    }
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}