package com.vegatel.scheme


import com.vegatel.scheme.model.loadElementMatrixFromFile
import kotlinx.coroutines.flow.MutableStateFlow

actual fun openElementMatrixFromDialog(elements: MutableStateFlow<com.vegatel.scheme.model.ElementMatrix>) {
    val filename = selectOpenFileDialog() ?: return
    try {
        elements.value = loadElementMatrixFromFile(filename)
    } catch (e: Exception) {
        log("App", "Ошибка загрузки файла: $e")
    }
}