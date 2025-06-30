package com.vegatel.scheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.vegatel.scheme.model.SerializableScheme
import com.vegatel.scheme.model.toElementMatrix
import com.vegatel.scheme.model.toSerializableScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File

actual fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectOpenFileDialog() ?: return
    try {
        val text = File(filename).readText()
        val schemeSerializable = Json.decodeFromString<SerializableScheme>(text)
        val loadedElements = schemeSerializable.matrix.toElementMatrix()
        val loadedSchemeOffset = Offset(
            schemeSerializable.schemeOffset.x,
            schemeSerializable.schemeOffset.y
        )
        val loadedElementOffsets = schemeSerializable.elementOffsets.associate {
            it.id to Offset(it.offset.x, it.offset.y)
        }
        // Директория файла схемы
        val jsonDir = File(filename).parentFile
        // Имя и полный путь PDF-файла подложки
        val bgFileName = schemeSerializable.backgroundFileName
        val bgFullPath = bgFileName?.let { jsonDir.resolve(it).absolutePath }
        // Загружаем подложку, если задана
        val bgImage = bgFullPath?.let {
            PDDocument.load(File(it)).use { doc ->
                PDFRenderer(doc).renderImageWithDPI(0, 200f).also { doc.close() }
            }.toComposeImageBitmap()
        }
        state.value = state.value.copy(
            elements = loadedElements,
            schemeOffset = loadedSchemeOffset,
            elementOffsets = loadedElementOffsets,
            fileName = filename,
            isDirty = false,
            schemeScale = schemeSerializable.schemeScale,
            backgroundFileName = bgFullPath,
            backgroundScale = schemeSerializable.backgroundScale,
            background = bgImage
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

/**
 * Открывает PDF-файл и устанавливает первую страницу как подложку схемы.
 */
actual fun openBackgroundFromDialog(state: MutableStateFlow<SchemeState>) {
    val filename = selectOpenPdfDialog() ?: return
    try {
        val document = PDDocument.load(File(filename))
        val renderer = PDFRenderer(document)
        val bufferedImage = renderer.renderImageWithDPI(0, 200f)
        document.close()
        val imageBitmap: ImageBitmap = bufferedImage.toComposeImageBitmap()
        state.value = state.value.copy(
            background = imageBitmap,
            backgroundFileName = File(filename).name,
            isDirty = true
        )
    } catch (e: Exception) {
        log("App", "Ошибка загрузки PDF: $e")
    }
}

/**
 * Сохраняет полную схему (включая зум и подложку) без диалога в существующий файл.
 */
actual fun saveSchemeToFile(state: MutableStateFlow<SchemeState>) {
    val stateValue = state.value
    val filename = stateValue.fileName ?: return
    try {
        val schemeSerializable = stateValue.toSerializableScheme()
        File(filename).writeText(
            Json.encodeToString(SerializableScheme.serializer(), schemeSerializable)
        )
        state.value = state.value.copy(isDirty = false)
    } catch (e: Exception) {
        log("App", "Ошибка сохранения схемы: $e")
    }
}