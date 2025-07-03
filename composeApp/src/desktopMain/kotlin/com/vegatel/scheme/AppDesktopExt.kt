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
import org.jetbrains.skiko.toBufferedImage
import java.awt.RenderingHints
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
        val bgImage = bgFullPath?.let { path ->
            val ext = File(path).extension.lowercase()
            val buffered: BufferedImage? = when (ext) {
                "pdf" -> PDDocument.load(File(path)).use { doc ->
                    PDFRenderer(doc).renderImageWithDPI(0, 72f)
                }

                "jpg", "jpeg", "png" -> javax.imageio.ImageIO.read(File(path))
                else -> null
            }
            buffered?.downscaleIfLarge()?.toComposeImageBitmap()
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
        val ext = File(filename).extension.lowercase()
        val allowed = setOf("pdf", "jpg", "jpeg", "png")
        if (ext !in allowed) {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "Поддерживаются только PDF, JPG, PNG",
                "Неподдерживаемый файл",
                javax.swing.JOptionPane.WARNING_MESSAGE
            )
            return
        }

        val bufferedRaw: BufferedImage = when (ext) {
            "pdf" -> {
                PDDocument.load(File(filename)).use { doc ->
                    PDFRenderer(doc).renderImageWithDPI(0, 72f)
                }
            }

            else -> {
                javax.imageio.ImageIO.read(File(filename))
                    ?: throw IllegalArgumentException("Не удалось прочитать изображение $filename")
            }
        }

        val bufferedImage = bufferedRaw.downscaleIfLarge()
        val imageBitmap: ImageBitmap = bufferedImage.toComposeImageBitmap()
        state.value = state.value.copy(
            background = imageBitmap,
            backgroundFileName = File(filename).name,
            isDirty = true
        )
    } catch (e: Exception) {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Ошибка загрузки подложки: ${'$'}e",
            "Ошибка",
            javax.swing.JOptionPane.ERROR_MESSAGE
        )
        log("App", "Ошибка загрузки подложки: $e")
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

        val fullImage: BufferedImage = skiaLayer
            ?.screenshot()
            ?.toBufferedImage()
            ?: run {
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

private fun BufferedImage.downscaleIfLarge(maxDim: Int = AppConfig.MAX_BACKGROUND_DIM): BufferedImage {
    if (this.width <= maxDim && this.height <= maxDim) return this
    val scale = maxDim.toDouble() / maxOf(this.width, this.height)
    val newW = (this.width * scale).toInt().coerceAtLeast(1)
    val newH = (this.height * scale).toInt().coerceAtLeast(1)
    val scaled = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR
    )
    g.drawImage(this, 0, 0, newW, newH, null)
    g.dispose()
    return scaled
}