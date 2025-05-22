package com.vegatel.scheme

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.model.SerializableElementMatrix
import com.vegatel.scheme.model.toElementMatrix
import com.vegatel.scheme.model.toSerializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.BufferedReader

// Open
private var openFileCallback: (() -> Unit)? = null

fun ComponentActivity.registerOpenElementMatrixFromDialog(
    elements: MutableStateFlow<ElementMatrix>
) {
    val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        val serializable = Json.decodeFromString<SerializableElementMatrix>(json)
                        elements.value = serializable.toElementMatrix()
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

actual fun openElementMatrixFromDialog(elements: MutableStateFlow<ElementMatrix>) {
    openFileCallback?.invoke()
}

// Вспомогательная функция для десериализации из строки
fun loadElementMatrixFromFileContent(content: String): ElementMatrix {
    return Json.decodeFromString(
        SerializableElementMatrix.serializer(),
        content
    ).toElementMatrix()
}

// Save
private var saveFileCallback: (() -> Unit)? = null

fun ComponentActivity.registerSaveElementMatrixFromDialog(
    elements: StateFlow<ElementMatrix>
) {
    val saveFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    val serializable = elements.value.toSerializable()
                    val json =
                        Json.encodeToString(SerializableElementMatrix.serializer(), serializable)
                    outputStream?.bufferedWriter()?.use { it.write(json) }
                } catch (e: Exception) {
                    log("App", "Ошибка сохранения файла: $e")
                }
            }
        }

    saveFileCallback = {
        saveFileLauncher.launch("elements.json")
    }
}

actual fun saveElementMatrixFromDialog(elements: StateFlow<ElementMatrix>) {
    saveFileCallback?.invoke()
}