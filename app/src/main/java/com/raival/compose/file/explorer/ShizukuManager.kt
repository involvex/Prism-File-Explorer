package com.raival.compose.file.explorer

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.raival.compose.file.explorer.App.Companion.appContext
import com.raival.compose.file.explorer.App.Companion.globalClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

class ShizukuManager {
    companion object {
        private const val TAG = "ShizukuManager"
        private const val PERMISSION_REQUEST_CODE = 9001
    }

    private val _shizukuAvailable = MutableStateFlow(false)
    val shizukuAvailable: StateFlow<Boolean> = _shizukuAvailable

    private val _shizukuGranted = MutableStateFlow(false)
    val shizukuGranted: StateFlow<Boolean> = _shizukuGranted

    private val _shizukuEnabled = MutableStateFlow(false)
    val shizukuEnabled: StateFlow<Boolean> = _shizukuEnabled

    private val _isRoot = MutableStateFlow(false)
    val isRoot: StateFlow<Boolean> = _isRoot

    private val _isShell = MutableStateFlow(false)
    val isShell: StateFlow<Boolean> = _isShell

    private var shellBinder: ShizukuShellService.ShellBinder? = null
    private var serviceBound = false
    private val serviceLock = java.util.concurrent.locks.ReentrantLock()
    private val serviceCondition = serviceLock.newCondition()
    private var serviceReady = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        refreshAvailability()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        _shizukuAvailable.value = false
        _shizukuGranted.value = false
        _isRoot.value = false
        _isShell.value = false
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shellBinder = service as? ShizukuShellService.ShellBinder
            Log.d(TAG, "ShizukuShellService connected")
            serviceLock.lock()
            try {
                serviceReady = true
                serviceCondition.signalAll()
            } finally {
                serviceLock.unlock()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellBinder = null
            serviceLock.lock()
            try {
                serviceReady = false
            } finally {
                serviceLock.unlock()
            }
            Log.d(TAG, "ShizukuShellService disconnected")
        }
    }

    init {
        try {
            _shizukuEnabled.value = globalClass.preferencesManager.shizukuEnabled
        } catch (_: Exception) {
            _shizukuEnabled.value = false
        }
        checkAvailability()
        checkPermissionState()
        registerListeners()
    }

    private fun checkAvailability() {
        _shizukuAvailable.value = try {
            Shizuku.isPreV11().not() && Shizuku.getVersion() != 0
        } catch (_: Exception) {
            false
        }
    }

    fun refreshAvailability() {
        checkAvailability()
        if (_shizukuAvailable.value) {
            checkPermissionState()
        } else {
            _shizukuGranted.value = false
            _isRoot.value = false
            _isShell.value = false
        }
    }

    private fun checkPermissionState() {
        if (!_shizukuAvailable.value) return
        try {
            _shizukuGranted.value = Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED
            updateShellStatus()
        } catch (_: Exception) { }
    }

    private fun updateShellStatus() {
        if (!_shizukuGranted.value) {
            _isRoot.value = false
            _isShell.value = false
            return
        }
        try {
            val uid = Shizuku.getUid()
            _isRoot.value = uid == 0
            _isShell.value = uid == 2000
        } catch (_: Exception) { }
    }

    fun registerListeners() {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (_: Exception) { }
    }

    fun unregisterListeners() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (_: Exception) { }
    }

    fun requestPermission() {
        if (!_shizukuAvailable.value) return
        if (_shizukuGranted.value) return
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (_: Exception) { }
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != PERMISSION_REQUEST_CODE) return
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _shizukuGranted.value = granted
        updateShellStatus()
        if (granted) {
            _shizukuEnabled.value = true
            globalClass.preferencesManager.shizukuEnabled = true
            bindShellService()
        } else {
            _shizukuEnabled.value = false
            globalClass.preferencesManager.shizukuEnabled = false
            unbindShellService()
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        if (enabled) {
            if (_shizukuGranted.value) {
                _shizukuEnabled.value = true
                globalClass.preferencesManager.shizukuEnabled = true
                bindShellService()
            } else {
                requestPermission()
            }
        } else {
            _shizukuEnabled.value = false
            _shizukuGranted.value = false
            globalClass.preferencesManager.shizukuEnabled = false
            unbindShellService()
        }
    }

    fun bindShellService() {
        if (serviceBound || !_shizukuEnabled.value) return
        serviceLock.lock()
        try {
            serviceReady = false
        } finally {
            serviceLock.unlock()
        }
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(appContext, ShizukuShellService::class.java)
            )
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(false)
                .version(1)

            Shizuku.bindUserService(args, serviceConnection)
            serviceBound = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind ShizukuShellService", e)
        }
    }

    fun unbindShellService() {
        if (!serviceBound) return
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(appContext, ShizukuShellService::class.java)
            )
                .daemon(false)
                .processNameSuffix("shell")
                .version(1)
            Shizuku.unbindUserService(args, serviceConnection, true)
        } catch (_: Exception) { }
        shellBinder = null
        serviceBound = false
    }

    fun getUriForFile(file: java.io.File): Uri? {
        if (!_shizukuEnabled.value) return null
        return try {
            androidx.core.content.FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get URI for file", e)
            null
        }
    }

    fun getShellUid(): Int {
        return if (_shizukuEnabled.value) {
            try { Shizuku.getUid() } catch (_: Exception) { -1 }
        } else -1
    }

    fun isAccessible(): Boolean = _shizukuEnabled.value

    fun runShellCommand(command: String): String? {
        if (!_shizukuEnabled.value) return null
        return try {
            val binder = shellBinder
            if (binder != null) {
                binder.execute(command)
            } else {
                bindShellService()
                serviceLock.lock()
                try {
                    if (!serviceReady) {
                        serviceCondition.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    }
                } finally {
                    serviceLock.unlock()
                }
                shellBinder?.execute(command)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run shell command", e)
            null
        }
    }

    fun newProcess(): android.os.Process? = null
}
