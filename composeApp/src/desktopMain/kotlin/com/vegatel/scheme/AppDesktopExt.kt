package com.vegatel.scheme


import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.model.loadElementMatrixFromFile
import com.vegatel.scheme.model.saveElementMatrixToFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun openElementMatrixFromDialog(elements: MutableStateFlow<ElementMatrix>) {
    val filename = selectOpenFileDialog() ?: return
    try {
        elements.value = loadElementMatrixFromFile(filename)
    } catch (e: Exception) {
        log("App", "Ошибка загрузки файла: $e")
    }
}

actual fun saveElementMatrixFromDialog(elements: StateFlow<ElementMatrix>) {
    val filename = selectSaveFileDialog() ?: return
    try {
        saveElementMatrixToFile(elements.value, filename)
    } catch (e: Exception) {
        log("App", "Ошибка сохранения файла: $e")
    }
}