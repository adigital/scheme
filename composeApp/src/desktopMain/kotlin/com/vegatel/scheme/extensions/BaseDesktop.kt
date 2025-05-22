package com.vegatel.scheme.extensions

import androidx.compose.runtime.Composable

@Composable
actual fun displayFileName(fileName: String?): String {
    return fileName?.let {
        val name = it.substringAfterLast('/', it)
            .substringAfterLast('\\')
        name.substringBeforeLast('.', name)
    } ?: "Новая"
}