package com.raival.compose.file.explorer.screen.main.tab.files.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.toFormattedSize
import com.raival.compose.file.explorer.screen.main.tab.files.FilesTab
import com.raival.compose.file.explorer.screen.main.tab.files.holder.ContentHolder
import com.raival.compose.file.explorer.screen.main.tab.files.holder.LocalFileHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

private data class ChildSize(val holder: ContentHolder, val bytes: Long)
private data class LargestFile(val path: String, val name: String, val bytes: Long)

@Composable
fun StorageAnalyzerDialog(
    show: Boolean,
    tab: FilesTab,
    onDismissRequest: () -> Unit
) {
    if (!show) return
    val folder = tab.targetFile?.takeIf { it.isFolder } ?: tab.activeFolder
    if (folder !is LocalFileHolder) {
        onDismissRequest()
        return
    }

    var scanning by remember(folder.uniquePath) { mutableStateOf(true) }
    var children by remember(folder.uniquePath) { mutableStateOf<List<ChildSize>>(emptyList()) }
    var largest by remember(folder.uniquePath) { mutableStateOf<List<LargestFile>>(emptyList()) }
    var totalBytes by remember(folder.uniquePath) { mutableStateOf(0L) }
    var showLargest by remember { mutableStateOf(false) }

    LaunchedEffect(folder.uniquePath) {
        scanning = true
        val result = withContext(Dispatchers.IO) {
            val childSizes = arrayListOf<ChildSize>()
            val topFiles = ArrayList<LargestFile>()
            var total = 0L
            runCatching {
                val kids = folder.listContent()
                for (kid in kids) {
                    if (!isActive) break
                    if (kid !is LocalFileHolder) continue
                    val size = dirSize(kid.file, topFiles, depth = 0)
                    total += size
                    childSizes.add(ChildSize(kid, size))
                }
            }
            childSizes.sortByDescending { it.bytes }
            topFiles.sortByDescending { it.bytes }
            Triple(childSizes.take(30), topFiles.take(100), total)
        }
        children = result.first
        largest = result.second
        totalBytes = result.third
        scanning = false
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.storage_analyzer),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = folder.displayName + " • " + totalBytes.toFormattedSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showLargest = false }) {
                        Text(
                            stringResource(R.string.folders),
                            fontWeight = if (!showLargest) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    TextButton(onClick = { showLargest = true }) {
                        Text(
                            stringResource(R.string.largest_files),
                            fontWeight = if (showLargest) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (scanning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (!showLargest) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(children, key = { it.holder.uniquePath }) { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = item.holder.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.bytes.toFormattedSize(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val frac = if (totalBytes > 0) item.bytes.toFloat() / totalBytes else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(frac.coerceIn(0f, 1f))
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(largest, key = { it.path }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = item.bytes.toFormattedSize(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    onClick = onDismissRequest
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

private fun dirSize(root: File, topFiles: MutableList<LargestFile>, depth: Int): Long {
    if (!root.exists()) return 0L
    if (root.isFile) {
        val len = runCatching { root.length() }.getOrDefault(0L)
        if (depth <= 6) {
            topFiles.add(LargestFile(root.absolutePath, root.name, len))
            if (topFiles.size > 400) {
                topFiles.sortByDescending { it.bytes }
                while (topFiles.size > 200) topFiles.removeAt(topFiles.lastIndex)
            }
        }
        return len
    }
    var total = 0L
    // Avoid runaway recursion on huge trees; depth cap keeps scan fast
    if (depth > 8) return 0L
    runCatching {
        root.listFiles()?.forEach { total += dirSize(it, topFiles, depth + 1) }
    }
    return total
}
