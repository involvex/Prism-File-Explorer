package com.raival.compose.file.explorer.screen.viewer

import android.net.Uri

class DocumentViewerInstance(
    override val uri: Uri,
    override val id: String
) : ViewerInstance {
    var conversionError: String? = null

    override fun onClose() {}
}
