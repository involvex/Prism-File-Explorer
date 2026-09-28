package com.raival.compose.file.explorer.screen.main.tab.files.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.ui.Space
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.files.holder.LocalFileHolder
import com.raival.compose.file.explorer.screen.main.tab.files.misc.FileMimeType
import com.raival.compose.file.explorer.screen.main.tab.files.zip.ArchiveHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File

@Composable
fun ExtractArchiveDialog(
    show: Boolean,
    tab: FilesTab,
    onDismissRequest: () -> Unit
) {
    if (!show) return
    val target = tab.targetFile as? LocalFileHolder ?: run { onDismissRequest(); return }
    val ext = target.file.extension.lowercase()
    if (!FileMimeType.extractableArchiveFileType.contains(ext)) {
        onDismissRequest()
        return
    }

    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val defaultDest = remember(target.uniquePath) {
        File(target.file.parentFile, target.file.nameWithoutExtension)
    }
    var destName by remember { mutableStateOf(defaultDest.name) }

    Dialog(onDismissRequest = { if (!working) onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.extract_archive),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = target.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = destName,
                    onValueChange = { destName = it },
                    label = { Text(stringResource(R.string.destination_folder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = TextFieldDefaults.colors(
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                if (ext == "zip" || ext == "7z") {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it; error = "" },
                        label = { Text(stringResource(R.string.archive_password_optional)) },
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(
                            errorIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !working,
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(6.dp)
                    ) { Text(stringResource(R.string.cancel)) }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !working && destName.isNotBlank(),
                        onClick = {
                            working = true
                            scope.launch {
                                val destDir = File(target.file.parentFile, destName)
                                val ok = withContext(Dispatchers.IO) {
                                    runCatching {
                                        when (ext) {
                                            "zip", "jar", "apk", "apks" -> extractZip(
                                                target.file,
                                                destDir,
                                                password.ifBlank { null }
                                            )
                                            else -> ArchiveHelper.extractTo(
                                                target.file,
                                                destDir,
                                                password.ifBlank { null }?.toCharArray()
                                            )
                                        }
                                    }.getOrDefault(false)
                                }
                                working = false
                                if (ok) {
                                    onDismissRequest()
                                    tab.reloadFiles()
                                    globalClass.showMsg(R.string.task_completed)
                                } else {
                                    error = globalClass.getString(R.string.failed_to_extract_files_from_zip)
                                }
                            }
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (working) CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        Text(stringResource(R.string.extract))
                    }
                }
                Space(0.dp)
            }
        }
    }
}

private fun extractZip(file: File, destDir: File, password: String?): Boolean {
    return try {
        destDir.mkdirs()
        val zip = if (!password.isNullOrEmpty()) ZipFile(file, password.toCharArray()) else ZipFile(file)
        zip.use {
            if (it.isEncrypted && password.isNullOrEmpty()) return false
            it.extractAll(destDir.absolutePath)
        }
        true
    } catch (e: Exception) {
        com.raival.compose.file.explorer.App.Companion.logger.logError(e)
        false
    }
}
