package com.raival.compose.file.explorer.screen.main.tab.sftp.ui

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
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun AddEditSftpServerDialog(
    existing: SftpServer? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var host by remember { mutableStateOf(existing?.host ?: "") }
    var port by remember { mutableStateOf((existing?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf(existing?.remotePath ?: "/") }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (existing == null) R.string.add_sftp_server else R.string.edit_sftp_server
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
                    label = { Text(stringResource(R.string.sftp_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(stringResource(R.string.sftp_host)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.sftp_port)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = remotePath,
                        onValueChange = { remotePath = it.ifBlank { "/" } },
                        label = { Text(stringResource(R.string.sftp_remote_path)) },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    label = { Text(stringResource(R.string.sftp_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            stringResource(R.string.sftp_password) +
                                if (existing != null) " *" else ""
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (existing != null) {
                        { Text("* leave blank to keep saved password") }
                    } else null
                )
                error?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
                testResult?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = {
                        val portInt = port.toIntOrNull() ?: 22
                        if (host.isBlank() || username.isBlank()) {
                            error = globalClass.getString(R.string.sftp_invalid_input)
                            return@OutlinedButton
                        }
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val probe = SftpServer(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                host = host,
                                port = portInt,
                                username = username,
                                remotePath = remotePath.ifBlank { "/" }
                            )
                            // Use typed password if provided, else stored one for edits
                            val pw = password.ifBlank {
                                globalClass.sftpManager.credentialsStore.getPassword(probe.id)
                                    ?: ""
                            }
                            val result = globalClass.sftpManager.testConnection(probe, pw)
                            withContext(Dispatchers.Main) {
                                testing = false
                                testResult = if (result.isSuccess) {
                                    globalClass.getString(R.string.sftp_connection_ok)
                                } else {
                                    globalClass.getString(
                                        R.string.sftp_connection_failed,
                                        result.exceptionOrNull()?.message ?: "unknown"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.sftp_testing))
                    } else {
                        Text(stringResource(R.string.sftp_test_connection))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portInt = port.toIntOrNull() ?: 22
                    if (host.isBlank() || username.isBlank() ||
                        (existing == null && password.isBlank())
                    ) {
                        error = globalClass.getString(R.string.sftp_invalid_input)
                        return@Button
                    }
                    scope.launch(Dispatchers.IO) {
                        val servers =
                            globalClass.sftpManager.getSavedServers().toMutableList()
                        val id = existing?.id ?: UUID.randomUUID().toString()
                        val server = SftpServer(
                            id = id,
                            name = name,
                            host = host,
                            port = portInt,
                            username = username,
                            remotePath = remotePath.ifBlank { "/" }
                        )
                        val idx = servers.indexOfFirst { it.id == id }
                        if (idx >= 0) servers[idx] = server else servers.add(server)
                        globalClass.sftpManager.saveServers(servers)
                        if (password.isNotBlank()) {
                            globalClass.sftpManager.credentialsStore.savePassword(id, password)
                        }
                        withContext(Dispatchers.Main) { onSaved() }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
