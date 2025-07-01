package com.vegatel.scheme

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

fun selectOpenFileDialog(title: String = "Открыть файл схемы"): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.file = "*.json"
    dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
    dialog.isVisible = true
    return dialog.file?.let { fileName ->
        dialog.directory?.let { dir -> "$dir$fileName" }
    }
}

fun selectSaveFileDialog(title: String = "Сохранить файл схемы"): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = "*.json"
    dialog.isVisible = true
    val fileName = dialog.file?.let {
        if (it.lowercase().endsWith(".json")) it else "$it.json"
    }
    return fileName?.let { dialog.directory?.let { dir -> "$dir$it" } }
}

/**
 * Открывает диалог выбора PDF-файла для подложки.
 */
fun selectOpenPdfDialog(title: String = "Выбрать подложку"): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.file = "*.pdf"
    dialog.filenameFilter = FilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
    dialog.isVisible = true
    return dialog.file?.let { fileName ->
        dialog.directory?.let { dir -> "${dir}$fileName" }
    }
}

/**
 * Открывает диалог сохранения PDF-файла.
 */
fun selectSavePdfDialog(title: String = "Сохранить PDF"): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = "*.pdf"
    dialog.isVisible = true
    val fileName = dialog.file?.let {
        if (it.lowercase().endsWith(".pdf")) it else "$it.pdf"
    }
    return fileName?.let { dialog.directory?.let { dir -> "$dir$it" } }
}