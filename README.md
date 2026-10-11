# Office One - All-in-One Android Office Suite

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Android%2010--15%20(API%2029--35)-blue.svg)]()
[![Privacy](https://img.shields.io/badge/Permissions-ZERO%20(No%20Internet)-success.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0%20%2F%20MIT-lightgrey.svg)]()

**Office One** is a lightweight, 100% offline, privacy-first Android office suite designed as an open-source alternative to proprietary office software. Built with modern Kotlin and Jetpack Compose Material 3, it combines document editing, spreadsheet modeling, presentation design, and PDF annotation into a single cohesive application.

---

## ✨ Features & Capabilities

### 📝 Word Processor (Word Alternative)
* **Formats Supported**: Microsoft Word (`.docx`, `.doc`), OpenDocument (`.odt`), Rich Text (`.rtf`), Plain Text (`.txt`), and Markdown (`.md`).
* **Desktop Ribbon Interface**: Familiar tabbed ribbon (Home, Insert, Layout, View) adapted for phone and tablet touchscreens.
* **Text & Paragraph Styling**: Real-time font sizing, colors, bold, italic, underline, alignment presets, bulleted and numbered lists.
* **Document Metrics**: Live word count, character count, and page estimations.
* **Export**: One-tap print layout rendering and PDF export.

### 📊 Spreadsheet Engine (Excel Alternative)
* **Formats Supported**: Microsoft Excel (`.xlsx`, `.xls`), OpenDocument (`.ods`), Comma-Separated Values (`.csv`), and Tab-Separated Values (`.tsv`).
* **Formula Engine**: Custom calculation engine supporting 25+ standard Excel formulas:
  * *Math & Aggregates*: `SUM`, `AVERAGE`, `COUNT`, `MAX`, `MIN`, `PRODUCT`, `ROUND`.
  * *Logical*: `IF`, `AND`, `OR`, `NOT`.
  * *Lookup & Reference*: `VLOOKUP`.
  * *String Utilities*: `CONCATENATE`, `LEFT`, `RIGHT`, `UPPER`, `LOWER`.
* **Multi-Sheet Support**: Seamless navigation across workbooks with multiple worksheets.
* **Interactive Formula Bar**: View and edit computed results vs. raw formula expressions.

### 📽️ Presentation Editor (PowerPoint Alternative)
* **Formats Supported**: Microsoft PowerPoint (`.pptx`, `.ppt`), OpenDocument (`.odp`).
* **Slide Canvas**: WYSIWYG editing of slide titles, bulleted content boxes, and subtitles.
* **Slide Deck Manager**: Thumbnail carousel for rearranging, duplicating, and inserting slides.
* **Presenter Notes**: Dedicated speaker notes drawer.
* **Fullscreen Slideshow**: Integrated presentation viewer for live rehearsals.

### 📄 PDF Reader & Annotator
* **Multi-Page Rendering**: High-performance scrolling canvas with pinch-to-zoom (50% to 400%).
* **Annotations**: Text highlighting, underlining, and sticky notes.
* **Document Utilities**: Full-text keyword search, 90° page rotation, page deletion, PDF merging, and splitting.

---

## 🔒 Privacy & Security Architecture

1. **Zero Dangerous Permissions**:
   * **No `android.permission.INTERNET`**: The application contains no network stack. Files never leave your local device.
   * **No Storage Permissions**: Uses Android's **Storage Access Framework (SAF)** and `content://` URIs, giving the app scoped access only to documents you explicitly open.
2. **Decompression Bomb Neutralization**:
   * Strict 100:1 maximum compression ratio and 250MB total expansion limits prevent zip bomb attacks from crashing the system.
3. **Zip-Slip Traversal Prevention**:
   * Canonical path verification blocks malicious archives containing `../` directory traversal payloads.
4. **Macro Execution Hardening**:
   * Office One inspects OOXML archives and neutralizes embedded VBA macros (`vbaProject.bin`), running documents in a sandboxed, safe mode.
5. **Data Loss Prevention**:
   * Atomic `.bak` file writing guarantees that power cuts or dead batteries during file saving will never corrupt your original documents.
   * Background `AutoSaveManager` continuously creates crash-recovery snapshots every 30 seconds.

---

## 📁 Project Structure

```
OfficeOne/
├── .github/workflows/
│   └── build-release.yml         # Cloud CI/CD workflow (Zero-install APK build)
├── app/
│   ├── build.gradle.kts          # Module configuration, R8/ProGuard & dependencies
│   ├── proguard-rules.pro        # POI, PdfBox, and Compose optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Zero-permission manifest with 84 MIME bindings
│       │   ├── java/com/officone/app/
│       │   │   ├── autosave/     # AutoSaveManager & Crash Recovery
│       │   │   ├── data/         # Room Database & DataStore Preferences
│       │   │   ├── document/     # Document metadata and type resolvers
│       │   │   ├── engine/       # Calculation engines, LOKit & POI fallbacks
│       │   │   ├── fileio/       # Storage Access Framework (SAF) & MIME helpers
│       │   │   ├── navigation/   # Jetpack Compose Navigation Graph
│       │   │   ├── pdf/          # PDF rendering and annotation controllers
│       │   │   ├── security/     # DocumentSecurityGuard (Zip-slip & bomb defenses)
│       │   │   └── ui/           # Compose Material 3 Screens & Ribbon UI
│       │   └── res/              # Vector icons, colors, themes, strings
│       └── test/java/            # JVM unit tests (Formulas, Security, SAF)
├── gradle/
│   ├── libs.versions.toml        # Centralized Gradle Version Catalog
│   └── wrapper/                  # Gradle Wrapper configuration
├── sample_documents/             # ECMA-376 sample test files & QA scripts
├── build_release.bat             # Turnkey Windows build script
├── build_release.ps1             # PowerShell build automation script
├── gradlew.bat                   # Gradle wrapper launcher (Windows)
├── gradlew                       # Gradle wrapper launcher (Linux/macOS)
├── package_verifier.py           # Release package auditor script
├── README.md                     # Technical architecture documentation
├── USER_GUIDE.md                 # Complete end-user guide
└── RELEASE_NOTES.md              # Version release notes
```

---

## 🛠️ Building & Packaging the APK

You do **not** need Android Studio installed to build and install Office One.

### Method A: Zero-Install Cloud Build via GitHub (Recommended for Non-Programmers)
1. Fork or push this repository to your personal GitHub account.
2. Go to the **Actions** tab on your GitHub repository.
3. The **Build Office One APK** workflow will automatically run and compile the APK using GitHub's cloud servers.
4. Download the ready-to-install `OfficeOne-Debug-APK` directly from the workflow summary screen!

### Method B: Local Command-Line Build (No Android Studio Required)
If you have Java 17 and the Android SDK command-line tools installed:
1. Double-click `build_release.bat` (or run `./build_release.ps1` in PowerShell).
2. The script will automatically run pre-flight QA audits, execute the Gradle build, and place the compiled APK in the `releases/` folder:
   ```powershell
   ./gradlew assembleDebug
   ```
3. Transfer the generated APK (`OfficeOne-v1.0.0-alpha1-debug.apk`) to your Android device and install it.

---

## 🧪 Testing & Verification

Office One includes comprehensive automated test suites that can be executed directly:

1. **Formula Engine & Format Tests**:
   ```powershell
   python sample_documents/validate_poc.py
   ```
2. **Security, Decompression Bomb & Traversal Tests**:
   ```powershell
   python sample_documents/test_phase5_qa.py
   ```
3. **Packaging & Release Readiness Audit**:
   ```powershell
   python package_verifier.py
   ```

---

## 📄 License & Attributions
* **Office One** source code is released under the **Apache License 2.0**.
* **Third-Party Libraries**:
  * Apache POI (Apache 2.0)
  * PdfBox-Android (Apache 2.0)
  * Jetpack Compose & AndroidX (Apache 2.0)
  * Coil (Apache 2.0)
