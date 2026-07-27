package com.raival.compose.file.explorer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuShellService : Service() {

    private val suPaths = arrayOf(
        "/system/xbin/su",
        "/system/bin/su",
        "/vendor/xbin/su",
        "/sbin/su",
        "/su/bin/su"
    )

    private fun findSu(): String? {
        for (path in suPaths) {
            val file = java.io.File(path)
            if (file.exists() && file.canExecute()) return path
        }
        return null
    }

    inner class ShellBinder : Binder() {
        fun execute(command: String): String {
            return try {
                val suBinary = findSu()
                val process = if (suBinary != null) {
                    Log.d(TAG, "Using su binary: $suBinary")
                    Runtime.getRuntime().exec(arrayOf(suBinary, "-c", command))
                } else {
                    Log.d(TAG, "No su found, using sh directly")
                    Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", command))
                }
                val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
                val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
                val exitCode = process.waitFor()
                Log.d(TAG, "Command exit code: $exitCode, output: $output, error: $error")
                if (error.isNotBlank() && (error.contains("denied") || error.contains("permission") || error.contains("not allowed"))) error else output.ifBlank { error }
            } catch (e: Exception) {
                Log.e(TAG, "Shell execution failed: ${e.message}")
                "ERROR: ${e.message}"
            }
        }
    }

    private val binder = ShellBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TAG = "ShizukuShellService"
    }
}
