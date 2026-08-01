# Task 2.1: Add lifecycle listeners to ShizukuManager

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/ShizukuManager.kt`

## What to do

Add a public `refreshAvailability()` method and Shizuku binder lifecycle listeners so the app auto-detects when Shizuku becomes available or dies.

### 1. Add `refreshAvailability()` method

Add after the `checkAvailability()` method (after line 83):

```kotlin
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
```

### 2. Add Shizuku lifecycle listener fields

Add as class fields (e.g., after `serviceReady` field, around line 40):

```kotlin
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
```

### 3. Add `registerListeners()` and `unregisterListeners()` methods

```kotlin
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
```

### 4. Call `registerListeners()` in `init` block

At the end of the `init` block (after `checkPermissionState()` on line 74), add:

```kotlin
registerListeners()
```

## Context

- `ShizukuManager` is a class in `com.raival.compose.file.explorer`
- It imports `rikka.shizuku.Shizuku`
- The `Log` class is already imported (`android.util.Log`)
- The Shizuku API provides `addBinderReceivedListener`, `removeBinderReceivedListener`, `addBinderDeadListener`, `removeBinderDeadListener`
- These listeners fire when the Shizuku service connects/disconnects, allowing us to auto-detect availability changes

## Verification
- The project should compile without errors after these changes.
