package com.raival.compose.file.explorer.screen.viewer.pdf.misc

import android.net.Uri
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.App.Companion.logger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xslf.extractor.XSLFExtractor
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class DocumentConverter {
    companion object {
        private const val MAX_DOC_SIZE_BYTES = 50L * 1024 * 1024  // 50 MB
    }

    fun convertOfficeToPdf(uri: Uri, output: File): Boolean {
        return try {
            val assetFile = globalClass.contentResolver.openFileDescriptor(uri, "r") ?: return false
            val fileSize = assetFile.statSize
            if (fileSize > MAX_DOC_SIZE_BYTES) {
                assetFile.close()
                logger.logError("Document too large: $fileSize bytes (max $MAX_DOC_SIZE_BYTES)")
                return false
            }
            assetFile.close()
            val content = globalClass.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            val ext = File(uri.path ?: "").extension.lowercase()
            val text = when (ext) {
                "docx" -> extractDocx(content)
                "doc" -> extractDoc(content)
                "xlsx" -> extractXlsx(content)
                "xls" -> extractXls(content)
                "pptx" -> extractPptx(content)
                "ppt" -> extractPpt(content)
                "odt", "ods", "odp" -> extractOdf(uri, ext)
                else -> return false
            }

            if (text.isBlank()) {
                return false
            }

            createPdfFromText(text, output)
            true
        } catch (e: Exception) {
            logger.logError(e)
            false
        }
    }

    private fun extractDocx(content: ByteArray): String {
        return try {
            val doc = XWPFDocument(content.inputStream())
            val extractor = XWPFWordExtractor(doc)
            val text = extractor.text
            extractor.close()
            doc.close()
            text ?: ""
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractDoc(content: ByteArray): String {
        return try {
            val doc = HWPFDocument(content.inputStream())
            val range = doc.range
            range.text()
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractXlsx(content: ByteArray): String {
        return try {
            val workbook = XSSFWorkbook(content.inputStream())
            val text = StringBuilder()
            val sheetCount = workbook.numberOfSheets
            for (i in 0 until sheetCount) {
                val sheet = workbook.getSheetAt(i)
                text.append("Sheet: ").append(sheet.sheetName).append("\n")
                val rowIterator = sheet.iterator()
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    val cellIterator = row.cellIterator()
                    while (cellIterator.hasNext()) {
                        val cell = cellIterator.next()
                        text.append(cell?.toString()).append("\t")
                    }
                    text.append("\n")
                }
            }
            workbook.close()
            text.toString()
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractXls(content: ByteArray): String {
        return try {
            val fs = POIFSFileSystem(content.inputStream())
            val workbook = HSSFWorkbook(fs)
            val text = StringBuilder()
            val sheetCount = workbook.numberOfSheets
            for (i in 0 until sheetCount) {
                val sheet = workbook.getSheetAt(i)
                text.append("Sheet: ").append(sheet.sheetName).append("\n")
                val rowIterator = sheet.iterator()
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    val cellIterator = row.cellIterator()
                    while (cellIterator.hasNext()) {
                        val cell = cellIterator.next()
                        text.append(cell?.toString()).append("\t")
                    }
                    text.append("\n")
                }
            }
            workbook.close()
            fs.close()
            text.toString()
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractPptx(content: ByteArray): String {
        return try {
            val ppt = XMLSlideShow(content.inputStream())
            val extractor = XSLFExtractor(ppt)
            val text = extractor.text
            extractor.close()
            ppt.close()
            text ?: ""
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractPpt(content: ByteArray): String {
        return try {
            val fs = POIFSFileSystem(content.inputStream())
            val ppt = HSLFSlideShow(fs)
            val text = StringBuilder()
            for (slide in ppt.slides) {
                text.append("--- Slide ---\n")
                text.append(slide.toString()).append("\n")
            }
            ppt.close()
            fs.close()
            text.toString()
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun extractOdf(uri: Uri, ext: String): String {
        return try {
            val file = File(globalClass.cleanOnExitDir.file, "odf_temp_${System.currentTimeMillis()}.${ext}")
            globalClass.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            val text = StringBuilder()
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("content.xml")
                if (entry != null) {
                    val contentXml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    text.append(stripXmlTags(contentXml))
                }
                val metaEntry = zip.getEntry("meta.xml")
                if (metaEntry != null) {
                    val metaXml = zip.getInputStream(metaEntry).bufferedReader().use { it.readText() }
                    text.append("\n--- Metadata ---\n")
                    text.append(stripXmlTags(metaXml))
                }
            }
            file.delete()
            text.toString()
        } catch (e: Exception) {
            logger.logError(e)
            ""
        }
    }

    private fun stripXmlTags(xml: String): String {
        val text = xml.replace(Regex("<[^>]+>"), " ")
        val cleaned = Regex("\\s+").replace(text, " ")
        return cleaned.trim()
    }

    fun convertPdfToMarkdown(uri: Uri, output: File): Boolean {
        return try {
            val inputStream = globalClass.contentResolver.openInputStream(uri) ?: return false
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()

            val markdown = formatAsMarkdown(text)
            output.writeText(markdown)
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

    fun convertPdfToOdf(uri: Uri, output: File): Boolean {
        return try {
            val inputStream = globalClass.contentResolver.openInputStream(uri) ?: return false
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()

            createOdfFromText(text, output)
            true
        } catch (e: Exception) {
            logger.logError(e)
            false
        }
    }

    private fun createOdfFromText(text: String, output: File) {
        val escapedText = escapeXml(text)

        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlnsoffice:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2">
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

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun createPdfFromText(text: String, output: File) {
        val document = PDDocument()
        var page = PDPage(PDRectangle.A4)
        document.addPage(page)

        var contentStream = PDPageContentStream(document, page)
        contentStream.beginText()
        contentStream.setFont(PDType1Font.HELVETICA, 10f)
        contentStream.newLineAtOffset(50f, 750f)

        val lines = text.split("\n")
        var yPos = 750f
        val lineHeight = 12f

        for (line in lines) {
            if (yPos < 50f) {
                contentStream.endText()
                contentStream.close()
                page = PDPage(PDRectangle.A4)
                document.addPage(page)
                contentStream = PDPageContentStream(document, page)
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 10f)
                contentStream.newLineAtOffset(50f, 750f)
                yPos = 750f
            }
            contentStream.showText(line)
            contentStream.newLineAtOffset(0f, -lineHeight)
            yPos -= lineHeight
        }

        contentStream.endText()
        contentStream.close()
        document.save(FileOutputStream(output))
        document.close()
    }
}
