package com.raival.compose.file.explorer.screen.viewer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.App.Companion.logger
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.ui.SafeSurface
import com.raival.compose.file.explorer.screen.viewer.pdf.PdfViewerInstance
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.DocumentConverter
import com.raival.compose.file.explorer.screen.viewer.pdf.ui.PdfViewerContent
import com.raival.compose.file.explorer.theme.FileExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import java.io.File

class DocumentViewerActivity : ViewerActivity() {

    override fun onCreateNewInstance(uri: Uri, uid: String): ViewerInstance {
        // Cheap placeholder. The expensive Office -> PDF conversion is done on
        // Dispatchers.IO from onReady so Activity creation never blocks Main.
        return DocumentViewerInstance(uri, uid)
    }

    @OptIn(ExperimentalZoomableApi::class)
    override fun onReady(instance: ViewerInstance, parentDir: String?) {
        // After a rotation ViewersManager already holds the converted PDF instance
        // (or the placeholder carrying a previous conversion error).
        if (instance is PdfViewerInstance) {
            setPdfContent(instance, parentDir)
            return
        }
        val previousError = (instance as? DocumentViewerInstance)?.conversionError
        if (previousError != null) {
            showConversionError(instance, previousError, parentDir)
            return
        }

        startConversion(instance, parentDir)
    }

    private fun startConversion(instance: ViewerInstance, parentDir: String?) {
        setContent {
            FileExplorerTheme {
                SafeSurface(false) {
                    ConversionLoading()
                }
            }
        }

        lifecycleScope.launch {
            val pdfInstance = withContext(Dispatchers.IO) {
                runCatching {
                    val outputFile = File(
                        globalClass.cleanOnExitDir.file,
                        "doc_${System.currentTimeMillis()}.pdf"
                    )
                    val hintName = resolveDisplayName(instance.uri)
                    val hintExtension = hintName
                        ?.substringAfterLast('.', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                    val hintMime = runCatching {
                        globalClass.contentResolver.getType(instance.uri)
                    }.getOrNull()

                    if (!DocumentConverter().convertOfficeToPdf(
                            uri = instance.uri,
                            output = outputFile,
                            hintExtension = hintExtension,
                            hintMimeType = hintMime
                        )
                    ) return@runCatching null

                    val pdfUri = FileProvider.getUriForFile(
                        globalClass,
                        "${globalClass.packageName}.provider",
                        outputFile
                    )
                    PdfViewerInstance(pdfUri, instance.id)
                }.getOrNull()
            }

            if (!isFinishing && !isDestroyed) {
                if (pdfInstance != null) {
                    replaceInstance(pdfInstance)
                    setPdfContent(pdfInstance, parentDir)
                } else {
                    val message = globalClass.getString(R.string.conversion_failed_msg)
                    (instance as? DocumentViewerInstance)?.conversionError = message
                    showConversionError(instance, message, parentDir)
                }
            } else {
                pdfInstance?.onClose()
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            globalClass.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private fun showConversionError(
        instance: ViewerInstance,
        message: String,
        parentDir: String?
    ) {
        setContent {
            FileExplorerTheme {
                SafeSurface(false) {
                    ConversionErrorDialog(
                        message = message.ifBlank { stringResource(R.string.conversion_failed) },
                        onDismiss = { finish() },
                        onRetry = {
                            (instance as? DocumentViewerInstance)?.conversionError = null
                            startConversion(instance, parentDir)
                        },
                        onOpenExternal = {
                            openExternally(instance.uri)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun openExternally(uri: Uri) {
        val mime = runCatching { contentResolver.getType(uri) }.getOrNull()
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.conversion_failed_open_in)))
        } catch (_: ActivityNotFoundException) {
            globalClass.showMsg(R.string.no_app_can_open_file)
        } catch (e: Exception) {
            logger.logError(e)
            globalClass.showMsg(R.string.failed_to_open_this_file)
        }
    }

    @OptIn(ExperimentalZoomableApi::class)
    private fun setPdfContent(instance: PdfViewerInstance, parentDir: String?) {
        setContent {
            FileExplorerTheme {
                SafeSurface(false) {
                    PdfViewerContent(
                        instance = instance,
                        parentDir = parentDir,
                        onBackPress = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    @Composable
    private fun ConversionLoading() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.converting_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
