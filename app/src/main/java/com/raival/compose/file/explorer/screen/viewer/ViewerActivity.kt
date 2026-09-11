package com.raival.compose.file.explorer.screen.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.base.BaseActivity
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.randomString

abstract class ViewerActivity : BaseActivity() {
    companion object {
        const val EXTRA_PARENT_DIR = "parent_dir"
    }

    private var uri: Uri? = null
    private var uid: String = emptyString
    private var currentInstance: ViewerInstance? = null
    private var parentDir: String? = null

    abstract fun onCreateNewInstance(uri: Uri, uid: String): ViewerInstance
    abstract fun onReady(instance: ViewerInstance, parentDir: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (intent.data == null) {
            finish()
            return
        }

        uri = intent.data
        parentDir = intent.getStringExtra(EXTRA_PARENT_DIR)

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString("uid", String.randomString(12))
        }

        if (uid.isEmpty()) {
            uid = String.randomString(12)
        }

        val instance = globalClass.viewersManager.instances.find { it.id == uid }

        if (instance == null) {
            globalClass.viewersManager.instances.add(
                onCreateNewInstance(uri!!, uid).also { currentInstance = it }
            )
        }

        checkPermissions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("uid", uid)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentInstance?.onClose()
        globalClass.viewersManager.instances.remove(currentInstance)
    }

    override fun onPermissionGranted() {
        onReady(currentInstance!!, parentDir)
    }
}