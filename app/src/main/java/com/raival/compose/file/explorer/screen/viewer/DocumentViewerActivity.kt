package com.raival.compose.file.explorer.screen.viewer

import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.raival.compose.file.explorer.App.Companion.globalClass
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
            showConversionError(previousError)
            return
        }

        // Show a progress state immediately, then replace it with the PDF viewer.
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
                    if (!DocumentConverter().convertOfficeToPdf(instance.uri, outputFile)) return@runCatching null

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
                    showConversionError(message)
                }
            } else {
                pdfInstance?.onClose()
            }
        }
    }

    private fun showConversionError(message: String) {
        setContent {
            FileExplorerTheme {
                SafeSurface(false) {
                    ConversionErrorDialog(
                        message = message.ifBlank { stringResource(R.string.conversion_failed) },
                        onDismiss = { finish() }
                    )
                }
            }
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
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
