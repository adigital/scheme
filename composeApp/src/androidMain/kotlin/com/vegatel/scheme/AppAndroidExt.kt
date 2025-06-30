package com.vegatel.scheme

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.vegatel.scheme.extensions.displayFileNameFromUri
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
                            // Декодируем полную схему, включая зумы и фон
                            val schemeSerializable = Json.decodeFromString<SerializableScheme>(json)
                            val loadedElements = schemeSerializable.matrix.toElementMatrix()
                            val loadedSchemeOffset = Offset(
                                schemeSerializable.schemeOffset.x,
                                schemeSerializable.schemeOffset.y
                            )
                            val loadedElementOffsets = schemeSerializable.elementOffsets.associate {
                                it.id to Offset(it.offset.x, it.offset.y)
                            }
                            // Сохраняем новое состояние, включая зумы и имя фона
                            var newState = openFileState?.value?.copy(
                                elements = loadedElements,
                                schemeOffset = loadedSchemeOffset,
                                elementOffsets = loadedElementOffsets,
                                fileName = uri.toString(),
                                isDirty = false,
                                schemeScale = schemeSerializable.schemeScale,
                                backgroundFileName = schemeSerializable.backgroundFileName,
                                backgroundScale = schemeSerializable.backgroundScale
                            ) ?: return@registerForActivityResult
                            // Автозагрузка фонового PDF, если задан
                            schemeSerializable.backgroundFileName?.let { bfn ->
                                try {
                                    val uriBg = bfn.toUri()
                                    contentResolver.openFileDescriptor(uriBg, "r")?.use { pfd ->
                                        PdfRenderer(pfd).use { renderer ->
                                            renderer.openPage(0).use { page ->
                                                val bitmap = createBitmap(page.width, page.height)
                                                page.render(
                                                    bitmap,
                                                    null,
                                                    null,
                                                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                                                )
                                                newState =
                                                    newState.copy(background = bitmap.asImageBitmap())
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    log("App", "Ошибка загрузки подложки: $e")
                                }
                            }
                            openFileState?.value = newState
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

// --- Поддержка PDF-подложки ---
private var openBackgroundState: MutableStateFlow<SchemeState>? = null
private var openBackgroundCallback: (() -> Unit)? = null

fun ComponentActivity.registerOpenBackgroundFromDialog(state: MutableStateFlow<SchemeState>) {
    openBackgroundState = state
    val openPdfLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val pfd = contentResolver.openFileDescriptor(uri, "r")
                        ?: return@registerForActivityResult
                    val renderer = PdfRenderer(pfd)
                    val page = renderer.openPage(0)
                    val width = page.width
                    val height = page.height
                    val bitmap = createBitmap(width, height)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()
                    renderer.close()
                    pfd.close()
                    val imageBitmap = bitmap.asImageBitmap()
                    // Обновляем подложку и сохраняем имя файла подложки вместо URI
                    val backgroundName = displayFileNameFromUri(uri.toString())
                    openBackgroundState?.value = openBackgroundState?.value
                        ?.copy(
                            background = imageBitmap,
                            backgroundFileName = backgroundName,
                            isDirty = true
                        )
                        ?: return@registerForActivityResult
                } catch (e: Exception) {
                    log("App", "Ошибка открытия PDF: $e")
                }
            }
        }
    openBackgroundCallback = {
        openPdfLauncher.launch(arrayOf("application/pdf"))
    }
}

actual fun openBackgroundFromDialog(state: MutableStateFlow<SchemeState>) {
    openBackgroundState = state
    openBackgroundCallback?.invoke()
}

// Сохраняет полную схему (включая зум и подложку). Для Android перенаправляем на SaveAs диалог.
actual fun saveSchemeToFile(state: MutableStateFlow<SchemeState>) {
    // Используем SaveAs диалог для сохранения
    saveFileState = state
    saveFileCallback?.invoke()
}