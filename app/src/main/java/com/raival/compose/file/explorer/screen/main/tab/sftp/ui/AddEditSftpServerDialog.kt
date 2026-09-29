package com.raival.compose.file.explorer.screen.main.tab.sftp.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpAuthType
import com.raival.compose.file.explorer.screen.main.tab.sftp.model.SftpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun AddEditSftpServerDialog(
    existing: SftpServer? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Stable id up-front so an imported key can be staged before the server is saved
    val dialogId = remember { existing?.id ?: UUID.randomUUID().toString() }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var host by remember { mutableStateOf(existing?.host ?: "") }
    var port by remember { mutableStateOf((existing?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf(existing?.remotePath ?: "/") }
    var authType by remember { mutableStateOf(existing?.authType ?: SftpAuthType.PASSWORD) }
    var keyLabel by remember { mutableStateOf(existing?.keyLabel ?: "") }
    var passphrase by remember { mutableStateOf("") }
    var stagedKeyFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    fun currentKeyFile(): File? {
        stagedKeyFile?.takeIf { it.exists() }?.let { return it }
        return globalClass.sftpManager.keyFileFor(dialogId).takeIf { it.exists() }
    }

    // Drop an unused staged import if the dialog is closed without saving
    DisposableEffect(Unit) {
        onDispose {
            stagedKeyFile?.let { if (it.exists() && it.name.endsWith(".tmp")) it.delete() }
        }
    }

    val keyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val label = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
            }.getOrNull() ?: uri.lastPathSegment ?: "key"
            val tmp = File(globalClass.sftpManager.keysDir(), "$dialogId.key.tmp")
            val ok = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot read key file")
            }.isSuccess
            withContext(Dispatchers.Main) {
                if (ok) {
                    stagedKeyFile = tmp
                    keyLabel = label
                    error = null
                } else {
                    error = globalClass.getString(R.string.sftp_key_import_failed)
                }
            }
        }
    }

    fun buildProbe(): SftpServer = SftpServer(
        id = dialogId,
        name = name,
        host = host,
        port = port.toIntOrNull() ?: 22,
        username = username,
        remotePath = remotePath.ifBlank { "/" },
        authType = authType,
        keyLabel = keyLabel
    )

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
                Text(
                    text = stringResource(R.string.sftp_auth_method),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = authType == SftpAuthType.PASSWORD,
                        onClick = { authType = SftpAuthType.PASSWORD },
                        label = { Text(stringResource(R.string.sftp_auth_password)) }
                    )
                    FilterChip(
                        selected = authType == SftpAuthType.KEY,
                        onClick = { authType = SftpAuthType.KEY },
                        label = { Text(stringResource(R.string.sftp_auth_private_key)) }
                    )
                }
                if (authType == SftpAuthType.PASSWORD) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                stringResource(R.string.sftp_password) +
                                    if (existing != null && existing.authType == SftpAuthType.PASSWORD) " *" else ""
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = if (existing != null && existing.authType == SftpAuthType.PASSWORD) {
                            { Text("* leave blank to keep saved password") }
                        } else null
                    )
                } else {
                    val hasKey = currentKeyFile() != null
                    Text(
                        text = if (keyLabel.isNotBlank() && hasKey) {
                            globalClass.getString(R.string.sftp_key_selected, keyLabel)
                        } else {
                            stringResource(R.string.sftp_no_key_selected)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { keyPicker.launch(arrayOf("*/*")) }
                        ) {
                            Text(
                                stringResource(
                                    if (hasKey) R.string.sftp_replace_key_file
                                    else R.string.sftp_select_key_file
                                )
                            )
                        }
                        if (hasKey) {
                            TextButton(
                                onClick = {
                                    stagedKeyFile?.delete()
                                    stagedKeyFile = null
                                    globalClass.sftpManager.deleteKeyFile(dialogId)
                                    keyLabel = ""
                                }
                            ) { Text(stringResource(R.string.sftp_remove_key)) }
                        }
                    }
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(stringResource(R.string.sftp_key_passphrase_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = if (existing != null && existing.authType == SftpAuthType.KEY) {
                            { Text("* leave blank to keep saved passphrase") }
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
                        val portInt = port.toIntOrNull() ?: 22
                        if (host.isBlank() || username.isBlank()) {
                            error = globalClass.getString(R.string.sftp_invalid_input)
                            return@OutlinedButton
                        }
                        if (authType == SftpAuthType.KEY && currentKeyFile() == null) {
                            error = globalClass.getString(R.string.sftp_key_required)
                            return@OutlinedButton
                        }
                        testing = true
                        testResult = null
                        scope.launch(Dispatchers.IO) {
                            val probe = buildProbe()
                            val result = if (authType == SftpAuthType.KEY) {
                                val pp = passphrase.ifBlank {
                                    globalClass.sftpManager.credentialsStore
                                        .getKeyPassphrase(probe.id)
                                }
                                globalClass.sftpManager.testConnection(
                                    probe,
                                    keyFile = currentKeyFile(),
                                    passphrase = pp
                                )
                            } else {
                                // Use typed password if provided, else stored one for edits
                                val pw = password.ifBlank {
                                    globalClass.sftpManager.credentialsStore
                                        .getPassword(probe.id) ?: ""
                                }
                                globalClass.sftpManager.testConnection(probe, password = pw)
                            }
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
                    val needsPassword = authType == SftpAuthType.PASSWORD &&
                        (existing == null || existing.authType != SftpAuthType.PASSWORD)
                    if (host.isBlank() || username.isBlank() ||
                        (needsPassword && password.isBlank()) ||
                        (authType == SftpAuthType.KEY && currentKeyFile() == null)
                    ) {
                        error = globalClass.getString(
                            if (authType == SftpAuthType.KEY && currentKeyFile() == null) {
                                R.string.sftp_key_required
                            } else {
                                R.string.sftp_invalid_input
                            }
                        )
                        return@Button
                    }
                    scope.launch(Dispatchers.IO) {
                        val servers =
                            globalClass.sftpManager.getSavedServers().toMutableList()
                        val server = buildProbe().copy(port = portInt)
                        val idx = servers.indexOfFirst { it.id == dialogId }
                        if (idx >= 0) servers[idx] = server else servers.add(server)
                        globalClass.sftpManager.saveServers(servers)
                        if (authType == SftpAuthType.PASSWORD) {
                            if (password.isNotBlank()) {
                                globalClass.sftpManager.credentialsStore
                                    .savePassword(dialogId, password)
                            }
                        } else {
                            // Promote staged import to the live key file
                            stagedKeyFile?.takeIf { it.exists() }?.let { tmp ->
                                val live = globalClass.sftpManager.keyFileFor(dialogId)
                                runCatching {
                                    if (live.exists()) live.delete()
                                    tmp.renameTo(live)
                                }
                            }
                            stagedKeyFile = null
                            if (passphrase.isNotBlank()) {
                                globalClass.sftpManager.credentialsStore
                                    .saveKeyPassphrase(dialogId, passphrase)
                            }
                        }
                        // Drop any cached connection SAS credentials may have changed
                        globalClass.sftpManager.disconnect(dialogId)
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
