package com.vegatel.scheme

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.vegatel.scheme.model.toElementMatrix
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.BufferedReader

private var openFileCallback: ((String) -> Unit)? = null

fun ComponentActivity.registerOpenElementMatrixFromDialog(
    elements: MutableStateFlow<com.vegatel.scheme.model.ElementMatrix>
) {
    val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val text = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                    // Используем специальную функцию для загрузки из строки
                    elements.value = loadElementMatrixFromFileContent(text)
                } catch (e: Exception) {
                    log("App", "Ошибка загрузки файла: $e")
                }
            }
        }

    openFileCallback =
        { openFileLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
}

actual fun openElementMatrixFromDialog(elements: MutableStateFlow<com.vegatel.scheme.model.ElementMatrix>) {
    openFileCallback?.invoke("")
}

// Вспомогательная функция для десериализации из строки
fun loadElementMatrixFromFileContent(content: String): com.vegatel.scheme.model.ElementMatrix {
    return kotlinx.serialization.json.Json.decodeFromString(
        com.vegatel.scheme.model.SerializableElementMatrix.serializer(),
        content
    ).toElementMatrix()
}
