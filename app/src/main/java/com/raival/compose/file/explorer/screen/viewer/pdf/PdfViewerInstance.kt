package com.raival.compose.file.explorer.screen.viewer.pdf

import android.content.ContentResolver
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.text.format.Formatter
import android.util.Size
import com.anggrayudi.storage.extension.toDocumentFile
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.App.Companion.logger
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.isNot
import com.raival.compose.file.explorer.common.name
import com.raival.compose.file.explorer.common.toFormattedDate
import com.raival.compose.file.explorer.screen.viewer.ViewerInstance
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfFormField
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfMetadata
import com.raival.compose.file.explorer.screen.viewer.pdf.misc.PdfPageHolder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDNonTerminalField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDListBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDComboBox
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PdfViewerInstance(
    override val uri: Uri,
    override val id: String
) : ViewerInstance {
    private val fileDescriptor = try {
        globalClass.contentResolver.openFileDescriptor(uri, "r")
    } catch (e: Exception) {
        logger.logError(e)
        null
    }

    private val pdfRenderer = fileDescriptor?.let {
        try {
            PdfRenderer(it)
        } catch (e: Exception) {
            logger.logError(e)
            null
        }
    }

    val pages = arrayListOf<PdfPageHolder>()
    var defaultPageSize = Size(0, 0)
        private set

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private var pdDocument: PDDocument? = null
    private var acroForm: PDAcroForm? = null
    private val modifiedFieldValues = mutableMapOf<String, String>()

    var hasFormFields = false
        private set

    var hasUnsavedFormChanges = false
        private set

    val metadata by lazy {
        PdfMetadata(
            name = uri.name ?: globalClass.getString(R.string.unknown),
            path = uri.toString(),
            size = fileDescriptor?.statSize ?: 0L,
            lastModified = uri.toDocumentFile(globalClass)?.lastModified() ?: 0L,
            pages = pdfRenderer?.pageCount ?: 0
        )
    }

    private var isReady = false

    private var originalFile: File? = null

    init {
        try {
            val inputStream = openInputStream(uri)
            if (inputStream != null) {
                pdDocument = PDDocument.load(inputStream)
                pdDocument?.let { doc ->
                    val catalog = doc.documentCatalog
                    acroForm = catalog?.acroForm
                    hasFormFields = acroForm != null && acroForm?.fields?.isNotEmpty() == true
                }
            }
        } catch (e: Exception) {
            logger.logError(e)
        }
    }

    private fun openInputStream(uri: Uri): InputStream? {
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    val path = uri.path ?: return null
                    File(path).inputStream()
                }
                ContentResolver.SCHEME_CONTENT -> {
                    globalClass.contentResolver.openInputStream(uri)
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.logError(e)
            null
        }
    }

    private fun getOriginalFile(): File? {
        if (originalFile != null) return originalFile
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    File(uri.path ?: return null)
                }
                ContentResolver.SCHEME_CONTENT -> {
                    val tempFile = File(globalClass.cleanOnExitDir.file, "pdf_temp_${System.currentTimeMillis()}.pdf")
                    globalClass.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.logError(e)
            null
        }
    }

    fun prepare(onPrepared: (success: Boolean) -> Unit) {
        try {
            if (!isValid()) {
                onPrepared(false)
                return
            }

            if (!isReady) {
                pages.clear()

                for (i in 0 until pdfRenderer!!.pageCount) {
                    pages.add(PdfPageHolder(index = i))
                }

                val samplePageIndex = if (pages.size == 1) 0 else minOf(1, pages.size - 1)
                pdfRenderer.openPage(samplePageIndex).use { page ->
                    defaultPageSize = Size(page.width, page.height)
                }

                isReady = true
            }
            onPrepared(true)
        } catch (e: Exception) {
            logger.logError(e)
            onPrepared(false)
        }
    }

    fun getInfo(): List<Pair<String, String>> = listOf(
        globalClass.getString(R.string.name) to metadata.name,
        globalClass.getString(R.string.page_count) to metadata.pages.toString(),
        globalClass.getString(R.string.size) to Formatter.formatFileSize(
            globalClass,
            metadata.size
        ),
        globalClass.getString(R.string.path) to metadata.path,
        globalClass.getString(R.string.last_modified) to metadata.lastModified.toFormattedDate()
    )

    fun renderPage(page: PdfPageHolder, scale: Float = 2f, onFinished: (PdfPageHolder) -> Unit) {
        if (pdfRenderer != null) {
            page.render(scale, scope, mutex, pdfRenderer, onFinished = { onFinished(page) })
        }
    }

    fun recycle(page: PdfPageHolder) {
        page.recycle()
    }

    fun isValid(): Boolean = pdfRenderer isNot null

    // Form field operations

    fun getFormFields(): List<PdfFormField> {
        val fields = arrayListOf<PdfFormField>()
        try {
            val form = acroForm
            if (form == null || form.fields.isEmpty()) return emptyList()

            for (field in form.fields) {
                collectFormFields(field, 0, fields)
            }
        } catch (e: Exception) {
            logger.logError(e)
        }
        return fields
    }

    private fun collectFormFields(field: PDField, pageIndex: Int, result: ArrayList<PdfFormField>) {
        try {
            if (field is PDNonTerminalField) {
                val children = field.children
                if (children != null && children.isNotEmpty()) {
                    for (child in children) {
                        collectFormFields(child, pageIndex, result)
                    }
                    return
                }
            }

            val widget: PDAnnotationWidget? = try {
                field.widgets?.firstOrNull()
            } catch (e: Exception) {
                null
            }

            val fieldRect = try {
                val widgetAnnotation = widget as? PDAnnotation
                val cosObj = widgetAnnotation?.getCOSObject()
                val cosDict = cosObj as? com.tom_roush.pdfbox.cos.COSDictionary
                val rectArray = cosDict?.getDictionaryObject(com.tom_roush.pdfbox.cos.COSName.RECT) as? com.tom_roush.pdfbox.cos.COSArray
                if (rectArray != null) PDRectangle(rectArray) else null
            } catch (e: Exception) {
                null
            }
            if (fieldRect != null) {
                val widgetPage = try {
                    val widgetAnnotation = widget as? PDAnnotation
                    widgetAnnotation?.getPage()
                } catch (e: Exception) {
                    null
                }
                val pageNum = if (widgetPage != null) {
                    try {
                        pdDocument?.pages?.indexOf(widgetPage) ?: pageIndex
                    } catch (e: Exception) {
                        pageIndex
                    }
                } else pageIndex

                val fieldType = when (field) {
                    is PDTextField -> PdfFormField.FieldType.TEXT
                    is PDCheckBox -> PdfFormField.FieldType.CHECKBOX
                    is PDRadioButton -> PdfFormField.FieldType.RADIO_BUTTON
                    is PDListBox -> PdfFormField.FieldType.LIST
                    is PDComboBox -> PdfFormField.FieldType.COMBO_BOX
                    else -> PdfFormField.FieldType.UNKNOWN
                }

                val currentValue = getFieldValue(field) ?: ""
                val options = getFieldOptions(field)

                val normalizedRect = normalizeRect(fieldRect, pageNum)

                val fqn = field.fullyQualifiedName ?: ""
                val name = field.alternateFieldName ?: field.partialName ?: fqn

                result.add(
                    PdfFormField(
                        fullyQualifiedName = fqn,
                        displayLabel = name,
                        fieldType = fieldType,
                        value = modifiedFieldValues[fqn] ?: currentValue,
                        pageIndex = pageNum,
                        rect = normalizedRect,
                        options = options
                    )
                )
            }
        } catch (e: Exception) {
            logger.logError(e)
        }
    }

    private fun getFieldValue(field: PDField): String? {
        return try {
            when (field) {
                is PDTextField -> field.valueAsString
                is PDCheckBox -> if (field.isChecked) "Yes" else "Off"
                is PDRadioButton -> field.valueAsString
                is PDListBox -> field.valueAsString
                is PDComboBox -> field.valueAsString
                else -> field.valueAsString
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFieldOptions(field: PDField): List<String> {
        return try {
            when (field) {
                is PDListBox -> field.options ?: emptyList()
                is PDComboBox -> field.options ?: emptyList()
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun normalizeRect(rect: PDRectangle, pageIndex: Int): RectF {
        val page = try {
            pdDocument?.pages?.get(pageIndex)
        } catch (e: Exception) {
            null
        }
        val mediaBox = page?.mediaBox
        val pageWidthPt = mediaBox?.width ?: 612f
        val pageHeightPt = mediaBox?.height ?: 792f

        val left = rect.lowerLeftX
        val bottom = rect.lowerLeftY
        val right = rect.upperRightX
        val top = rect.upperRightY

        val topInAndroid = pageHeightPt - top
        val bottomInAndroid = pageHeightPt - bottom

        val normLeft = left / pageWidthPt
        val normTop = topInAndroid / pageHeightPt
        val normRight = right / pageWidthPt
        val normBottom = bottomInAndroid / pageHeightPt

        return RectF(
            normLeft.coerceIn(0f, 1f),
            normTop.coerceIn(0f, 1f),
            normRight.coerceIn(0f, 1f),
            normBottom.coerceIn(0f, 1f)
        )
    }

    fun fillFormField(fieldName: String, value: String) {
        try {
            val field = acroForm?.getField(fieldName)
            if (field != null) {
                when (field) {
                    is PDTextField -> field.setValue(value)
                    is PDCheckBox -> {
                        if (value.equals("Yes", ignoreCase = true)) field.check() else field.unCheck()
                    }
                    is PDRadioButton -> field.setValue(value)
                    is PDListBox -> field.setValue(value)
                    is PDComboBox -> field.setValue(value)
                }
                modifiedFieldValues[fieldName] = value
                hasUnsavedFormChanges = true
            }
        } catch (e: Exception) {
            logger.logError(e)
        }
    }

    fun savePdf(onResult: (success: Boolean, message: String?) -> Unit) {
        runBlocking(scope.coroutineContext) {
            try {
                val file = getOriginalFile()
                if (file == null) {
                    onResult(false, globalClass.getString(R.string.failed_to_save_pdf))
                    return@runBlocking
                }

                val tempFile = File(file.parentFile, "${file.nameWithoutExtension}_filled.pdf")
                val outputStream = FileOutputStream(tempFile)
                pdDocument?.save(outputStream)
                outputStream.close()

                val resolver = globalClass.contentResolver
                when (uri.scheme) {
                    ContentResolver.SCHEME_FILE -> {
                        if (file.canWrite()) {
                            tempFile.copyTo(file, overwrite = true)
                        } else {
                            onResult(false, globalClass.getString(R.string.permission_denied))
                            return@runBlocking
                        }
                    }
                    ContentResolver.SCHEME_CONTENT -> {
                        resolver.openOutputStream(uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                    else -> {
                        onResult(false, globalClass.getString(R.string.unsupported_scheme))
                        return@runBlocking
                    }
                }

                tempFile.delete()
                hasUnsavedFormChanges = false
                modifiedFieldValues.clear()
                onResult(true, null)
            } catch (e: Exception) {
                logger.logError(e)
                onResult(false, globalClass.getString(R.string.failed_to_save_pdf))
            }
        }
    }

    fun exportToMarkdown(output: File): Boolean {
        return try {
            val doc = pdDocument
            if (doc == null) return false
            val stripper = PDFTextStripper()
            val text = stripper.getText(doc)
            output.writeText(formatAsMarkdown(text))
            true
        } catch (e: Exception) {
            logger.logError(e)
            false
        }
    }

    private fun formatAsMarkdown(text: String): String {
        val lines = text.lines()
        val result = StringBuilder()
        var prevBlank = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (!prevBlank) {
                    result.append("\n")
                }
                prevBlank = true
            } else {
                result.append(trimmed).append("\n")
                prevBlank = false
            }
        }

        return result.toString().trim() + "\n"
    }

    fun exportToOdf(output: File): Boolean {
        return try {
            val doc = pdDocument
            if (doc == null) return false
            val stripper = PDFTextStripper()
            val text = stripper.getText(doc)
            createOdfFromText(text, output)
            true
        } catch (e: Exception) {
            logger.logError(e)
            false
        }
    }

    private fun createOdfFromText(text: String, output: File) {
        val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2">
                <office:body>
                    <office:text>
                        <text:p>$escapedText</text:p>
                    </office:text>
                </office:body>
            </office:document-content>
        """.trimIndent()

        val manifestXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
                <manifest:file-entry manifest:media-type="application/vnd.oasis.opendocument.text" manifest:full-path="/"/>
                <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="content.xml"/>
            </manifest:manifest>
        """.trimIndent()

        val zipOutput = java.util.zip.ZipOutputStream(FileOutputStream(output))
        zipOutput.putNextEntry(java.util.zip.ZipEntry("mimetype"))
        zipOutput.write("application/vnd.oasis.opendocument.text".toByteArray())
        zipOutput.closeEntry()
        zipOutput.putNextEntry(java.util.zip.ZipEntry("content.xml"))
        zipOutput.write(contentXml.toByteArray())
        zipOutput.closeEntry()
        zipOutput.putNextEntry(java.util.zip.ZipEntry("META-INF/manifest.xml"))
        zipOutput.write(manifestXml.toByteArray())
        zipOutput.closeEntry()
        zipOutput.close()
    }

    override fun onClose() {
        runBlocking {
            try {
                scope.cancel()
                pages.forEach { it.recycle() }
                pdfRenderer?.close()
                fileDescriptor?.close()
                pdDocument?.close()
            } catch (e: Exception) {
                logger.logError(e)
            }
        }
    }
}
