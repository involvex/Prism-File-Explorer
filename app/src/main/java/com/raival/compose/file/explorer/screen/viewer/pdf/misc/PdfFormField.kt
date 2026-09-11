package com.raival.compose.file.explorer.screen.viewer.pdf.misc

import android.graphics.RectF

data class PdfFormField(
    val fullyQualifiedName: String,
    val displayLabel: String,
    val fieldType: FieldType,
    val value: String,
    val pageIndex: Int,
    val rect: RectF,
    val options: List<String> = emptyList(),
) {
    enum class FieldType {
        TEXT,
        CHECKBOX,
        RADIO_BUTTON,
        LIST,
        COMBO_BOX,
        SIGNATURE,
        UNKNOWN
    }
}
