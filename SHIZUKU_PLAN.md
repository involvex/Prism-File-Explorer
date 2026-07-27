# Shizuku Integration Plan for Prism File Explorer

## Overview

Integrate Shizuku (RikkaApps/Shizuku) to enable non-root access to restricted directories like `/sdcard/Android/data`, `/data/data`, and other paths that are normally inaccessible without root privileges. Shizuku allows the app to execute elevated-privilege shell commands or use Shizuku's `FileProvider` to access files that standard Android APIs cannot reach.

**Why this matters**: On Android 11+, apps cannot browse `/sdcard/Android/data` or `/data/data` without special permissions (MANAGE_EXTERNAL_STORAGE with limitations, or root). Shizuku bridges this gap by providing a shell user (shell uid 2000) with elevated file access, enabling the file explorer to work on non-rooted devices.

---

## Phase 1: Dependency & Configuration

### 1.1 Add Shizuku dependency to version catalog

**File**: `gradle/libs.versions.toml`

- Add a new version entry for Shizuku (v13.x or latest stable)
- Add a new library entry: `shizuku = { module = "dev.rikka.shizuku:shizuku", version.ref = "shizuku" }`
- Note: Shizuku is already available on Maven Central since v13, so no JitPack-specific config is needed (JitPack is already configured in settings.gradle.kts as a fallback)

### 1.2 Add Shizuku dependency to app build.gradle.kts

**File**: `app/build.gradle.kts`

- Add `implementation(libs.shizuku)` in the dependencies block

### 1.3 Update AndroidManifest.xml

**File**: `app/src/main/AndroidManifest.xml`

Add `<queries>` for Shizuku so the app can detect if Shizuku is installed:
```xml
<queries>
    <package android:name="dev.rikka.shizuku" />
</queries>
```

Add optional permissions if using Shizuku's shell process:
- No extra permissions needed for Shizuku's FileProvider-based access
- Shizuku handles its own permission flow internally

### 1.4 Add strings for Shizuku UI

**File**: `app/src/main/res/values/strings.xml`

Add new string keys (follow existing snake_case pattern):
- `enable_shizuku_access` - "Enable Shizuku access"
- `shizuku_access_description` - "Grant Shizuku access to browse restricted directories like Android/data"
- `shizuku_not_installed` - "Shizuku is not installed"
- `shizuku_install_prompt` - "Install Shizuku from GitHub to access restricted directories"
- `shizuku_grant_permission` - "Grant Shizuku Permission"
- `shizuku_permission_required` - "Shizuku permission is required"
- `shizuku_permission_denied` - "Shizuku permission was denied"
- `shizuku_v11_required` - "Shizuku v11+ is required"
- `browse_android_data` - "Browse Android/data"
- `browse_data_data` - "Browse data/data"
- `shizuku_enabled` - "Shizuku access enabled"
- `shizuku_disabled` - "Shizuku access disabled"

---

## Phase 2: Shizuku Manager (App-level singleton)

### 2.1 Create ShizukuManager class

**New file**: `app/src/main/java/com/raival/compose/file/explorer/ShizukuManager.kt`

Follows the same `by lazy` singleton pattern as other managers in `App.kt`.

Responsibilities:
- Check Shizuku availability (`Shizuku.isPreV11()` for API level check)
- Check if Shizuku permission is granted (`Shizuku.checkSelfPermission()`)
- Request Shizuku permission (`Shizuku.requestPermission()`)
- Add permission result listener (`Shizuku.addRequestPermissionResultListener()`)
- Check if current shell user is root (`Shizuku.getUid() == 0`)
- Check if running as shell (`Shizuku.getUid() == 2000`)
- Provide `isShizukuAvailable()` and `isShizukuGranted()` state
- Use `Shizuku.FileProvider` for URI-based file access to restricted paths
- Expose `isEnabled` (`MutableStateFlow<Boolean>`) for UI observation
- Use `applicationScope` from App for coroutine dispatchers

Key API usage:
```kotlin
// Check availability
if (!Shizuku.isPreV11()) { /* show dialog to update Shizuku */ }

// Check permission
val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

// Request permission
Shizuku.requestPermission(requestCode)

// Listen for results
Shizuku.addRequestPermissionResultListener { _, granted ->
    // update state
}

// Check if running as root/shell
val uid = Shizuku.getUid() // 0 = root, 2000 = shell
val isRoot = uid == 0
val isShell = uid == 2000

// FileProvider for restricted paths
Shizuku.FileProvider.getUriForFile(context, authority, file)
```

### 2.2 Register ShizukuManager in App.kt

**File**: `app/src/main/java/com/raival/compose/file/explorer/App.kt`

Add as a lazy singleton following existing pattern:
```kotlin
val shizukuManager: ShizukuManager by lazy { ShizukuManager() }
```

Initialize Shizuku permission listener in `onCreate()`.

### 2.3 Add preferences for Shizuku state

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/PreferencesManager.kt`

Add new preference keys:
```kotlin
// Shizuku
var shizukuEnabled by prefMutableState(
    keyName = "shizuku_enabled",
    defaultValue = false,
    getPreferencesKey = { booleanPreferencesKey(it) }
)
```

---

## Phase 3: Permission Flow Integration

### 3.1 Add Shizuku permission check to BaseActivity

**File**: `app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt`

Add a new method `checkShizukuPermission()` similar to `checkPermissions()`:
- Check if Shizuku is available (app installed, v11+)
- Check if Shizuku permission is granted
- If not granted, show a dialog/screen explaining Shizuku and prompting the user to grant permission
- Use ActivityResultContracts.StartActivityForResult for the Shizuku permission intent
- On success, update `PreferencesManager.shizukuEnabled = true` and `ShizukuManager` state

Create a new composable `ShizukuPermissionScreen()` in BaseActivity following the same pattern as `StoragePermissionScreen()`:
- Shows icon, title, description
- "Grant Permission" button that launches Shizuku's permission request
- "Skip for now" button

### 3.2 Add Shizuku availability check in MainActivity

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/MainActivity.kt`

In `onCreate()`, after storage permissions are checked, also check Shizuku availability. This is a non-blocking check - Shizuku is optional, not required for the app to function.

### 3.3 Add Shizuku state to MainActivityState

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/MainActivityState.kt`

Add `shizukuAvailable: Boolean` and `shizukuGranted: Boolean` fields to track Shizuku state for UI decisions.

---

## Phase 4: Storage Access with Shizuku

### 4.1 Create Shizuku-aware ContentHolder

**New file**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/holder/ShizukuFileHolder.kt`

Extends `ContentHolder` (abstract base class) or wraps `LocalFileHolder` for files accessible via Shizuku's FileProvider.

Key differences from `LocalFileHolder`:
- `canRead` and `canWrite` use Shizuku's FileProvider URI permissions instead of raw `File.canRead()`/`File.canWrite()`
- `listContent()` uses Shizuku shell commands or FileProvider URIs to enumerate directory contents
- `open()` uses ContentResolver with Shizuku-provided URIs instead of direct file paths
- `listContent()` for restricted paths (`/data/data`, `/sdcard/Android/data`) uses `Shizuku.newProcess()` to run `ls` or `find` commands
- `readText()`/`writeText()` use Shizuku's shell commands (`cat`, `cp`, `tee`) for file content operations
- `delete()` uses Shizuku shell `rm -rf` command
- `createSubFile()` / `createSubFolder()` use Shizuku shell `touch` / `mkdir -p` commands

### 4.2 Update StorageProvider to expose Shizuku paths

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/provider/StorageProvider.kt`

Add a new method `getShizukuStorageDevices(context, shizukuManager)`:
- Detect accessible restricted paths when Shizuku is granted:
  - `/sdcard/Android/data` - app-specific data for all apps
  - `/data/data` - app internal data directories
  - `/sdcard/Android/obb` - OBB files for apps
- Return these as `StorageDevice` instances with `ShizukuFileHolder` as the content holder
- If Shizuku is not granted or not available, these paths are not shown (or shown as inaccessible)

Update `getStorageDevices()` to optionally include Shizuke-enabled devices when the user has Shizuku enabled.

### 4.3 Update StorageDeviceType enum

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/misc/Constant.kt`

Add new storage device type:
```kotlin
StorageDeviceType.SHIZUKU = 3
```

### 4.4 Update RootFileHolder to use Shizuku when available

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/holder/RootFileHolder.kt`

When Shizuku is granted, modify `listContent()` to:
- Use `Shizuku.newProcess()` to run `ls -la` on restricted directories
- Return `ShizukuFileHolder` instances for files in restrictive paths
- Keep `LocalFileHolder` for paths that are already accessible via standard `File` API
- This enables browsing `/data` and `/sdcard/Android/data` contents even from the Root virtual device

---

## Phase 5: File Operation Task Support

### 5.1 Update CopyTask to handle Shizuku paths

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/CopyTask.kt`

In `copyLocalFile()`, detect when source or destination is a `ShizukuFileHolder`:
- Use Shizuku shell commands for copying (`cp -r` for directories, `cp` for files)
- Provide progress reporting during copy via Shizuku's `cat` piped to destination
- Handle conflict resolution the same way as `LocalFileHolder` copies

### 5.2 Update DeleteTask to handle Shizuku paths

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/DeleteTask.kt`

In `handleLocalFileDeletion()`, detect `ShizukuFileHolder`:
- Use Shizuku shell `rm -rf` command for deletion
- Report progress per file

### 5.3 Update RenameTask to handle Shizuku paths

**New file or update**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/RenameTask.kt`

In rename handling, detect `ShizukuFileHolder`:
- Use Shizuku shell `mv` command for renaming

### 5.4 Update CompressTask to handle Shizuku paths

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/CompressTask.kt`

In compression handling, detect `ShizukuFileHolder`:
- Use Shizuku shell `zip` command or read files via Shizuku and add to zip normally

---

## Phase 6: UI Integration

### 6.1 Add Shizuku preference to Preferences screen

**New file**: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/ShizukuContainer.kt`

Follows the same pattern as `FileOperationContaner.kt`:
- Uses `Container` composable with title "Shizuku"
- Shows a `PreferenceItem` with a toggle switch for `preferences.shizukuEnabled`
- When enabled, triggers the Shizuku permission flow
- Shows status text (granted/denied/not installed)

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/PreferencesScreen.kt` (or wherever the preferences screen composes its containers)

Add `ShizukuContainer()` to the preference screen composition.

### 6.2 Update PreferenceItem to support Shizuku preference types

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/ui/PreferenceItem.kt`

No changes needed if the existing switch pattern is used, but may need to add a "click to grant" action type for the Shizuku toggle (tapping opens Shizuku permission request).

### 6.3 Update BottomOptionsBar or File Action Menu

When Shizuku is enabled and user navigates to a restricted directory, show appropriate file operation options (copy, delete, rename) for `ShizukuFileHolder` instances.

### 6.4 Update FilesTabContentView dialogs

**File**: `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/ui/FilesTabContentView.kt`

If Shizuku permission is needed but not granted, show a dialog prompting the user to enable Shizuku access when they try to navigate to a restricted path.

---

## Phase 7: Edge Cases & Error Handling

### 7.1 Shizuku not installed

- When user enables Shizuku in preferences but doesn't have it installed, show a dialog with:
  - "Shizuku is not installed" message
  - Link to install from GitHub (https://github.com/RikkaApps/Shizuku)
  - "Install" button that opens the Shizuku GitHub release page
  - "Skip" button to dismiss

### 7.2 Shizuku permission denied

- If user denies Shizuku permission, show a toast explaining that restricted directory access is disabled
- Disable Shizuku paths in StorageProvider
- Allow the user to re-enable from preferences

### 7.3 Shizuku process dies or becomes unavailable

- Register `Shizuku.addBinderDeadReceiver()` to detect when Shizuku service dies
- When Shizuku becomes unavailable, fall back to standard file access (read-only for accessible paths)
- Show a toast: "Shizuku connection lost, some features may be unavailable"

### 7.4 File operations failing via Shizuku

- Wrap Shizuku shell command execution in try-catch blocks
- On failure, fall back to showing "Unable to access file" error
- In task system, mark task as FAILED with appropriate error message

### 7.5 Performance considerations

- Shizuku shell commands are slower than direct file I/O
- For bulk operations (copy, delete many files), batch commands where possible
- Use `Shizuku.newProcess()` with a persistent shell session to avoid repeated process creation overhead
- Show progress appropriately for each file operation

---

## Phase 8: ProGuard & Testing

### 8.1 ProGuard rules

**File**: `app/proguard-rules.pro`

Shizuku's library classes are automatically kept by the project's existing `-keep class com.raival.compose.file.explorer.** { *; }` rule. However, if Shizuku uses reflection or specific classes, may need to add:
```proguard
-keep class dev.rikka.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
```

### 8.2 Unit Tests

- Test `ShizukuManager.isShizukuAvailable()`
- Test `ShizukuManager.isShizukuGranted()`
- Test `ShizukuFileHolder.listContent()` for restricted paths
- Test that `StorageProvider.getShizukuStorageDevices()` returns correct devices when Shizuku is enabled

### 8.3 Integration Tests

- Test full Shizuku permission flow on an emulated device
- Test file browsing of `/sdcard/Android/data` with Shizuku granted
- Test copy/delete/rename operations on ShizukuFileHolder instances
- Test fallback behavior when Shizuku is not installed or permission is denied

---

## File Change Summary

### Modified Files
| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add Shizuku version and library entry |
| `app/build.gradle.kts` | Add Shizuku dependency |
| `app/src/main/AndroidManifest.xml` | Add `<queries>` for Shizuku package |
| `app/src/main/java/com/raival/compose/file/explorer/App.kt` | Add `shizukuManager` lazy singleton |
| `app/src/main/java/com/raival/compose/file/explorer/base/BaseActivity.kt` | Add Shizuku permission check flow |
| `app/src/main/java/com/raival/compose/file/explorer/screen/preferences/PreferencesManager.kt` | Add `shizukuEnabled` preference |
| `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/provider/StorageProvider.kt` | Add `getShizukuStorageDevices()` |
| `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/holder/RootFileHolder.kt` | Integrate Shizuku for restricted path listing |
| `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/CopyTask.kt` | Handle ShizukuFileHolder copy |
| `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/task/DeleteTask.kt` | Handle ShizukuFileHolder delete |
| `app/src/main/java/com/raival/compose/file/explorer/screen/main/tab/files/misc/Constant.kt` | Add `SHIZUKU` storage device type |
| `app/src/main/res/values/strings.xml` | Add Shizuku-related strings |

### New Files
| File | Purpose |
|------|---------|
| `ShizukuManager.kt` | App-level Shizuku state management |
| `ShizukuFileHolder.kt` | ContentHolder implementation for Shizuku-accessible files |
| `ShizukuContainer.kt` | Preferences UI for Shizuku settings |

---

## Execution Order

1. Phase 1 (Dependency & Config) - 1 task
2. Phase 2 (Shizuku Manager) - 1 task
3. Phase 3 (Permission Flow) - 1 task
4. Phase 4 (Storage Access) - 1 task
5. Phase 5 (File Ops Support) - 1 task
6. Phase 6 (UI Integration) - 1 task
7. Phase 7 (Edge Cases) - 1 task
8. Phase 8 (Testing) - 1 task

Each phase can be executed independently as long as the previous phase's changes are complete, since later phases depend on earlier structural additions.
