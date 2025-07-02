package com.vegatel.scheme

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.vegatel.scheme.model.SerializableScheme
import com.vegatel.scheme.model.toElementMatrix
import com.vegatel.scheme.model.toSerializableScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage
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
                PDFRenderer(doc).renderImageWithDPI(0, 72f).also { doc.close() }
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
        val bufferedImage = renderer.renderImageWithDPI(0, 72f)
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

/**
 * Открывает диалог сохранения PDF и сохраняет текущую схему (вместе с подложкой) как изображение в PDF.
 */
actual fun exportSchemeToPdfFromDialog(state: MutableStateFlow<SchemeState>) {
    ExportFlag.isExporting = true
    val filename = selectSavePdfDialog("Экспорт схемы в PDF") ?: run {
        ExportFlag.isExporting = false
        return
    }
    try {
        val rect = ExportArea.rect ?: run {
            log("App", "Неизвестна область схемы для экспорта")
            return
        }
        val window =
            java.awt.Frame.getFrames().firstOrNull { it.isActive } ?: java.awt.Frame.getFrames()
                .firstOrNull()
            ?: return

        // Ищем SkiaLayer тремя способами: отражением из ComposeWindow, прямым кастом компонента, рекурсивным обходом
        val skiaLayer: org.jetbrains.skiko.SkiaLayer? = when (window) {
            is ComposeWindow -> {
                // Пытаемся достать приватное поле/свойство "layer" рефлексией (в разных версиях Compose оно присутствует)
                val byReflection = runCatching {
                    val field = ComposeWindow::class.java.getDeclaredField("layer")
                    field.isAccessible = true
                    field.get(window) as? org.jetbrains.skiko.SkiaLayer
                }.getOrNull()

                byReflection ?: findSkiaLayer(window)
            }

            else -> findSkiaLayer(window)
        }

        val fullImage: BufferedImage = skiaLayer?.let { layer ->
            // Переключаем рендер в SOFTWARE, иначе screenshot() может вернуть null
            val prevApi = layer.renderApi
            if (prevApi != GraphicsApi.SOFTWARE_FAST) {
                layer.renderApi = GraphicsApi.SOFTWARE_FAST
                layer.needRedraw()
                java.awt.Toolkit.getDefaultToolkit().sync()
            }

            val desiredW = rect.width.toInt()
            val desiredH = rect.height.toInt()

            // также увеличиваем окно, чтобы GL viewport стал нужного размера
            val oldBounds = window.bounds
            if (desiredW > oldBounds.width || desiredH > oldBounds.height) {
                window.setSize(desiredW, desiredH)
                window.validate()
                window.doLayout()
                layer.setSize(desiredW, desiredH)
                layer.needRedraw()
                java.awt.Toolkit.getDefaultToolkit().sync()
            }

            val img = layer.screenshot()!!.toBufferedImage()

            // Сохраняем исходный размер
            val oldW = layer.width
            val oldH = layer.height

            var needResize = desiredW > oldW || desiredH > oldH
            if (needResize) {
                layer.setSize(desiredW, desiredH)
                layer.needRedraw()
                // ждём прорисовку (минимальная синхронизация OpenGL);
                java.awt.Toolkit.getDefaultToolkit().sync()
            }

            // Возвращаем исходный размер, если меняли
            if (needResize) {
                layer.setSize(oldW, oldH)
                layer.needRedraw()
            }

            // возвращаем старый размер окна
            if (desiredW > oldBounds.width || desiredH > oldBounds.height) {
                window.bounds = oldBounds
                window.validate()
                window.doLayout()
            }

            // Возвращаем прошлый API, если меняли
            if (prevApi != GraphicsApi.SOFTWARE_FAST) {
                layer.renderApi = prevApi
                layer.needRedraw()
            }

            img
        } ?: run {
            // Fallback: делаем скриншот окна, если слой не найден
            val wb = window.bounds
            val robot = java.awt.Robot(window.graphicsConfiguration.device)
            robot.createScreenCapture(wb)
        }

        // Обрезаем до нужного прямоугольника экспорта
        val x = rect.left.toInt().coerceAtLeast(0)
        val y = rect.top.toInt().coerceAtLeast(0)
        val w = rect.width.toInt().coerceAtMost(fullImage.width - x)
        val h = rect.height.toInt().coerceAtMost(fullImage.height - y)

        val schemeImage = fullImage.getSubimage(x, y, w, h)

        val document = PDDocument()
        val page = PDPage(PDRectangle(w.toFloat(), h.toFloat()))
        document.addPage(page)
        val pdImage = LosslessFactory.createFromImage(document, schemeImage)
        PDPageContentStream(document, page).use { stream ->
            stream.drawImage(pdImage, 0f, 0f, w.toFloat(), h.toFloat())
        }
        document.save(filename)
        document.close()
    } catch (e: Exception) {
        log("App", "Ошибка экспорта в PDF: $e")
    } finally {
        ExportFlag.isExporting = false
    }
}

private fun findSkiaLayer(component: java.awt.Component): org.jetbrains.skiko.SkiaLayer? {
    if (component is org.jetbrains.skiko.SkiaLayer) return component
    if (component is java.awt.Container) {
        component.components.forEach { child ->
            val res = findSkiaLayer(child)
            if (res != null) return res
        }
    }
    return null
}