package com.raival.compose.file.explorer.screen.viewer

import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.ui.SafeSurface
import com.raival.compose.file.explorer.screen.viewer.pdf.PdfViewerInstance
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.DocumentConverter
import com.raival.compose.file.explorer.screen.viewer.pdf.ui.PdfViewerContent
import com.raival.compose.file.explorer.theme.FileExplorerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import java.io.File

class DocumentViewerActivity : ViewerActivity() {
    private var conversionError: String? = null

    override fun onCreateNewInstance(uri: Uri, uid: String): ViewerInstance {
        val outputFile = File(globalClass.cleanOnExitDir.file, "doc_${System.currentTimeMillis()}.pdf")
        val converter = DocumentConverter()
        val success = runBlocking {
            withContext(Dispatchers.IO) {
                converter.convertOfficeToPdf(uri, outputFile)
            }
        }

        return if (success) {
            val pdfUri = FileProvider.getUriForFile(
                globalClass,
                "${globalClass.packageName}.provider",
                outputFile
            )
            PdfViewerInstance(pdfUri, uid)
        } else {
            conversionError = globalClass.getString(R.string.conversion_failed_msg)
            DocumentViewerInstance(uri, uid)
        }
    }

    @OptIn(ExperimentalZoomableApi::class)
    override fun onReady(instance: ViewerInstance) {
        when (instance) {
            is PdfViewerInstance -> {
                setContent {
                    FileExplorerTheme {
                        SafeSurface(false) {
                            PdfViewerContent(
                                instance = instance,
                                onBackPress = { onBackPressedDispatcher.onBackPressed() }
                            )
                        }
                    }
                }
            }
            else -> {
                setContent {
                    FileExplorerTheme {
                        SafeSurface(false) {
                            ConversionErrorDialog(
                                message = conversionError ?: getString(R.string.conversion_failed),
                                onDismiss = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}
