> [!NOTE]
> **This is the maintained fork of Prism File Explorer** by [involvex](https://github.com/involvex).
> Upstream (`Raival-e/Prism-File-Explorer`) is no longer actively maintained — all new development
> (Shizuku, SFTP, Office viewer, archive expansion, storage analyzer, …) happens here.

<div align="center">

<img src="assets/app_icon.png" width="120" alt="Prism File Explorer Logo"/>

# Prism File Explorer

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/involvex/Prism-File-Explorer?label=Release)](https://github.com/involvex/Prism-File-Explorer/releases)
[![APK Downloads](https://img.shields.io/github/downloads/involvex/Prism-File-Explorer/total.svg?label=APK%20Downloads)](https://github.com/involvex/Prism-File-Explorer/releases)
[![Stars](https://img.shields.io/github/stars/involvex/Prism-File-Explorer?style=flat&logo=github)](https://github.com/involvex/Prism-File-Explorer/stargazers)
[![Forks](https://img.shields.io/github/forks/involvex/Prism-File-Explorer?style=flat&logo=github)](https://github.com/involvex/Prism-File-Explorer/network/members)
[<img src="https://shields.rbtlog.dev/simple/com.raival.compose.file.explorer" alt="RB shield">](https://shields.rbtlog.dev/com.raival.compose.file.explorer)

**A modern, feature-rich, and lightweight file manager for Android, built entirely with Kotlin and Jetpack Compose.**

*Delivering a seamless file management experience with a beautiful Material Design interface*

</div>

---

## 📱 Screenshots

<div align="center">
  <img src="assets/image1.png" width="22%" alt="Main Interface"/>
  <img src="assets/image2.png" width="22%" alt="File Operations"/>
  <img src="assets/image3.png" width="22%" alt="Apps Extractor"/>
  <img src="assets/image4.png" width="22%" alt="Text Editor"/>
</div>

<div align="center">
  <img src="assets/image6.png" width="21%" alt="Audio Player"/>
  <img src="assets/image7.png" width="21%" alt="Image Viewer"/>
  <img src="assets/image5.png" width="21%" alt="PDF Viewer"/>
</div>

---

## ✨ Key Features

### 🎨 **Modern User Interface**

- **Jetpack Compose UI**: Fully declarative and responsive interface
- **Material Design 3**: Beautiful, consistent design language
- **Dark/Light Theme**: Automatic theme switching support
- **Smooth Animations**: Fluid transitions and interactions

### 📁 **Comprehensive File Management**

- **Complete File Operations**: Create, copy, move, rename, delete files and folders
- **Advanced Selection**: Multi-select with intuitive gestures
- **Smart Cut/Copy/Paste**: Clipboard operations with visual feedback
- **Batch Operations**: Perform actions on multiple files simultaneously
- **File Properties**: Detailed information about files and directories

### 🗂️ **Advanced Features**

- **Multi-Tab Interface**: Manage multiple directories simultaneously
- **Quick Navigation**: Breadcrumb navigation and quick access shortcuts
- **Search Functionality**: Find files and folders quickly
- **Sorting Options**: Sort by name, size, date, type with ascending/descending order
- **View Modes**: Grid and list view options

### 📺 **Built-in Media Viewers**

- **Image Viewer**: Support for JPEG, PNG, GIF, WebP, and more
- **Video Player**: Play MP4, AVI, MKV, MOV, and other formats
- **Audio Player**: MP3, WAV, FLAC, OGG playback support
- **PDF Viewer**: View PDF documents natively
- **Text Editor**: Syntax highlighting for code files

### 🗜️ **Archive Management**

- **Extract Archives**: Support for ZIP format
- **Create Archives**: Compress files and folders into ZIP format
- **Archive Preview**: Browse archive contents without extraction

### ⚡ **Performance & Efficiency**

- **Lightweight**: Minimal resource usage and fast startup
- **Optimized**: Smooth performance on low-end devices
- **Background Operations**: Non-blocking file operations
- **Memory Efficient**: Smart memory management for large directories

---

## 📥 Download Options

| Platform            | Link                                                                                                                                                                                                                                                                                                                                                                        |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **GitHub Releases** | [![GitHub](https://img.shields.io/badge/GitHub-Download-black?style=for-the-badge&logo=github)](https://github.com/involvex/Prism-File-Explorer/releases)                                                                                                                                                                                                                   |
| **IzzyOnDroid**     | [![IzzyOnDroid](https://img.shields.io/badge/IzzyOnDroid-Download-green?style=for-the-badge)](https://apt.izzysoft.de/fdroid/index/apk/com.raival.compose.file.explorer)                                                                                                                                                                                                    |
| **Obtainium**       | [![Obtainium](https://img.shields.io/badge/Obtainium-Download-blue?style=for-the-badge)](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.raival.compose.file.explorer%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Finvolvex%2FPrism-File-Explorer%22%2C%22author%22%3A%22involvex%22%2C%22name%22%3A%22File%20Explorer%20Compose%22%7D) |
| **OpenApk**         | [![OpenApk](https://img.shields.io/badge/OpenApk-Download-purple?style=for-the-badge)](https://www.openapk.net/file-explorer/com.raival.compose.file.explorer/)                                                                                                                                                                                                             |

> **Latest release: [v1.4.0](https://github.com/involvex/Prism-File-Explorer/releases/tag/v1.4.0)** — debug APK (unsigned, side-by-side with Play builds).

### 📋 System Requirements

- **Android Version**: 8.0 (API 26) or higher
- **Permissions**: Storage access for file management

---

## 🆕 Fork Enhancements (involvex)

On top of upstream, this fork adds:

| Area | Enhancement |
|------|-------------|
| 🔓 **Shizuku** | Browse `Android/data`, `data/data`, `Android/obb` without root; shell-backed copy/delete/rename |
| 🌐 **SFTP** | Saved servers, TOFU known-hosts, encrypted credentials, remote browsing + sharing |
| 📄 **Documents** | Office/ODF viewing via PDF conversion, export to Markdown/ODF, filled-form save |
| 🗜️ **Archives** | Extract 7z/tar/gz/bz2/xz, password-protected (AES-256) ZIP create + extract |
| 📊 **Analyzer** | Storage analyzer: per-folder size bars + largest-100-files (folder menu) |
| 🎨 **Themes** | Hacker (green-on-black) theme, Material You dynamic-color toggle |
| ⚙️ **Behavior** | Disable-recycle-bin toggle, home-layout prefs, per-folder view memory |
| ✅ **Checksums** | MD5 + SHA-256 in Properties (long-press to copy) |
| ✏️ **Rename** | Batch rename with patterns, regex find-replace, live preview |
| 🔨 **Build** | AGP 8.13.2, debug/release side-by-side installs, in-app update checker (this repo) |

---

## 🛠️ Built With

| Technology                                                                                                               | Purpose                  | Version       |
|--------------------------------------------------------------------------------------------------------------------------|--------------------------|---------------|
| ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)                             | **Programming Language** | Latest        |
| ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white) | **UI Framework**         | Latest Stable |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)                          | **Platform**             | SDK 36, min 26 |
| ![Material Design](https://img.shields.io/badge/Material%20Design-757575?style=flat&logo=materialdesign&logoColor=white) | **Design System**        | Material 3    |


---

## 🔨 Building from Source

### Prerequisites

- **JDK**: 17 or higher
- **Android SDK**: API level 34+
- **Git**: For cloning the repository

### Build Instructions

1. **Clone the repository**:
    ```bash
    git clone https://github.com/involvex/Prism-File-Explorer.git
    cd Prism-File-Explorer
    ```

2. **Build the project**:
    ```bash
    ./gradlew assembleDebug      # unsigned, package com.raival.compose.file.explorer.debug
    ./gradlew assembleRelease    # minified, needs signing config
    ```

3. **Find the APK**:
     - Debug APK: `app/build/outputs/apk/debug/`
     - Release APK: `app/build/outputs/apk/release/`
---

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### 📝 **Development Guidelines**

- Follow Kotlin coding conventions
- Write meaningful commit messages

---

## 📊 Project Stats

![GitHub stats](https://github-readme-stats.vercel.app/api?username=involvex&repo=Prism-File-Explorer&show_icons=true&theme=default)

---

## ☕ Support the Project

If you find **Prism File Explorer** useful and would like to support its development, consider
buying me a coffee! Your support helps me maintain and improve this project.

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://www.buymeacoffee.com/RaivalR)

*Every contribution, no matter how small, helps keep this project alive and growing! ❤️*

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0**.

```
Prism File Explorer - A modern Android file manager
Copyright (C) 2024 Raival-e

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

See the [LICENSE](LICENSE) file for the full license text.

---

## 💬 Support & Community

| Platform               | Purpose                       |
|------------------------|-------------------------------|
| **GitHub Issues**      | Bug reports, feature requests |
| **GitHub Discussions** | Community support, questions  |
| **Email**              | Private inquiries             |

**⭐ If you find this project useful, please consider giving it a star!**

[![Star History](https://img.shields.io/github/stars/involvex/Prism-File-Explorer?style=social)](https://github.com/involvex/Prism-File-Explorer/stargazers)

*Upstream project by [Raival-e](https://github.com/Raival-e/Prism-File-Explorer) (archived) — full credit for the original app.*
