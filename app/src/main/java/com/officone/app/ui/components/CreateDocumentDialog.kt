package com.officone.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.officone.app.document.DocumentType

enum class DocumentFormat(val extension: String, val mimeType: String, val docType: DocumentType) {
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentType.DOCUMENT),
    ODT("odt", "application/vnd.oasis.opendocument.text", DocumentType.DOCUMENT),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", DocumentType.SPREADSHEET),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet", DocumentType.SPREADSHEET),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", DocumentType.PRESENTATION),
    ODP("odp", "application/vnd.oasis.opendocument.presentation", DocumentType.PRESENTATION)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateDocumentDialog(
    initialDocumentType: DocumentType = DocumentType.DOCUMENT,
    onDismissRequest: () -> Unit,
    onCreateConfirm: (fileName: String, format: DocumentFormat) -> Unit
) {
    val defaultFormat = when (initialDocumentType) {
        DocumentType.SPREADSHEET -> DocumentFormat.XLSX
        DocumentType.PRESENTATION -> DocumentFormat.PPTX
        else -> DocumentFormat.DOCX
    }

    val defaultFileName = when (initialDocumentType) {
        DocumentType.SPREADSHEET -> "Untitled Spreadsheet"
        DocumentType.PRESENTATION -> "Untitled Presentation"
        else -> "Untitled Document"
    }

    var fileName by remember { mutableStateOf(defaultFileName) }
    var selectedFormat by remember { mutableStateOf(defaultFormat) }
    var isError by remember { mutableStateOf(false) }

    val availableFormats = DocumentFormat.entries.toTypedArray()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "New Document",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        if (isError && it.isNotBlank()) {
                            isError = false
                        }
                    },
                    label = { Text("File Name") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("File name cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "File Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableFormats.forEach { format ->
                        FilterChip(
                            selected = (selectedFormat == format),
                            onClick = { selectedFormat = format },
                            label = { Text(".${format.extension.uppercase()}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = fileName.trim()
                    if (trimmed.isEmpty()) {
                        isError = true
                    } else {
                        val finalName = if (trimmed.endsWith(".${selectedFormat.extension}", ignoreCase = true)) {
                            trimmed
                        } else {
                            "$trimmed.${selectedFormat.extension}"
                        }
                        onCreateConfirm(finalName, selectedFormat)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
