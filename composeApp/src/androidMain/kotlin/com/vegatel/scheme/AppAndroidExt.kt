package com.vegatel.scheme

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Offset
import com.vegatel.scheme.model.SerializableScheme
import com.vegatel.scheme.model.toElementMatrix
import com.vegatel.scheme.model.toSerializableScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

// Open
private var openFileCallback: (() -> Unit)? = null
private var openFileState: MutableStateFlow<SchemeState>? = null

fun ComponentActivity.registerOpenElementMatrixFromDialog(
    state: MutableStateFlow<SchemeState>
) {
    openFileState = state
    val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        try {
                            // Декодируем полную схему с учетом смещений
                            val schemeSerializable = Json.decodeFromString<SerializableScheme>(json)
                            val loadedElements = schemeSerializable.matrix.toElementMatrix()
                            val loadedSchemeOffset = Offset(
                                schemeSerializable.schemeOffset.x,
                                schemeSerializable.schemeOffset.y
                            )
                            val loadedElementOffsets = schemeSerializable.elementOffsets.associate {
                                it.id to
                                        Offset(it.offset.x, it.offset.y)
                            }
                            openFileState?.value = openFileState?.value?.copy(
                                elements = loadedElements,
                                schemeOffset = loadedSchemeOffset,
                                elementOffsets = loadedElementOffsets,
                                fileName = uri.toString(),
                                isDirty = false
                            ) ?: return@registerForActivityResult
                        } catch (e: Exception) {
                            log("App", "Ошибка открытия файла: $e")
                        }
                    }
                } catch (e: Exception) {
                    log("App", "Ошибка открытия файла: $e")
                }
            }
        }
    openFileCallback = {
        openFileLauncher.launch(arrayOf("application/json"))
    }
}

actual fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    openFileState = state
    openFileCallback?.invoke()
}

// Save
private var saveFileState: MutableStateFlow<SchemeState>? = null
private var saveFileCallback: (() -> Unit)? = null

fun ComponentActivity.registerSaveElementMatrixFromDialog(
    state: MutableStateFlow<SchemeState>
) {
    saveFileState = state
    val saveFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                try {
                    val outputStream = contentResolver.openOutputStream(uri, "wt")
                    val stateValue = saveFileState?.value ?: return@registerForActivityResult
                    // Сериализуем всю схему с учетом смещений
                    val schemeSerializable = stateValue.toSerializableScheme()
                    val json =
                        Json.encodeToString(SerializableScheme.serializer(), schemeSerializable)
                    outputStream?.bufferedWriter()
                        ?.use { writer -> writer.write(json) }
                    // После успешного сохранения обновляем fileName и isDirty
                    saveFileState?.value = saveFileState?.value?.copy(
                        fileName = uri.toString(),
                        isDirty = false
                    ) ?: return@registerForActivityResult
                } catch (e: Exception) {
                    log("App", "Ошибка сохранения файла: $e")
                }
            }
        }

    saveFileCallback = {
        saveFileLauncher.launch("Схема.json")
    }
}

actual fun saveElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    saveFileState = state
    saveFileCallback?.invoke()
}