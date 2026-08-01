# Task 2.2: Register/unregister listeners in BaseActivity

## Files
- Modify: `app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt`

## What to do

Register the Shizuku lifecycle listeners in `onCreate` and unregister them in `onDestroy`.

### 1. Add registration in `onCreate`

In `BaseActivity.onCreate()` (line 64-69), after the existing `Shizuku.addRequestPermissionResultListener` call, add:

```kotlin
        globalClass.shizukuManager.registerListeners()
```

The resulting `onCreate` should look like:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) { }
        globalClass.shizukuManager.registerListeners()
    }
```

### 2. Add unregistration in `onDestroy`

In `BaseActivity.onDestroy()` (line 71-76), after the existing `Shizuku.removeRequestPermissionResultListener` call, add:

```kotlin
        globalClass.shizukuManager.unregisterListeners()
```

The resulting `onDestroy` should look like:

```kotlin
    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) { }
        globalClass.shizukuManager.unregisterListeners()
    }
```

## Context

- `globalClass` is already imported at line 39: `import com.raival.compose.file.explorer.App.Companion.globalClass`
- `ShizukuManager.registerListeners()` and `unregisterListeners()` were added in Task 2.1
- The existing pattern for Shizuku operations in this file uses try/catch with empty catch blocks

## Verification
- The project should compile without errors after these changes.
