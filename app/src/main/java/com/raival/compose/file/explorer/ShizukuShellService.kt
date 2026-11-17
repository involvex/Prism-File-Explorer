package com.raival.compose.file.explorer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.concurrent.TimeUnit


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
        /**
         * Returns "<exit code>\n<combined output>". Callers must use the exit code,
         * never string sniffing ("denied", "error", ...) to decide success.
         * stderr is merged into stdout so a chatty command can never fill the
         * stderr pipe while we are still reading stdout (a classic deadlock).
         */
        fun execute(command: String): String {
            var process: Process? = null
            // A watchdog is needed because stream.read() itself can block forever
            // when a command hangs without producing output.
            val watchdog = Thread {
                try {
                    Thread.sleep(COMMAND_TIMEOUT_SECONDS * 1000)
                    process?.takeIf { it.isAlive }?.destroyForcibly()
                } catch (_: InterruptedException) {
                    // command finished normally
                }
            }.apply { isDaemon = true }

            return try {
                val suBinary = findSu()
                val processBuilder = if (suBinary != null) {
                    Log.d(TAG, "Using su binary: $suBinary")
                    ProcessBuilder(suBinary, "-c", command)
                } else {
                    Log.d(TAG, "No su found, using sh directly")
                    ProcessBuilder("/system/bin/sh", "-c", command)
                }
                process = processBuilder.redirectErrorStream(true).start()
                watchdog.start()

                val output = StringBuilder()
                val buffer = ByteArray(8192)
                var total = 0
                process!!.inputStream.use { stream ->
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > MAX_OUTPUT_BYTES) {
                            process!!.destroyForcibly()
                            return "$SHELL_ERROR_CODE\nOutput exceeded $MAX_OUTPUT_BYTES bytes"
                        }
                        output.append(String(buffer, 0, read, Charsets.UTF_8))
                    }
                }
                if (!process!!.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process!!.destroyForcibly()
                    process!!.waitFor(5, TimeUnit.SECONDS)
                    return "$SHELL_ERROR_CODE\nCommand timed out after ${COMMAND_TIMEOUT_SECONDS}s"
                }
                val exitCode = process!!.exitValue()
                Log.d(TAG, "Command exit code: $exitCode, output: $output")
                "$exitCode\n$output"
            } catch (e: Exception) {
                Log.e(TAG, "Shell execution failed: ${e.message}")
                "$SHELL_ERROR_CODE\n${e.message ?: "shell error"}"
            } finally {
                watchdog.interrupt()
                process?.destroy()
            }
        }
    }

    private val binder = ShellBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val TAG = "ShizukuShellService"
        const val SHELL_ERROR_CODE = 999
        private const val COMMAND_TIMEOUT_SECONDS = 60L
        private const val MAX_OUTPUT_BYTES = 1 * 1024 * 1024
    }
}
