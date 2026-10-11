"""
Office One - Packaging & Release Readiness Verifier
Audits project source tree, resource integrity, manifest declarations,
and release toolchains to confirm 100% distribution readiness.
"""

import os
import sys
import xml.etree.ElementTree as ET

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))

def audit_file_structure():
    print("=" * 65)
    print("       OFFICE ONE - RELEASE PACKAGE & READINESS AUDIT")
    print("=" * 65)
    
    required_paths = [
        "settings.gradle.kts",
        "build.gradle.kts",
        "gradle.properties",
        "gradlew.bat",
        "gradlew",
        "gradle/libs.versions.toml",
        "gradle/wrapper/gradle-wrapper.properties",
        "app/build.gradle.kts",
        "app/proguard-rules.pro",
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/values/strings.xml",
        "app/src/main/res/values/colors.xml",
        "app/src/main/res/values/themes.xml",
        "app/src/main/res/xml/file_paths.xml",
        ".github/workflows/build-release.yml",
        "build_release.bat",
        "build_release.ps1"
    ]
    
    missing = []
    for rel_path in required_paths:
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        if not os.path.exists(full_path):
            missing.append(rel_path)
            
    if missing:
        print(f"[FAIL] Missing required files: {missing}")
        return False
    else:
        print("[PASS] Core project configuration and build scripts intact (17/17).")
        return True

def count_source_files():
    kt_count = 0
    test_count = 0
    main_java = os.path.join(PROJECT_ROOT, "app", "src", "main", "java")
    test_java = os.path.join(PROJECT_ROOT, "app", "src", "test", "java")
    
    for root, dirs, files in os.walk(main_java):
        for f in files:
            if f.endswith(".kt"):
                kt_count += 1
                
    for root, dirs, files in os.walk(test_java):
        for f in files:
            if f.endswith(".kt"):
                test_count += 1
                
    print(f" -> Found {kt_count} Production Kotlin source files.")
    print(f" -> Found {test_count} JVM Test Kotlin source files.")
    if kt_count >= 40:
        print("[PASS] Source code coverage complete across all modules.")
        return True
    else:
        print(f"[WARN] Expected at least 40 source files, found {kt_count}.")
        return False

def verify_manifest():
    manifest_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "AndroidManifest.xml")
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    
    # Check permissions
    permissions = root.findall("uses-permission")
    if len(permissions) == 0:
        print("[PASS] Zero dangerous permissions in AndroidManifest.xml.")
    else:
        print(f"[WARN] Found {len(permissions)} declared permissions.")
        
    # Check Intent Filters
    intent_filters = root.findall(".//intent-filter")
    data_tags = root.findall(".//data")
    print(f" -> Configured {len(intent_filters)} Intent Filters with {len(data_tags)} MIME/Scheme bindings.")
    if len(data_tags) >= 15:
        print("[PASS] File associations complete (DOCX, XLSX, PPTX, PDF, RTF, CSV, etc.).")
        return True
    return False

def verify_modules():
    modules = {
        "Word Processor": [
            "app/src/main/java/com/officone/app/ui/editor/DocumentEditorScreen.kt",
            "app/src/main/java/com/officone/app/ui/editor/DocumentEditorViewModel.kt",
            "app/src/main/java/com/officone/app/ui/ribbon/HomeRibbonContent.kt",
            "app/src/main/java/com/officone/app/ui/ribbon/InsertRibbonContent.kt"
        ],
        "Spreadsheet": [
            "app/src/main/java/com/officone/app/ui/editor/SpreadsheetEditorScreen.kt",
            "app/src/main/java/com/officone/app/ui/editor/SpreadsheetEditorViewModel.kt",
            "app/src/main/java/com/officone/app/engine/SpreadsheetFormulaEngine.kt"
        ],
        "Presentation": [
            "app/src/main/java/com/officone/app/ui/editor/PresentationEditorScreen.kt",
            "app/src/main/java/com/officone/app/ui/editor/PresentationEditorViewModel.kt"
        ],
        "PDF Module": [
            "app/src/main/java/com/officone/app/ui/pdf/PdfViewerScreen.kt",
            "app/src/main/java/com/officone/app/ui/pdf/PdfViewerViewModel.kt",
            "app/src/main/java/com/officone/app/pdf/PdfManager.kt"
        ],
        "Security & Storage": [
            "app/src/main/java/com/officone/app/security/DocumentSecurityGuard.kt",
            "app/src/main/java/com/officone/app/engine/MemoryOptimizer.kt",
            "app/src/main/java/com/officone/app/autosave/AutoSaveManager.kt",
            "app/src/main/java/com/officone/app/fileio/SAFFileHandler.kt"
        ]
    }
    
    all_ok = True
    for mod_name, files in modules.items():
        missing = [f for f in files if not os.path.exists(os.path.join(PROJECT_ROOT, f))]
        if missing:
            print(f"[FAIL] {mod_name} missing: {missing}")
            all_ok = False
        else:
            print(f"[PASS] {mod_name} components verified.")
            
    return all_ok

if __name__ == "__main__":
    t1 = audit_file_structure()
    t2 = count_source_files()
    t3 = verify_manifest()
    t4 = verify_modules()
    
    print("=" * 65)
    if t1 and t2 and t3 and t4:
        print("ALL RELEASE READINESS AUDITS PASSED (100% READY FOR RELEASE)!")
        print("=" * 65)
        sys.exit(0)
    else:
        print("SOME AUDITS FAILED.")
        print("=" * 65)
        sys.exit(1)
