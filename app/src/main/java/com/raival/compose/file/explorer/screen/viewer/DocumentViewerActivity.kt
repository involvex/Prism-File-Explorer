package com.raival.compose.file.explorer.screen.viewer

import android.net.Uri
import androidx.activity.compose.setContent
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
                globalClass.showMsg(getString(R.string.failed_to_load_pdf))
                finish()
            }
        }
    }
}
