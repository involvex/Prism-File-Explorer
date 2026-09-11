package com.raival.compose.file.explorer.screen.viewer.pdf.ui

import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.isNot
import com.raival.compose.file.explorer.screen.viewer.pdf.PdfViewerInstance
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfFormField
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfPageHolder
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarLayoutSide
import my.nanihadesuka.compose.ScrollbarSettings
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomableWithScroll
import java.io.File

@OptIn(ExperimentalZoomableApi::class)
@Composable
fun PdfViewerContent(
    instance: PdfViewerInstance,
    parentDir: String? = null,
    onBackPress: () -> Unit
) {
    BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val constraints = this.constraints
        var showToolbars by remember { mutableStateOf(true) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val pdfPages = remember { mutableStateListOf<PdfPageHolder>() }
        var showInfoDialog by remember { mutableStateOf(false) }

        var isEditMode by remember { mutableStateOf(false) }
        val formFieldsByPage = remember { mutableStateListOf<PdfFormField>() }
        var isExporting by remember { mutableStateOf(false) }
        var showExportChoice by remember { mutableStateOf<ExportType?>(null) }

        val listState = rememberLazyListState()
        val zoomState = rememberZoomState()
        var defaultPageSize by remember { mutableStateOf(Size(0, 0)) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val markdownSaveLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/markdown")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val tempFile = File.createTempFile("export", ".md", globalClass.cleanOnExitDir.file)
                    val success = instance.exportToMarkdown(tempFile)
                    if (success) {
                        globalClass.contentResolver.openOutputStream(uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        Toast.makeText(context, globalClass.getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, globalClass.getString(R.string.failed_to_save_pdf), Toast.LENGTH_SHORT).show()
                    }
                    isExporting = false
                }
            } else {
                isExporting = false
            }
        }
        val odfSaveLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/vnd.oasis.opendocument.text")
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val tempFile = File.createTempFile("export", ".odt", globalClass.cleanOnExitDir.file)
                    val success = instance.exportToOdf(tempFile)
                    if (success) {
                        globalClass.contentResolver.openOutputStream(uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        Toast.makeText(context, globalClass.getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, globalClass.getString(R.string.failed_to_save_pdf), Toast.LENGTH_SHORT).show()
                    }
                    isExporting = false
                }
            } else {
                isExporting = false
            }
        }

        val isFirstItemVisible by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0
            }
        }

        val visiblePageNumbers by remember {
            derivedStateOf {
                val visiblePages = listState.layoutInfo.visibleItemsInfo.map { visibleItem ->
                    visibleItem.index
                }.filter { it > 0 }
                buildString {
                    append(visiblePages.firstOrNull() ?: 1)
                    if (visiblePages.size > 1) {
                        append("-")
                        append(visiblePages.lastOrNull() ?: 1)
                    }
                }
            }
        }

        LaunchedEffect(isFirstItemVisible) {
            showToolbars = isFirstItemVisible
        }

        LaunchedEffect(Unit) {
            withContext(IO) {
                instance.prepare { success ->
                    if (success) {
                        defaultPageSize = Size(
                            constraints.maxWidth,
                            instance.defaultPageSize.height * constraints.maxWidth / instance.defaultPageSize.width
                        )
                        pdfPages.addAll(instance.pages)

                        if (instance.hasFormFields) {
                            formFieldsByPage.addAll(instance.getFormFields())
                        }

                        isLoading = false
                    } else {
                        errorMessage = globalClass.getString(R.string.failed_to_load_pdf)
                        isLoading = false
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                instance.onClose()
            }
        }

        if (showInfoDialog) {
            InfoDialog(
                title = globalClass.getString(R.string.pdf_info),
                properties = instance.getInfo(),
                onDismiss = { showInfoDialog = false }
            )
        }

        when {
            isLoading -> LoadingScreen()
            errorMessage isNot null -> ErrorScreen(
                message = errorMessage!!,
                onClose = onBackPress
            )

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomableWithScroll(
                            zoomState = zoomState,
                            onTap = { if (!isFirstItemVisible) showToolbars = !showToolbars }
                        ),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(100.dp)) }

                    items(
                        items = pdfPages,
                        key = { it.index }
                    ) { page ->
                        val pageFormFields = formFieldsByPage.filter { it.pageIndex == page.index }
                        PdfPageItem(
                            page = page,
                            pageSize = defaultPageSize,
                            instance = instance,
                            zoomState = zoomState,
                            isEditMode = isEditMode,
                            formFields = pageFormFields,
                            onFormFieldChange = { field, newValue ->
                                instance.fillFormField(field.fullyQualifiedName, newValue)
                                val index = formFieldsByPage.indexOfFirst { it.fullyQualifiedName == field.fullyQualifiedName }
                                if (index >= 0) {
                                    formFieldsByPage[index] = field.copy(value = newValue)
                                }
                            }
                        )
                    }
                }
                InternalLazyColumnScrollbar(
                    modifier = Modifier.padding(top = 110.dp),
                    state = listState,
                    settings = ScrollbarSettings.Default.copy(
                        thumbUnselectedColor = colorScheme.surfaceContainerHigh,
                        thumbSelectedColor = colorScheme.primary,
                        side = ScrollbarLayoutSide.Start,
                        thumbThickness = 6.dp,
                        scrollbarPadding = 12.dp
                    ),
                    indicatorContent = { index, isPressed ->
                        PageIndicator(
                            pageNumber = visiblePageNumbers,
                            isPressed = isPressed,
                            totalPages = pdfPages.size
                        )
                    }
                )

                TopToolbar(
                    visible = showToolbars,
                    title = instance.metadata.name,
                    onBackClick = onBackPress,
                    onInfoClick = {
                        showInfoDialog = true
                    },
                    isEditMode = isEditMode,
                    hasUnsavedChanges = instance.hasUnsavedFormChanges,
                    onEditModeToggle = {
                        isEditMode = !isEditMode
                    },
                    onSaveClick = {
                        coroutineScope.launch {
                            instance.savePdf { success, message ->
                                if (!success) {
                                    Toast.makeText(context, message ?: globalClass.getString(R.string.failed_to_save_pdf), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, globalClass.getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
                                }
                                isEditMode = false
                            }
                        }
                    },
                    onExportMarkdown = {
                        if (parentDir != null) {
                            showExportChoice = ExportType.MARKDOWN
                        } else {
                            isExporting = true
                            markdownSaveLauncher.launch("markdown.md")
                        }
                    },
                    onExportOdf = {
                        if (parentDir != null) {
                            showExportChoice = ExportType.ODF
                        } else {
                            isExporting = true
                            odfSaveLauncher.launch("document.odt")
                        }
                    }
                )

                if (isExporting) {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        title = { Text("Exporting") },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("Please wait...")
                            }
                        }
                    )
                }

                if (showExportChoice != null) {
                    val exportType = showExportChoice!!
                    AlertDialog(
                        onDismissRequest = { showExportChoice = null },
                        confirmButton = { TextButton(onClick = {
                            isExporting = true
                            showExportChoice = null
                            when (exportType) {
                                ExportType.MARKDOWN -> {
                                    coroutineScope.launch {
                                        val result = instance.saveMarkdownToFolder(parentDir!!)
                                        if (result != null) {
                                            Toast.makeText(context, globalClass.getString(R.string.saved_successfully) + ": " + result.name, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, globalClass.getString(R.string.failed_to_save_pdf), Toast.LENGTH_SHORT).show()
                                        }
                                        isExporting = false
                                    }
                                }
                                ExportType.ODF -> {
                                    coroutineScope.launch {
                                        val result = instance.saveOdfToFolder(parentDir!!)
                                        if (result != null) {
                                            Toast.makeText(context, globalClass.getString(R.string.saved_successfully) + ": " + result.name, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, globalClass.getString(R.string.failed_to_save_pdf), Toast.LENGTH_SHORT).show()
                                        }
                                        isExporting = false
                                    }
                                }
                            }
                        }) { Text("Save to folder") } },
                        dismissButton = { TextButton(onClick = {
                            showExportChoice = null
                            when (exportType) {
                                ExportType.MARKDOWN -> markdownSaveLauncher.launch("markdown.md")
                                ExportType.ODF -> odfSaveLauncher.launch("document.odt")
                            }
                            isExporting = true
                        }) { Text("Save as...") } },
                        title = { Text("Save Location") },
                        text = { Text("Save to current folder or choose another location?") }
                    )
                }
            }
        }
    }
}

private enum class ExportType { MARKDOWN, ODF }
