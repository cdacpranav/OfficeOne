package com.officone.app.ui.ribbon

import androidx.compose.ui.graphics.vector.ImageVector

data class RibbonTab(
    val name: String,
    val icon: ImageVector? = null,
    val groups: List<RibbonGroup> = emptyList()
)
