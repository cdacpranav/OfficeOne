package com.officone.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Primary - Professional Blue Palette
val BluePrimary = Color(0xFF1A73E8)
val BlueOnPrimary = Color(0xFFFFFFFF)
val BluePrimaryContainer = Color(0xFFD2E3FC)
val BlueOnPrimaryContainer = Color(0xFF041E49)

// Dark Primary
val BluePrimaryDark = Color(0xFFA8C7FA)
val BlueOnPrimaryDark = Color(0xFF062E6F)
val BluePrimaryContainerDark = Color(0xFF0842A0)
val BlueOnPrimaryContainerDark = Color(0xFFD2E3FC)

// Secondary - Slate Gray Palette
val SlateSecondary = Color(0xFF5F6368)
val SlateOnSecondary = Color(0xFFFFFFFF)
val SlateSecondaryContainer = Color(0xFFE8EAED)
val SlateOnSecondaryContainer = Color(0xFF202124)

// Dark Secondary
val SlateSecondaryDark = Color(0xFFBDC1C6)
val SlateOnSecondaryDark = Color(0xFF202124)
val SlateSecondaryContainerDark = Color(0xFF3C4043)
val SlateOnSecondaryContainerDark = Color(0xFFE8EAED)

// Tertiary - Accent Green for success states
val GreenTertiary = Color(0xFF1E8E3E)
val GreenOnTertiary = Color(0xFFFFFFFF)
val GreenTertiaryContainer = Color(0xFFCEEAD6)
val GreenOnTertiaryContainer = Color(0xFF0D652D)

// Dark Tertiary
val GreenTertiaryDark = Color(0xFF81C995)
val GreenOnTertiaryDark = Color(0xFF043815)
val GreenTertiaryContainerDark = Color(0xFF0F5223)
val GreenOnTertiaryContainerDark = Color(0xFFCEEAD6)

// Error Palette
val ErrorRed = Color(0xFFD93025)
val OnErrorRed = Color(0xFFFFFFFF)
val ErrorContainerRed = Color(0xFFFCE8E6)
val OnErrorContainerRed = Color(0xFFC5221F)

val ErrorRedDark = Color(0xFFF28B82)
val OnErrorRedDark = Color(0xFF601410)
val ErrorContainerRedDark = Color(0xFF8C1D18)
val OnErrorContainerRedDark = Color(0xFFFCE8E6)

// Neutral / Surface - Light
val BackgroundLight = Color(0xFFF8F9FA)
val OnBackgroundLight = Color(0xFF202124)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF202124)
val SurfaceVariantLight = Color(0xFFF1F3F4)
val OnSurfaceVariantLight = Color(0xFF5F6368)
val OutlineLight = Color(0xFFDADCE0)
val OutlineVariantLight = Color(0xFFE8EAED)

// Neutral / Surface - Dark
val BackgroundDark = Color(0xFF121212)
val OnBackgroundDark = Color(0xFFE8EAED)
val SurfaceDark = Color(0xFF1E1E1E)
val OnSurfaceDark = Color(0xFFE8EAED)
val SurfaceVariantDark = Color(0xFF2D2E30)
val OnSurfaceVariantDark = Color(0xFFBDC1C6)
val OutlineDark = Color(0xFF5F6368)
val OutlineVariantDark = Color(0xFF3C4043)

// Document Type Specific Colors
val DocBlue = Color(0xFF1A73E8)
val SheetGreen = Color(0xFF1E8E3E)
val PresentationOrange = Color(0xFFE37400)
val PdfRed = Color(0xFFD93025)
val UnknownGray = Color(0xFF5F6368)

val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = GreenTertiaryDark,
    onTertiary = GreenOnTertiaryDark,
    tertiaryContainer = GreenTertiaryContainerDark,
    onTertiaryContainer = GreenOnTertiaryContainerDark,
    error = ErrorRedDark,
    onError = OnErrorRedDark,
    errorContainer = ErrorContainerRedDark,
    onErrorContainer = OnErrorContainerRedDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)
