package com.officone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.officone.app.document.DocumentType
import com.officone.app.ui.theme.DocBlue
import com.officone.app.ui.theme.PdfRed
import com.officone.app.ui.theme.PresentationOrange
import com.officone.app.ui.theme.SheetGreen
import com.officone.app.ui.theme.UnknownGray

@Composable
fun DocumentTypeIcon(
    documentType: DocumentType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp
) {
    val (icon: ImageVector, color: Color) = when (documentType) {
        DocumentType.DOCUMENT -> Icons.Default.Description to DocBlue
        DocumentType.SPREADSHEET -> Icons.Default.GridOn to SheetGreen
        DocumentType.PRESENTATION -> Icons.Default.Slideshow to PresentationOrange
        DocumentType.PDF -> Icons.Default.PictureAsPdf to PdfRed
        DocumentType.UNKNOWN -> Icons.Default.InsertDriveFile to UnknownGray
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = documentType.getDisplayName(),
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}
