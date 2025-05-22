package com.vegatel.scheme

import java.awt.FileDialog
import java.awt.Frame

fun selectOpenFileDialog(title: String = "Открыть файл схемы"): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    return dialog.file?.let { fileName ->
        dialog.directory?.let { dir -> "$dir$fileName" }
    }
}