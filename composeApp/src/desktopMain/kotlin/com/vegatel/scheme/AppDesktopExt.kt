package com.vegatel.scheme


import com.vegatel.scheme.model.loadElementMatrixFromFile
import com.vegatel.scheme.model.saveElementMatrixToFile
import kotlinx.coroutines.flow.MutableStateFlow

actual fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectOpenFileDialog() ?: return
    try {
        val elements = loadElementMatrixFromFile(filename)
        state.value = state.value.copy(elements = elements, fileName = filename, isDirty = false)
    } catch (e: Exception) {
        log("App", "Ошибка загрузки файла: $e")
    }
}

actual fun saveElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectSaveFileDialog() ?: return
    try {
        saveElementMatrixToFile(state.value.elements, filename)
        state.value = state.value.copy(fileName = filename, isDirty = false)
    } catch (e: Exception) {
        log("App", "Ошибка сохранения файла: $e")
    }
}