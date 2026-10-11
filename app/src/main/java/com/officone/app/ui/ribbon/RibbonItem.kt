package com.officone.app.ui.ribbon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

sealed class RibbonItem {
    data class IconButton(
        val icon: ImageVector,
        val label: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true
    ) : RibbonItem()

    data class ToggleButton(
        val icon: ImageVector,
        val label: String,
        val isActive: Boolean,
        val onToggle: (Boolean) -> Unit,
        val enabled: Boolean = true
    ) : RibbonItem()

    data class DropdownButton(
        val icon: ImageVector? = null,
        val label: String,
        val options: List<String>,
        val selectedOption: String = "",
        val onSelect: (String) -> Unit,
        val enabled: Boolean = true
    ) : RibbonItem()

    data class ColorPicker(
        val icon: ImageVector,
        val label: String,
        val currentColor: Color,
        val onColorSelect: (Color) -> Unit,
        val enabled: Boolean = true
    ) : RibbonItem()

    data class Spinner(
        val label: String,
        val value: Int,
        val onValueChange: (Int) -> Unit,
        val min: Int = 1,
        val max: Int = 100,
        val step: Int = 1,
        val enabled: Boolean = true
    ) : RibbonItem()
}
