package com.officone.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentSecurityGuardTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testValidZipPassesSecurityCheck() {
        val zipFile = tempFolder.newFile("valid.docx")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write("<xml>Valid Content</xml>".toByteArray())
            zos.closeEntry()
        }

        // Should not throw
        DocumentSecurityGuard.validateZipArchive(zipFile)
    }

    @Test
    fun testPathTraversalDetectedAndBlocked() {
        val maliciousZip = tempFolder.newFile("malicious_slip.docx")
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            zos.putNextEntry(ZipEntry("../../../etc/passwd"))
            zos.write("attack payload".toByteArray())
            zos.closeEntry()
        }

        try {
            DocumentSecurityGuard.validateZipArchive(maliciousZip)
            fail("Expected SecurityException for path traversal attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Path traversal") == true)
        }
    }

    @Test
    fun testMacroDetection() {
        val macroFile = tempFolder.newFile("sales.xlsm")
        val warnings = DocumentSecurityGuard.detectDocumentFeatures(macroFile)
        assertTrue(warnings.isNotEmpty())
        assertTrue(warnings.any { it.contains("Macro-enabled") })
    }

    @Test
    fun testAtomicBackupAndRestore() {
        val originalFile = tempFolder.newFile("important_contract.docx")
        originalFile.writeText("Original contract text v1")

        val backupDir = tempFolder.newFolder("backups")

        // 1. Create atomic backup
        val backupFile = DocumentSecurityGuard.createAtomicBackup(originalFile, backupDir)
        assertNotNull(backupFile)
        assertTrue(backupFile!!.exists())
        assertEquals("Original contract text v1", backupFile.readText())

        // 2. Simulate file corruption
        originalFile.writeText("CORRUPTED BY POWER LOSS")
        assertEquals("CORRUPTED BY POWER LOSS", originalFile.readText())

        // 3. Restore backup
        val restored = DocumentSecurityGuard.restoreBackup(backupFile, originalFile)
        assertTrue(restored)
        assertEquals("Original contract text v1", originalFile.readText())
    }
}
