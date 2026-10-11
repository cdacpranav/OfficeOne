# Office One - Release Notes
## Version 1.0.0-alpha1 (Milestone Release)

We are thrilled to announce the completion and release package of **Office One v1.0.0-alpha1**, an open-source, private, and fully offline Android office suite engineered to replace proprietary desktop office suites on smartphones and tablets.

---

## 🌟 Key Highlights

### 1. Unified 4-in-1 Productivity Suite
* **Document Editor (Word Alternative)**:
  * Tabbed ribbon interface (Home, Insert, Layout, View).
  * Formatting styles: bold, italic, underline, strikethrough, text coloring, alignment presets.
  * Real-time metrics: word, character, and page counts.
  * Export directly to PDF.
* **Spreadsheet Engine (Excel Alternative)**:
  * Multi-worksheet tab management.
  * Formula bar with live cell evaluation.
  * Built-in calculation engine supporting 25+ Excel functions (`SUM`, `AVERAGE`, `COUNT`, `MAX`, `MIN`, `IF`, `AND`, `OR`, `VLOOKUP`, `CONCATENATE`, etc.).
  * Range calculations and cyclic dependency detection.
* **Presentation Editor (PowerPoint Alternative)**:
  * 16:9 interactive slide canvas with editable text blocks.
  * Slide thumbnail manager for reordering, adding, and reviewing decks.
  * Speaker notes drawer and full-screen slideshow preview mode.
* **PDF Reader & Annotator**:
  * Fluid multipage scrolling with pinch-to-zoom (50% to 400%).
  * Text search across all pages.
  * Highlighting, sticky notes, page rotation (90°), and PDF merge/split utilities.

### 2. Zero-Permission Privacy Model
* **100% Offline**: Zero internet permission declared (`NO android.permission.INTERNET`). Your data never leaves your phone.
* **Scoped Storage Only**: Zero external storage permissions requested. Document access is governed solely by Android's **Storage Access Framework (SAF)** and user-selected Content URIs.

### 3. Enterprise-Grade Security & Reliability
* **Decompression Bomb Neutralization**: 100:1 compression ratio limits and 250MB maximum expansion thresholds defend against archive bombs.
* **Zip-Slip Traversal Protection**: Rejects archives attempting path manipulation (`../`).
* **Macro Hardening**: Embedded macros (`vbaProject.bin`) are automatically neutralized to prevent malicious code execution.
* **Data Loss Prevention**: Atomic `.bak` copies prevent document corruption if the device powers down during saves.
* **AutoSave Recovery**: Automatic 30-second snapshotting recovers in-progress edits after unexpected app restarts.

### 4. Zero-Install Build Automation
* Included `.github/workflows/build-release.yml` allows non-programmers to compile APKs in the cloud via GitHub Actions with zero local tool installation.
* Turnkey local scripts (`build_release.bat` / `build_release.ps1`) for developers using command-line Gradle.

---

## 📋 File Format Support Matrix

| Format | Extension | Viewing | Editing | Saving / Export |
| :--- | :--- | :---: | :---: | :---: |
| **Microsoft Word** | `.docx`, `.doc` | ✅ | ✅ | ✅ (DOCX, PDF) |
| **OpenDocument Text** | `.odt` | ✅ | ✅ | ✅ (ODT, PDF) |
| **Rich Text Format** | `.rtf` | ✅ | ✅ | ✅ |
| **Plain Text / Markdown**| `.txt`, `.md` | ✅ | ✅ | ✅ |
| **Microsoft Excel** | `.xlsx`, `.xls` | ✅ | ✅ | ✅ (XLSX, CSV) |
| **OpenDocument Sheet** | `.ods` | ✅ | ✅ | ✅ (ODS, CSV) |
| **Delimited Data** | `.csv`, `.tsv` | ✅ | ✅ | ✅ |
| **Microsoft PowerPoint**| `.pptx`, `.ppt` | ✅ | ✅ | ✅ (PPTX, PDF) |
| **OpenDocument Slides** | `.odp` | ✅ | ✅ | ✅ |
| **Portable Document** | `.pdf` | ✅ | ✅ (Markup) | ✅ (Annotated PDF)|

---

## 🔧 Tested Environments
* **Target Platforms**: Android 10 (API 29), Android 11 (API 30), Android 12 (API 31/32), Android 13 (API 33), Android 14 (API 34), Android 15 (API 35).
* **Architecture Targets**: `arm64-v8a`, `armeabi-v7a`, `x86_64`.
* **Testing Suites**: 100% passed on format validation, formula evaluation, security hardening, and packaging audits.
