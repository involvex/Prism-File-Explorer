package com.raival.compose.file.explorer.screen.main.tab.files.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.raival.compose.file.explorer.App.Companion.globalClass
import com.raival.compose.file.explorer.R
import com.raival.compose.file.explorer.common.emptyString
import com.raival.compose.file.explorer.common.isNot
import com.raival.compose.file.explorer.common.showMsg
import com.raival.compose.file.explorer.screen.main.tab.files.service.ContentOperationService.Companion.startNewBackgroundTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TaskManager {
    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Atomic check-and-set: two bringToForeground() calls must never both start
    // a monitor, and a non-volatile boolean gave no visibility guarantee.
    private val monitoring = AtomicBoolean(false)

    private val allTasks = ConcurrentHashMap<String, Task>()

    val pendingTasks: MutableCollection<Task> = Collections.synchronizedList(mutableListOf<Task>())
    val runningTasks: MutableCollection<Task> = Collections.synchronizedList(mutableListOf<Task>())
    val pausedTasks: MutableCollection<Task> = Collections.synchronizedList(mutableListOf<Task>())
    val failedTasks: MutableCollection<Task> = Collections.synchronizedList(mutableListOf<Task>())
    val invalidTasks: MutableCollection<Task> = Collections.synchronizedList(mutableListOf<Task>())
    val completedTasks: MutableCollection<Task> =
        Collections.synchronizedList(mutableListOf<Task>())

    @Volatile
    var runningTaskDialogInfo = RunningTaskDialogInfo()
    val taskInterceptor = TaskConflict()

    fun addTask(
        task: Task,
        notifyUser: Boolean = true
    ) {
        allTasks[task.id] = task
        pendingTasks.add(task)
        if (notifyUser) globalClass.showMsg(globalClass.resources.getString(R.string.new_task_has_been_added))
    }

    fun addTaskAndRun(task: Task, parameters: TaskParameters) {
        addTask(task, false)
        runTask(task.id, parameters)
    }

    fun removeTask(id: String) {
        allTasks[id]?.abortTask()
        allTasks.remove(id)

        pendingTasks.removeIf { it.id == id }
        runningTasks.removeIf { it.id == id }
        pausedTasks.removeIf { it.id == id }
        failedTasks.removeIf { it.id == id }
        invalidTasks.removeIf { it.id == id }
        completedTasks.removeIf { it.id == id }
    }

    suspend fun validateTasks(onFinished: (() -> Unit)? = null) {
        // Collections.synchronizedList does NOT make iteration safe on its own,
        // and validate() is a suspend call so it cannot run inside the lock.
        // Snapshot under the lock, validate outside, then remove by identity.
        val candidates = synchronized(pendingTasks) { pendingTasks.toList() }
        val invalid = candidates.filterNot { it.validate() }
        if (invalid.isNotEmpty()) {
            val invalidIds = invalid.map { it.id }.toSet()
            synchronized(pendingTasks) {
                pendingTasks.removeAll { it.id in invalidIds }
            }
            invalidTasks.addAll(invalid)
        }
        onFinished?.invoke()
    }

    fun runTask(id: String, taskParameters: TaskParameters) {
        val task = allTasks[id]

        if (task == null) {
            showMsg(R.string.task_not_found)
            return
        }

        if (task.getCurrentStatus() isNot TaskStatus.PENDING) {
            showMsg(R.string.task_is_not_in_pending_state)
            return
        }

        moveTaskToRunning(task)
        task.setParameters(taskParameters)
        bringToForeground(task)
        startNewBackgroundTask(globalClass, task.id)
    }

    fun continueTask(taskId: String) {
        val task = allTasks[taskId]
        if (task == null) {
            showMsg(R.string.task_not_found)
            return
        }

        val currentStatus = task.getCurrentStatus()

        if (currentStatus isNot TaskStatus.CONFLICT
            && currentStatus isNot TaskStatus.FAILED
            && currentStatus isNot TaskStatus.PAUSED
        ) {
            showMsg(
                globalClass.getString(
                    R.string.task_cannot_be_continued_from_current_state,
                    currentStatus
                )
            )
            return
        }

        // Move task back to running
        pausedTasks.removeIf { it.id == taskId }
        failedTasks.removeIf { it.id == taskId }

        if (!runningTasks.any { it.id == taskId }) {
            runningTasks.add(task)
        }

        bringToForeground(task)
        startNewBackgroundTask(globalClass, task.id)
    }

    private fun moveTaskToRunning(task: Task) {
        pendingTasks.removeIf { it.id == task.id }
        pausedTasks.removeIf { it.id == task.id }
        failedTasks.removeIf { it.id == task.id }

        if (!runningTasks.any { it.id == task.id }) {
            runningTasks.add(task)
        }
    }

    fun handleTaskStatusChange(task: Task, newStatus: TaskStatus) {
        when (newStatus) {
            TaskStatus.SUCCESS -> {
                runningTasks.removeIf { it.id == task.id }
                completedTasks.add(task)
                showMsg(R.string.task_completed)
            }

            TaskStatus.FAILED -> {
                runningTasks.removeIf { it.id == task.id }
                failedTasks.add(task)
                showMsg(R.string.task_failed)
            }

            TaskStatus.PAUSED -> {
                runningTasks.removeIf { it.id == task.id }
                pausedTasks.add(task)
                showMsg(R.string.task_paused)
            }

            TaskStatus.CONFLICT -> {
                runningTasks.removeIf { it.id == task.id }
                pausedTasks.add(task)
                // Conflict will be handled by the UI
            }

            else -> {
                // No action needed for other states
            }
        }
    }

    fun getTask(id: String): Task? = allTasks[id]

    fun overrideConflicts(taskId: String, resolution: TaskContentStatus) {
        allTasks[taskId]?.overrideConflicts(resolution)
    }

    fun bringToForeground(task: Task) {
        // Always point the dialog at the newest task, even when a monitor is
        // already running (the service supports multiple concurrent tasks).
        runningTaskDialogInfo.showOrSwitch(task)
        if (monitoring.compareAndSet(false, true)) {
            taskScope.launch { monitorRunningTasks() }
        }
    }

    private fun isRunning(task: Task): Boolean = synchronized(runningTasks) {
        runningTasks.contains(task)
    }

    private suspend fun monitorRunningTasks() {
        try {
            while (true) {
                // The user may have dismissed the dialog while the task continues.
                if (!runningTaskDialogInfo.showDialog) break

                val current = runningTaskDialogInfo.linkedTask
                if (current != null && isRunning(current)) {
                    runningTaskDialogInfo.updateInfo(current.progressMonitor)
                } else {
                    // Current task finished; follow another one if any is left.
                    val next = synchronized(runningTasks) { runningTasks.firstOrNull() }
                    if (next != null) runningTaskDialogInfo.showOrSwitch(next) else break
                }
                delay(100)
            }
        } finally {
            monitoring.set(false)
            // Don't hide a dialog that now belongs to a different task.
            if (synchronized(runningTasks) { runningTasks.isEmpty() }) {
                runningTaskDialogInfo.hide()
            }
        }
    }

    fun hideRunningTaskDialog() {
        runningTaskDialogInfo.hide()
    }

    class TaskConflict {
        var hasConflict by mutableStateOf(false)
            private set
        var message by mutableStateOf(emptyString)
            private set
        var taskContentItem by mutableStateOf<TaskContentItem?>(null)
            private set
        var task by mutableStateOf<Task?>(null)
            private set

        fun interceptTask(taskContentItem: TaskContentItem, task: Task) {
            this.hasConflict = true
            this.message = globalClass.resources.getString(
                R.string.file_already_exists,
                taskContentItem.content.displayName
            )
            this.taskContentItem = taskContentItem
            this.task = task
        }

        private fun reset() {
            hasConflict = false
            task = null
            message = emptyString
            taskContentItem = null
        }

        fun hide() {
            reset()
        }

        fun resolve(resolution: TaskContentStatus, applyToAllConflicts: Boolean = false) {
            val currentTask = task
            val currentItem = taskContentItem

            if (currentTask == null || currentItem == null) return

            currentItem.status = resolution

            if (applyToAllConflicts) {
                globalClass.taskManager.overrideConflicts(currentTask.id, resolution)
            }

            // Reset before continuing to avoid race conditions
            hide()

            // Continue the task
            globalClass.taskManager.continueTask(currentTask.id)
        }
    }

    class RunningTaskDialogInfo {
        var linkedTask by mutableStateOf<Task?>(null)
        var showDialog by mutableStateOf(false)
            private set
        var progressMonitor by mutableStateOf<TaskProgressMonitor?>(null)

        fun show(task: Task) {
            if (showDialog) return

            linkedTask = task
            showDialog = true
        }

        /** Switch the visible task without tearing down an active monitor. */
        fun showOrSwitch(task: Task) {
            linkedTask = task
            progressMonitor = null
            showDialog = true
        }

        fun hide() {
            if (!showDialog) return

            showDialog = false
            reset()
        }

        private fun reset() {
            linkedTask = null
            progressMonitor = null
        }

        fun updateInfo(progressMonitor: TaskProgressMonitor) {
            this.progressMonitor = TaskProgressMonitor(
                status = progressMonitor.status,
                taskTitle = progressMonitor.taskTitle,
                processName = progressMonitor.processName,
                contentName = progressMonitor.contentName,
                totalContent = progressMonitor.totalContent,
                remainingContent = progressMonitor.remainingContent,
                progress = progressMonitor.progress,
                summary = progressMonitor.summary
            )
        }
    }
}