package com.raival.compose.file.explorer.screen.main.tab

import com.raival.compose.file.explorer.App.Companion.globalClass
import kotlinx.coroutines.launch

abstract class Tab {
    var isCreated = false
    var parentTab: Tab? = null

    abstract val id: Int
    abstract val header: String

    open fun onTabRemoved() {}
    open fun onTabStopped() {}
    open fun onTabResumed() {}
    open fun onTabStarted() {
        isCreated = true
    }

    abstract suspend fun getTitle(): String
    abstract suspend fun getSubtitle(): String

    open fun onBackPressed(): Boolean = false

    fun requestHomeToolbarUpdate() {
        if (globalClass.mainActivityManager.getActiveTab() == this) {
            // Use the application scope instead of spawning an unmanaged scope
            // on every selection / reload event.
            globalClass.applicationScope.launch {
                globalClass.mainActivityManager.updateHomeToolbar(
                    title = getTitle(),
                    subtitle = getSubtitle()
                )
            }
        }
    }
}