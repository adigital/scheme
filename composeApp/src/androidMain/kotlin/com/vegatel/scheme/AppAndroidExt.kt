package com.vegatel.scheme

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
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
                            var newState = state.value.copy(
                                elements = loadedElements,
                                schemeOffset = loadedSchemeOffset,
                                elementOffsets = loadedElementOffsets,
                                fileName = uri.toString(),
                                isDirty = false,
                                // Очищаем старую подложку перед загрузкой новой
                                background = null,
                                schemeScale = schemeSerializable.schemeScale,
                                backgroundFileName = schemeSerializable.backgroundFileName,
                                backgroundScale = schemeSerializable.backgroundScale,
                                baseStationSignal = schemeSerializable.baseStationSignal
                            )
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
    val openBgLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val mime = contentResolver.getType(uri) ?: ""
                    val rawBitmap: android.graphics.Bitmap? = when {
                        mime == "application/pdf" || uri.toString().endsWith(".pdf", true) -> {
                            val pfd = contentResolver.openFileDescriptor(uri, "r")
                                ?: return@registerForActivityResult
                            val renderer = PdfRenderer(pfd)
                            val page = renderer.openPage(0)
                            val bmp = createBitmap(page.width, page.height)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            page.close(); renderer.close(); pfd.close()
                            bmp
                        }

                        mime.startsWith("image/") || uri.toString()
                            .matches(Regex(".*\\.(png|jpg|jpeg)", RegexOption.IGNORE_CASE)) -> {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                android.graphics.BitmapFactory.decodeStream(stream)
                            }
                        }

                        else -> null
                    }

                    rawBitmap ?: return@registerForActivityResult

                    val maxDim = AppConfig.MAX_BACKGROUND_DIM
                    val bitmap = if (rawBitmap.width > maxDim || rawBitmap.height > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height)
                        val newW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                        val newH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                        rawBitmap.scale(newW, newH)
                    } else rawBitmap

                    val imageBitmap = bitmap.asImageBitmap()

                    // Получаем название файла
                    val backgroundName = contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                    } ?: uri.lastPathSegment ?: "Файл"

                    openBackgroundState?.value = openBackgroundState?.value?.copy(
                        background = imageBitmap,
                        backgroundFileName = backgroundName,
                        isDirty = true
                    ) ?: return@registerForActivityResult
                } catch (e: Exception) {
                    log("App", "Ошибка открытия подложки: $e")
                }
            }
        }

    openBackgroundCallback = {
        openBgLauncher.launch(arrayOf("application/pdf", "image/png", "image/jpeg"))
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

// Export
private var exportCallback: (() -> Unit)? = null

fun ComponentActivity.registerExportSchemeToPdfFromDialog() {
    val createPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
            if (uri != null) {
                try {
                    ExportFlag.isExporting = true
                    val rect = ExportArea.rect ?: run {
                        log("App", "Неизвестна область схемы для экспорта")
                        return@registerForActivityResult
                    }
                    // Рисуем текущий ComposeView и обрезаем по области экспорта
                    val rootContent =
                        window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
                    val composeView = rootContent?.getChildAt(0) ?: window.decorView
                    val fullWidth = composeView.width
                    val fullHeight = composeView.height
                    val fullBitmap = createBitmap(fullWidth, fullHeight)
                    val canvas = android.graphics.Canvas(fullBitmap)
                    composeView.draw(canvas)
                    val x = rect.left.toInt().coerceAtLeast(0)
                    val y = rect.top.toInt().coerceAtLeast(0)
                    val w = rect.width.toInt().coerceAtMost(fullWidth - x)
                    val h = rect.height.toInt().coerceAtMost(fullHeight - y)
                    val schemeBitmap = android.graphics.Bitmap.createBitmap(fullBitmap, x, y, w, h)

                    // Создаём PDF из обрезанного Bitmap
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                        w,
                        h,
                        1
                    ).create()
                    val page = pdfDoc.startPage(pageInfo)
                    page.canvas.drawBitmap(schemeBitmap, 0f, 0f, null)
                    pdfDoc.finishPage(page)
                    contentResolver.openOutputStream(uri)?.use { out ->
                        pdfDoc.writeTo(out)
                    }
                    pdfDoc.close()
                } catch (e: Exception) {
                    log("App", "Ошибка экспорта в PDF: $e")
                } finally {
                    ExportFlag.isExporting = false
                }
            }
        }
    exportCallback = {
        ExportFlag.isExporting = true
        createPdfLauncher.launch("Схема.pdf")
    }
}

actual fun exportSchemeToPdfFromDialog(state: MutableStateFlow<SchemeState>) {
    exportCallback?.invoke()
}