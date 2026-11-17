package com.raival.compose.file.explorer.screen.main.tab.files.task

import com.raival.compose.file.explorer.common.emptyString

/**
 * Written by the task coroutine and read by the progress monitor / UI on other
 * threads, so every field must have volatile visibility.
 */
data class TaskProgressMonitor(
    @Volatile var status: TaskStatus = TaskStatus.PENDING, // Pending, Running, Success, Failed, Cancelled
    @Volatile var taskTitle: String = emptyString, // e.g, "Copy", "Move"
    @Volatile var processName: String = emptyString, // e.g, "Copying", "Moving"
    @Volatile var contentName: String = emptyString, // e.g, "File 1", "File 2". can be empty when not processing files
    @Volatile var totalContent: Int = 0, // Total number of content to be processed
    @Volatile var remainingContent: Int = 0, // Remaining number of content to be processed
    @Volatile var progress: Float = -1f, // Progress of the task from 0 to 1
    @Volatile var summary: String = emptyString // Summary of the task after it stops
)
