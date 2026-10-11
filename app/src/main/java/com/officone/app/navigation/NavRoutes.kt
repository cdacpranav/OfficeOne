package com.officone.app.navigation

import android.net.Uri
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class NavRoutes(val route: String) {

    data object Home : NavRoutes("home")

    data object Settings : NavRoutes("settings")

    data object About : NavRoutes("about")

    data object DocumentEditor : NavRoutes("editor/doc/{encodedUri}?mimeType={mimeType}") {
        fun createRoute(uri: String, mimeType: String): String {
            val encoded = encodeParam(uri)
            val encodedMime = encodeParam(mimeType)
            return "editor/doc/$encoded?mimeType=$encodedMime"
        }
    }

    data object SpreadsheetEditor : NavRoutes("editor/sheet/{encodedUri}?mimeType={mimeType}") {
        fun createRoute(uri: String, mimeType: String): String {
            val encoded = encodeParam(uri)
            val encodedMime = encodeParam(mimeType)
            return "editor/sheet/$encoded?mimeType=$encodedMime"
        }
    }

    data object PresentationEditor : NavRoutes("editor/slides/{encodedUri}?mimeType={mimeType}") {
        fun createRoute(uri: String, mimeType: String): String {
            val encoded = encodeParam(uri)
            val encodedMime = encodeParam(mimeType)
            return "editor/slides/$encoded?mimeType=$encodedMime"
        }
    }

    data object PdfViewer : NavRoutes("pdf/{encodedUri}") {
        fun createRoute(uri: String): String {
            val encoded = encodeParam(uri)
            return "pdf/$encoded"
        }
    }

    companion object {
        fun encodeParam(param: String): String {
            return Uri.encode(param)
        }

        fun decodeParam(param: String): String {
            return Uri.decode(param)
        }
    }
}
