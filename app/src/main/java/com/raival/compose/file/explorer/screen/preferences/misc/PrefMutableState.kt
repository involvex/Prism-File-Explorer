package com.raival.compose.file.explorer.screen.preferences.misc

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.raival.compose.file.explorer.App
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "PrefSnapshot"

/**
 * Single in-memory snapshot of the preferences DataStore.
 *
 * Reads are served from a cached snapshot that is kept fresh by a single
 * DataStore collector, so neither the first [flow] collection nor individual
 * property reads block the caller. Writes update the snapshot optimistically
 * and are persisted on a single IO worker, so preference changes no longer
 * perform synchronous disk I/O on the main thread.
 */
object PrefSnapshot {
    private val lock = Any()

    @Volatile
    private var cached: Preferences? = null

    private val stateFlow = MutableStateFlow<Preferences>(emptyPreferences())

    /** Reactive view of all preferences; collect with collectAsState in Compose. */
    val flow: StateFlow<Preferences> get() = stateFlow

    private data class Write(
        val transform: (MutablePreferences) -> Unit,
        val completion: CompletableDeferred<Unit>?
    )

    private val writeChannel = Channel<Write>(Channel.UNLIMITED)

    init {
        launchDataStoreCollector(App.globalClass.applicationScope)
        launchWriteWorker(App.globalClass.applicationScope)
    }

    private fun launchDataStoreCollector(scope: CoroutineScope) {
        scope.launch {
            App.globalClass.prefDataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { prefs ->
                    synchronized(lock) {
                        cached = prefs
                        stateFlow.value = prefs
                    }
                }
        }
    }

    private fun launchWriteWorker(scope: CoroutineScope) {
        scope.launch {
            for (write in writeChannel) {
                try {
                    val updated = App.globalClass.prefDataStore.edit(write.transform)
                    synchronized(lock) {
                        cached = updated
                        stateFlow.value = updated
                    }
                    write.completion?.complete(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist preferences", e)
                    // Roll back the optimistic snapshot to the persisted value.
                    runCatching { App.globalClass.prefDataStore.data.first() }
                        .getOrNull()?.let { fresh ->
                            synchronized(lock) {
                                cached = fresh
                                stateFlow.value = fresh
                            }
                        }
                    write.completion?.completeExceptionally(e)
                }
            }
        }
    }

    fun read(): Preferences {
        cached?.let { return it }
        return synchronized(lock) {
            cached ?: runBlocking { App.globalClass.prefDataStore.data.first() }
                .also {
                    cached = it
                    stateFlow.value = it
                }
        }
    }

    /** Update the in-memory snapshot immediately; persist on the IO worker. */
    fun write(transform: (MutablePreferences) -> Unit) {
        synchronized(lock) {
            val base = cached ?: return@synchronized
            val optimistic = base.toMutablePreferences().apply(transform).toPreferences()
            cached = optimistic
            stateFlow.value = optimistic
        }
        writeChannel.trySend(Write(transform, null))
    }

    /** Wait until all queued writes have been persisted. */
    suspend fun flush() {
        val completion = CompletableDeferred<Unit>()
        writeChannel.send(Write({}, completion))
        completion.await()
    }

    /** Reload the snapshot after an external bulk write (e.g. preference import). */
    suspend fun invalidate() {
        flush()
        val fresh = App.globalClass.prefDataStore.data.first()
        synchronized(lock) {
            cached = fresh
            stateFlow.value = fresh
        }
    }
}

inline fun <reified A> prefMutableState(
    keyName: String,
    defaultValue: A,
    getPreferencesKey: (keyName: String) -> Preferences.Key<A>,
): MutableState<A> {
    val key: Preferences.Key<A> = getPreferencesKey(keyName)
    val snapshotMutableState: MutableState<A> = mutableStateOf(
        PrefSnapshot.read()[key] ?: defaultValue
    )

    return object : MutableState<A> {
        override var value: A
            get() = snapshotMutableState.value
            set(value) {
                val rollbackValue = snapshotMutableState.value
                snapshotMutableState.value = value
                try {
                    PrefSnapshot.write {
                        if (value != null) {
                            it[key] = value as A
                        } else {
                            it.remove(key)
                        }
                    }
                } catch (_: Exception) {
                    snapshotMutableState.value = rollbackValue
                }
            }

        override fun component1() = value
        override fun component2(): (A) -> Unit = { value = it }
    }
}
