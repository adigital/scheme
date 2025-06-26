package com.vegatel.scheme

import androidx.compose.ui.geometry.Offset
import com.vegatel.scheme.model.SerializableScheme
import com.vegatel.scheme.model.toElementMatrix
import com.vegatel.scheme.model.toSerializableScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.io.File

actual fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectOpenFileDialog() ?: return
    try {
        val text = File(filename).readText()
        // Декодируем полную схему с учетом смещений
        val schemeSerializable = Json.decodeFromString<SerializableScheme>(text)
        val loadedElements = schemeSerializable.matrix.toElementMatrix()
        val loadedSchemeOffset = Offset(
            schemeSerializable.schemeOffset.x,
            schemeSerializable.schemeOffset.y
        )
        val loadedElementOffsets = schemeSerializable.elementOffsets.associate {
            it.id to
                    Offset(it.offset.x, it.offset.y)
        }
        state.value = state.value.copy(
            elements = loadedElements,
            schemeOffset = loadedSchemeOffset,
            elementOffsets = loadedElementOffsets,
            fileName = filename,
            isDirty = false
        )
    } catch (e: Exception) {
        log("App", "Ошибка загрузки файла: $e")
    }
}

actual fun saveElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectSaveFileDialog() ?: return
    try {
        val stateValue = state.value
        // Сериализуем полную схему с учетом смещений
        val schemeSerializable = stateValue.toSerializableScheme()
        File(filename).writeText(
            Json.encodeToString(
                SerializableScheme.serializer(),
                schemeSerializable
            )
        )
        state.value = state.value.copy(fileName = filename, isDirty = false)
    } catch (e: Exception) {
        log("App", "Ошибка сохранения файла: $e")
    }
}