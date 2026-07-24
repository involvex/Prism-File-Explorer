# Prism File Explorer — Agent Instructions

> **Project**: A modern, feature-rich Android file manager built entirely with Kotlin and Jetpack Compose.
> **Package**: `com.raival.compose.file.explorer`
> **Current Version**: 1.3.2 (10)

---

## Useful Commands

### Build & Run
| Command | Description |
|---------|-------------|
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleDebugCoverage` | Build debug with coverage |
| `./gradlew installDebug` | Install debug APK on connected device |
| `./gradlew connectedDebugAndroidTest` | Run instrumentation tests on connected device |
| `./gradlew test` | Run unit tests |
| `./gradlew lint` | Run lint checks |
| `./gradlew baselineprofile:generateReleaseBaselineProfile` | Generate baseline profile for release |

### Clean
| Command | Description |
|---------|-------------|
| `./gradlew clean` | Clean build outputs |

### Dependencies
| Command | Description |
|---------|-------------|
| `./gradlew dependencies` | Print all dependencies |
| `./gradlew dependencyUpdates` | Check for dependency updates |

### Key Build Files
| File | Purpose |
|------|---------|
| `build.gradle.kts` | Top-level build config, plugin declarations |
| `app/build.gradle.kts` | App module config, dependencies, signing |
| `gradle/libs.versions.toml` | Centralized version catalog for all deps |
| `gradle.properties` | Project-wide Gradle settings (JVM args, AndroidX flags) |
| `app/proguard-rules.pro` | ProGuard/R8 rules for release builds |

### CI/CD
| Command | Description |
|---------|-------------|
| GitHub Actions workflow `.github/workflows/android.yml` | Builds release APK on push/PR and uploads artifact |

---

## Technologies

### Core Platform
| Technology | Version | Purpose |
|------------|---------|---------|
| Android | SDK 36 (target), minSdk 26 | Mobile platform |
| Kotlin | 2.2.0 | Programming language |
| Jetpack Compose | BOM 2025.07.00 | Declarative UI framework |
| Material Design 3 | Latest | Design system & theming |
| AGP (Android Gradle Plugin) | 8.13.2 | Build system |
| Gradle | Wrapper (8.13) | Build automation |

### UI & Layout
| Library | Purpose |
|---------|---------|
| `androidx.compose.foundation` | Core Compose primitives |
| `androidx.compose.material3` | Material 3 components |
| `androidx.compose.material.icons.extended` | Extended material icons |
| `com.cheonjaeung.compose:grid` | Grid layout for file browsing |
| `io.github.kevinnzou:compose-swipebox` | Swipe-to-dismiss/box selection |
| `sh.calvin.reorderable:reorderable` | Drag-and-drop reordering |
| `me.saket.cascade:cascade-compose` | Dropdown/popup menus |
| `com.github.nanihadesuka:LazyColumnScrollbar` | Scrollbar for lazy columns |
| `net.engawapg.lib:zoomable` | Image zoom/pan gestures |
| `me.saket.telephoto:zoomable-image-coil3` | Zoomable images with Coil |

### Data, Networking & Storage
| Library | Purpose |
|---------|---------|
| `androidx.datastore:datastore-preferences` | Key-value preferences storage |
| `com.anggrayudi:storage` | Storage access framework helpers |
| `androidx.core:core-ktx` | Kotlin extensions for Android |
| `commons-net:commons-net` | FTP/network file operations |
| `com.anggrayudi:storage` | Storage access framework |

### Media & Imaging
| Library | Purpose |
|---------|---------|
| `io.coil-kt.coil3:coil-compose-android` | Image loading & caching |
| `io.coil-kt.coil3:coil-gif` | GIF decoding |
| `io.coil-kt.coil3:coil-svg` | SVG decoding |
| `io.coil-kt.coil3:coil-video` | Video thumbnail decoding |
| `androidx.media3:media3-exoplayer` | Video/audio playback |
| `androidx.media3:media3-ui` | Media UI components |
| `androidx.media3:media3-ui-compose` | Compose media UI |
| `androidx.palette:palette-ktx` | Color extraction from images |
| `net.lingala.zip4j:zip4j` | ZIP archive creation/extraction |

### Code Editing & Syntax Highlighting
| Library | Purpose |
|---------|---------|
| `io.github.Rosemoe.sora-editor:editor` | Sora code editor (TextMate-based) |
| `io.github.Rosemoe.sora-editor:language-java` | Java syntax support |
| `io.github.Rosemoe.sora-editor:language-textmate` | TextMate grammar support |

### APK & Signing
| Library | Purpose |
|---------|---------|
| `com.android.tools.build:apksig` | APK signature verification |
| `APKEditor.jar` | Local APK editing (libs/ directory) |

### Testing
| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.test.ext:junit` | 1.3.0 | AndroidX JUnit tests |
| `androidx.test.espresso:espresso-core` | 3.7.0 | UI testing |
| `androidx.test.uiautomator:uiautomator` | 2.3.0 | UI automation |
| `androidx.benchmark:benchmark-macro-junit4` | 1.4.0 | Performance benchmarking |
| `androidx.profileinstaller` | 1.4.1 | Baseline profile installation |

### Serialization
| Library | Purpose |
|---------|---------|
| `com.google.code.gson:gson` | JSON parsing |

### Desugaring
| Library | Purpose |
|---------|---------|
| `com.android.tools:desugar_jdk_libs` | JDK 17+ APIs on older Android versions |

---

## Project Structure

```
app/src/main/java/com/raival/compose/file/explorer/
├── App.kt                          # Application class, DI, global state
├── base/BaseActivity.kt            # Base activity class
├── theme/Theme.kt                  # Material 3 dark/light theme config
├── common/
│   ├── Utils.kt                    # Shared utility extensions
│   ├── FileExplorerLogger.kt       # Logging utility
│   ├── icons/                      # SVG icon definitions (code, java, kotlin, etc.)
│   └── ui/                         # Shared composables (BottomSheet, SegmentedControl, etc.)
├── coil/                           # Coil image decoder adapters (APK, audio, PDF, ZIP)
├── screen/
│   ├── main/
│   │   ├── MainActivity.kt         # Main entry point activity
│   │   ├── MainActivityManager.kt  # Main screen state management
│   │   ├── MainActivityState.kt    # State data class
│   │   ├── tab/
│   │   │   ├── Tab.kt              # Tab data model
│   │   │   ├── apps/               # Apps tab (installed apps browser)
│   │   │   ├── files/              # Files tab (file browser)
│   │   │   │   ├── ContentOperationService.kt  # File operations (copy, delete, rename)
│   │   │   │   ├── SearchManager.kt           # File search logic
│   │   │   │   ├── StorageProvider.kt         # Storage/directory provider
│   │   │   │   ├── ContentPropertiesProvider.kt # File properties/content extraction
│   │   │   │   ├── task/             # Async file operations (CopyTask, DeleteTask, RenameTask, etc.)
│   │   │   │   ├── state/            # UI state holders
│   │   │   │   ├── provider/         # Content providers / factories
│   │   │   │   ├── holder/           # View holders for different file types
│   │   │   │   └── misc/             # Constants, mimes, sort configs, view types
│   │   │   └── logs/                 # System logs viewer
│   │   └── startup/StartupTabs.kt   # Startup tab configuration
│   ├── preferences/                  # Settings/preferences screen
│   ├── textEditor/                   # Code editor screen (Sora editor)
│   └── viewer/                       # File viewers (image, video, audio, PDF, text)
└── ...
```

---

## Best Practices and Guidelines

### Architecture
- **MVX/MVI pattern**: Screen state is managed via state classes (e.g., `MainActivityState`, `BottomOptionsBarState`, `DialogsState`).
- **Coroutines**: All async work uses Kotlin coroutines with `Dispatchers.IO` for file operations and `Dispatchers.Main` for UI updates.
- **Lazy initialization**: Managers are initialized with `by lazy` in the `App` class to avoid unnecessary startup overhead.
- **Singleton pattern**: `App` class serves as the central access point for app-wide state via `globalClass` and `appContext`.

### Kotlin & Compose
- **Kotlin code style**: Set to `"official"` in `gradle.properties`.
- **Compose best practices**: Use `remember`, `LaunchedEffect`, and `SideEffect` appropriately. Avoid recomposition overhead by using `MutableState` and derived state wisely.
- **Jetpack Compose BOM**: Use the BOM (`androidx.compose.bom`) to ensure version compatibility across all Compose libraries — do not specify individual Compose library versions.
- **API level targeting**: `minSdk = 26`, `targetSdk = 36`. Use `Build.VERSION.SDK_INT` checks for feature-gating on newer APIs.
- **Core library desugaring**: Enabled (`isCoreLibraryDesugaringEnabled = true`) to use JDK 17 APIs on older Android versions. `compileOptions` and `kotlinOptions` both target `JavaVersion.VERSION_17`.

### File Operations
- **Background execution**: All heavy file I/O (copy, delete, rename, compress) runs in `Dispatchers.IO` via coroutines — never on the main thread.
- **Task pattern**: File operations follow a `Task` pattern (`CopyTask`, `DeleteTask`, `RenameTask`, `CompressTask`, `ApksMergeTask`) — each is a self-contained unit of work with progress reporting.
- **Recycle bin**: Deleted files go to `.prism/bin/` (app's internal storage) for recovery, not directly to the system trash.
- **Batch operations**: Support multi-select and batch copy/move/delete/compress.

### Image Loading (Coil)
- **Custom decoders**: The app registers custom Coil decoders for APK, audio, PDF, and ZIP files via `ImageLoader.Builder.components`.
- **Memory cache**: Set to 35% of available memory (`maxSizePercent = 0.35`).
- **Disk cache**: Enabled with `CachePolicy.ENABLED`.
- **Crossfade**: Enabled for smooth image transitions.

### Theming
- **Material 3**: Uses `darkColorScheme()` / `lightColorScheme()` as base, with `dynamicDarkColorScheme()` / `dynamicLightColorScheme()` on Android 12+.
- **Theme preference**: Three modes — `LIGHT`, `DARK`, `SYSTEM` (follows system dark mode).
- **Status bar**: Tinted based on theme (light status bars in dark theme and vice versa).

### ProGuard / Release
- **ProGuard rules**: Keep all project classes (`com.raival.compose.file.explorer.**`), TextMate editor classes (`org.eclipse.tm4e.**`, `org.joni.**`), and APKSig classes.
- **R8**: Enabled in release builds (`isMinifyEnabled = true`). Use `android.nonTransitiveRClass=true` to reduce R class size.
- **Debug builds**: `applicationIdSuffix = ".debug"` in `buildTypes.debug` so the debug APK (`com.raival.compose.file.explorer.debug`) can be installed alongside the release build.
- **Baseline profiles**: Generated for release builds to optimize startup and scroll performance.

### Testing
- **Unit tests**: Use JUnit 4 with `androidx.test.ext:junit`.
- **UI tests**: Use Espresso and UI Automator for integration/UI testing.
- **Benchmark tests**: Use `benchmark-macro-junit4` for performance benchmarks.

### CI/CD
- **GitHub Actions**: The workflow at `.github/workflows/android.yml` builds a release APK on push/PR and uploads the artifact.
- **Trigger paths**: Workflow triggers include all `app/**`, `gradle/**`, build files, and `public-stable-ids.txt`.
- **JDK**: CI uses Eclipse Temurin JDK 21.0.6 on Ubuntu runners.

### Code Style
- **Meaningful commit messages**: Follow conventional commit style when possible (e.g., `feat:`, `fix:`, `perf:`, `refactor:`).
- **Kotlin conventions**: Follow standard Kotlin style guide — no need for Java-style Javadoc on internal code; use descriptive function/variable names.
- **Package structure**: Keep packages organized by feature/screen (e.g., `screen.main.tab.files.task` for file operation tasks).
- **Avoid hardcoded strings**: Use `@StringRes` references and `strings.xml` for all user-facing text.
- **Resource naming**: Use snake_case for resource names (images, layouts, values).

### Dependencies Management
- **Version catalog**: All dependency versions are centralized in `gradle/libs.versions.toml`. Do not specify versions inline in `build.gradle.kts` files.
- **Compose BOM**: Use `platform(libs.androidx.compose.bom)` to align all Compose library versions automatically.
- **Update safety**: Before updating a dependency version, check the corresponding version reference and verify API compatibility in `libs.versions.toml`.

### Security
- **APK signing**: Use the signing config in `app/build.gradle.kts` for release builds. Keystore files are in `app/src/main/assets/keystore/` (do not commit keystore passwords to VCS).
- **Network security**: Configured in `app/src/main/res/xml/network_security_config.xml`.
- **File permissions**: Validate and sanitize all file paths before access to prevent path traversal.
- **ProGuard obfuscation**: Enabled in release builds to protect code logic.

### Performance
- **Baseline profiles**: Generate and use baseline profiles for release builds to improve startup and scrolling.
- **Image loading**: Coil caches aggressively; use `crossfade` for UX and limit `bitmapFactoryMaxParallelism` to 1 to avoid memory pressure.
- **Lazy loading**: Use `LazyColumn`/`LazyRow` for long lists; avoid inflating all items at once.
- **Configuration cache**: Enabled in `gradle.properties` (`org.gradle.configuration-cache=true`) for faster builds.

---

## Additional Notes

- This repository is **no longer actively maintained** by the original author (as noted in README.md). Forks are welcome.
- The project uses **GPLv3** license — all contributions must be compatible with GPLv3.
- Fastlane metadata is maintained in `fastlane/metadata/android/en-US/` for Play Store listings.
- The `assets/textmate/` directory contains TextMate grammar files for the code editor (Java, Kotlin, JSON, XML syntax highlighting).
- The `baselineProfile/` module generates baseline profiles for performance optimization.
