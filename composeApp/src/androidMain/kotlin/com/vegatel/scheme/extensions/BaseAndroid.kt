package com.vegatel.scheme.extensions

import android.content.Context
import android.database.Cursor
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun displayFileName(fileName: String?): String {
    val context = LocalContext.current
    return fileName?.let { context.displayFileNameFromUri(it) } ?: "Новая"
}

fun Context.displayFileNameFromUri(uriString: String): String {
    return if (uriString.startsWith("content://")) {
        try {
            val uri = uriString.toUri()
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) {
                    val name = it.getString(nameIndex)
                    return name.substringBeforeLast('.', name)
                }
            }
            "Файл"
        } catch (e: Exception) {
            "Файл"
        }
    } else {
        // Фоллбэк для обычных путей
        val name = uriString.substringAfterLast('/', uriString)
            .substringAfterLast('\\')
        name.substringBeforeLast('.', name)
    }
}