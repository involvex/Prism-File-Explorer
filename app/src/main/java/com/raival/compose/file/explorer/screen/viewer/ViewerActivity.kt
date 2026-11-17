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

        // On configuration change the instance already exists in ViewersManager.
        // Assign it to currentInstance in BOTH branches - otherwise onPermissionGranted()
        // would dereference a null currentInstance after a rotation, and onDestroy()
        // would never release the restored instance.
        currentInstance = globalClass.viewersManager.instances.find { it.id == uid }
            ?: onCreateNewInstance(uri!!, uid).also {
                globalClass.viewersManager.instances.add(it)
            }

        checkPermissions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("uid", uid)
    }

    override fun onDestroy() {
        super.onDestroy()
        // A configuration change calls onDestroy on the old Activity while the
        // new one reuses the same instance from ViewersManager. Only release on
        // a real finish (back / task removed).
        if (!isChangingConfigurations) {
            currentInstance?.onClose()
            globalClass.viewersManager.instances.remove(currentInstance)
        }
    }

    /**
     * Replace the instance after an asynchronous preparation step (e.g. Office
     * to PDF conversion). Keeps [currentInstance] and ViewersManager in sync so
     * onDestroy still disposes exactly one owner.
     */
    protected fun replaceInstance(newInstance: ViewerInstance) {
        currentInstance?.let { old ->
            if (old !== newInstance) {
                globalClass.viewersManager.instances.remove(old)
                old.onClose()
            }
        }
        currentInstance = newInstance
        if (globalClass.viewersManager.instances.none { it.id == newInstance.id }) {
            globalClass.viewersManager.instances.add(newInstance)
        }
    }

    override fun onPermissionGranted() {
        onReady(currentInstance!!, parentDir)
    }
}