package com.vegatel.scheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.extensions.toPx
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.model.REPEATER_ID
import com.vegatel.scheme.ui.components.SchemeConstructor
import com.vegatel.scheme.ui.views.MainMenu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import scheme.composeapp.generated.resources.Res
import scheme.composeapp.generated.resources.zoom_in
import scheme.composeapp.generated.resources.zoom_out
import kotlin.math.min

// Elements
fun buildElementMatrix(
    rows: Int,
    cols: Int,
    builder: ElementMatrix.() -> Unit
): ElementMatrix {
    return ElementMatrix(initialRows = rows, initialCols = cols).apply(builder)
}

val initialElements = buildElementMatrix(rows = 3, cols = 1) {
    this[0, 0] = Antenna(
        id = 1,
        signalPower = 11.0,
        endElementId = REPEATER_ID
    )

    this[1, 0] = Repeater(
        endElementId = 2
    )

    this[2, 0] = Antenna(
        id = 2,
        signalPower = 9.0,
        endElementId = REPEATER_ID
    )
}

// SchemeState
data class SchemeState(
    val elements: ElementMatrix,
    val fileName: String? = null,
    val isDirty: Boolean = false,
    val baseStationSignal: Double = AppConfig.DEFAULT_BASE_STATION_SIGNAL_DBM,
    val frequency: Int = 800,
    val schemeOffset: Offset = Offset.Zero,
    val elementOffsets: Map<Int, Offset> = emptyMap(),
    val background: ImageBitmap? = null,
    val schemeScale: Float = 1f,
    val backgroundScale: Float = 1f,
    val backgroundFileName: String? = null,
    val considerAntennaGain: Boolean = AppSettings.considerAntennaGain
)

val initialSchemeState = SchemeState(
    elements = initialElements,
    fileName = null,
    isDirty = false,
    baseStationSignal = AppConfig.DEFAULT_BASE_STATION_SIGNAL_DBM,
    frequency = 800,
    considerAntennaGain = AppSettings.considerAntennaGain
)

class AppState {
    private val _schemeState = MutableStateFlow(initialSchemeState)
    val schemeState: StateFlow<SchemeState> = _schemeState.asStateFlow()

    // History management
    private val _history = mutableListOf<SchemeState>()
    private var _historyIndex = -1

    init {
        addToHistory(initialSchemeState)
    }

    internal fun addToHistory(state: SchemeState) {
        // Remove all states after current index
        while (_history.size > _historyIndex + 1) {
            _history.removeLast()
        }

        // Add new state
        _history.add(state)

        // Keep history size limited
        if (_history.size > MAX_HISTORY_SIZE) {
            _history.removeFirst()
        } else {
            _historyIndex++
        }
    }

    fun canUndo(): Boolean = _historyIndex > 0
    fun canRedo(): Boolean = _historyIndex < _history.size - 1

    fun undo() {
        if (canUndo()) {
            _historyIndex--
            _schemeState.value = _history[_historyIndex]
        }
    }

    fun redo() {
        if (canRedo()) {
            _historyIndex++
            _schemeState.value = _history[_historyIndex]
        }
    }

    fun updateState(newState: SchemeState) {
        _schemeState.value = newState
        addToHistory(newState)
    }

    fun resetState() {
        _schemeState.value = initialSchemeState
        clearHistory()
        addToHistory(initialSchemeState)
    }

    internal fun clearHistory() {
        _history.clear()
        _historyIndex = -1
    }

    // Добавляем геттер для _schemeState
    val mutableSchemeState: MutableStateFlow<SchemeState>
        get() = _schemeState
}

private const val MAX_HISTORY_SIZE = 100

private val appState = AppState()

@Composable
@Preview
fun App() {
    MaterialTheme {
        val schemeState by appState.schemeState.collectAsState()
        // пользовательский масштаб схемы
        val scale = schemeState.schemeScale
        // пользовательский масштаб подложки
        val bgScale = schemeState.backgroundScale
        // автоматический масштаб подложки (и схемы) при изменении размера экрана
        var fitScale by remember { mutableStateOf(1f) }
        // счётчик версии для сброса положения при повторном рендере
        var schemeVersion by remember { mutableStateOf(0) }

        // Диалог выбора подложки при открытии схемы
        var showBackgroundPrompt by remember { mutableStateOf(false) }
        LaunchedEffect(schemeState.backgroundFileName) {
            if (schemeState.backgroundFileName != null && schemeState.background == null) {
                showBackgroundPrompt = true
            }
        }

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize()
            ) {
                MainMenu(
                    fileName = schemeState.fileName,
                    isDirty = schemeState.isDirty,
                    canUndo = appState.canUndo(),
                    canRedo = appState.canRedo(),
                    baseStationSignal = schemeState.baseStationSignal,
                    frequency = schemeState.frequency,
                    showExport = schemeState.background != null,
                    considerAntennaGain = schemeState.considerAntennaGain,
                    onToggleConsiderAntennaGain = {
                        val newValue = !schemeState.considerAntennaGain
                        AppSettings.considerAntennaGain = newValue
                        appState.updateState(schemeState.copy(considerAntennaGain = newValue))
                    },
                    onFrequencyChange = { newFreq ->
                        appState.updateState(schemeState.copy(frequency = newFreq))
                    },
                    onBaseStationSignalChange = { newSignal ->
                        appState.updateState(schemeState.copy(baseStationSignal = newSignal))
                    },
                    onNew = {
                        appState.resetState()
                        schemeVersion++
                    },
                    onOpen = {
                        openElementMatrixFromDialog(appState.mutableSchemeState)
                        appState.clearHistory()
                        appState.addToHistory(appState.schemeState.value)
                        schemeVersion++
                    },
                    onSave = {
                        if (schemeState.fileName == null) {
                            // Новая схема: показываем SaveAs диалог
                            saveElementMatrixFromDialog(appState.mutableSchemeState)
                        } else if (getPlatform() == "Desktop") {
                            // Сохранение без диалога на Desktop
                            saveSchemeToFile(appState.mutableSchemeState)
                        } else {
                            // На других платформах используем диалог SaveAs
                            saveElementMatrixFromDialog(appState.mutableSchemeState)
                        }
                        appState.addToHistory(appState.schemeState.value)
                    },
                    onSaveAs = {
                        saveElementMatrixFromDialog(appState.mutableSchemeState)
                        appState.addToHistory(appState.schemeState.value)
                    },
                    onLoadBackground = { openBackgroundFromDialog(appState.mutableSchemeState) },
                    onExport = { exportSchemeToPdfFromDialog(appState.mutableSchemeState) },
                    onUndo = { appState.undo() },
                    onRedo = { appState.redo() }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (schemeState.background != null) Color.Gray else Color.White)
                ) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(2f, LocalDensity.current.fontScale)
                    ) {
                        schemeState.background?.let { bmp ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { coords ->
                                        val containerPxWidth = coords.size.width.toFloat()
                                        val containerPxHeight = coords.size.height.toFloat()
                                        val bmpPxWidth = bmp.width.toFloat()
                                        val bmpPxHeight = bmp.height.toFloat()
                                        // пересчитываем автоматический масштаб для подложки при изменении размеров контейнера
                                        fitScale = min(
                                            containerPxWidth / bmpPxWidth,
                                            containerPxHeight / bmpPxHeight
                                        )
                                        val effectiveScale = fitScale * bgScale
                                        val pos = coords.positionInWindow()
                                        val left = pos.x
                                        val top = pos.y
                                        val right = pos.x + bmpPxWidth * effectiveScale
                                        val bottom = pos.y + bmpPxHeight * effectiveScale
                                        ExportArea.rect = Rect(left, top, right, bottom)
                                    }
                            ) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.TopStart,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = bgScale,
                                            scaleY = bgScale,
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        )
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            // применяем автоматический fitScale и пользовательский zoom-scheme
                                            scaleX = fitScale * scale,
                                            scaleY = fitScale * scale,
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        )
                                ) {
                                    // шаги ячейки в пикселях для текущей Density (2f)
                                    val dxPx = 96.dp.toPx()
                                    val dyPx = 128.dp.toPx()

                                    SchemeConstructor(
                                        elements = schemeState.elements,
                                        schemeOffset = schemeState.schemeOffset,
                                        elementOffsets = schemeState.elementOffsets,
                                        onSchemeOffsetChange = { newOffset ->
                                            appState.updateState(
                                                schemeState.copy(
                                                    schemeOffset = newOffset,
                                                    isDirty = true
                                                )
                                            )
                                        },
                                        onElementOffsetChange = { id, offset ->
                                            val newOffsets =
                                                schemeState.elementOffsets.toMutableMap()
                                                    .apply { put(id, offset) }
                                            appState.updateState(
                                                schemeState.copy(
                                                    elementOffsets = newOffsets,
                                                    isDirty = true
                                                )
                                            )
                                        },
                                        onElementsChange = { newElements ->
                                            val isDirty =
                                                newElements != schemeState.elements || schemeState.isDirty.not()

                                            // Если схема или хотя бы один элемент уже сдвинуты, компенсируем смещения
                                            val updatedOffsets =
                                                if (schemeState.elementOffsets.isNotEmpty() || schemeState.schemeOffset != Offset.Zero) {
                                                    compensateOffsets(
                                                        schemeState.elements,
                                                        newElements,
                                                        schemeState.elementOffsets,
                                                        dxPx,
                                                        dyPx
                                                    )
                                                } else schemeState.elementOffsets

                                            val newState =
                                                schemeState.copy(
                                                    elements = newElements,
                                                    elementOffsets = updatedOffsets,
                                                    isDirty = isDirty
                                                )
                                            appState.updateState(newState)
                                        },
                                        baseStationSignal = schemeState.baseStationSignal,
                                        frequency = schemeState.frequency,
                                        considerAntennaGain = schemeState.considerAntennaGain,
                                        resetKey = schemeVersion
                                    )
                                }
                            }
                        } ?: Box(
                            Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    transformOrigin = TransformOrigin(0f, 0f)
                                )
                        ) {
                            // вычисляем dx/dy для «обычной» Density
                            val dxPx2 = 96.dp.toPx()
                            val dyPx2 = 128.dp.toPx()

                            SchemeConstructor(
                                elements = schemeState.elements,
                                schemeOffset = schemeState.schemeOffset,
                                elementOffsets = schemeState.elementOffsets,
                                onSchemeOffsetChange = { newOffset ->
                                    appState.updateState(
                                        schemeState.copy(
                                            schemeOffset = newOffset,
                                            isDirty = true
                                        )
                                    )
                                },
                                onElementOffsetChange = { id, offset ->
                                    val newOffsets =
                                        schemeState.elementOffsets.toMutableMap()
                                            .apply { put(id, offset) }
                                    appState.updateState(
                                        schemeState.copy(
                                            elementOffsets = newOffsets,
                                            isDirty = true
                                        )
                                    )
                                },
                                onElementsChange = { newElements ->
                                    val isDirty =
                                        newElements != schemeState.elements || schemeState.isDirty.not()

                                    val updatedOffsets =
                                        if (schemeState.elementOffsets.isNotEmpty() || schemeState.schemeOffset != Offset.Zero) {
                                            compensateOffsets(
                                                schemeState.elements,
                                                newElements,
                                                schemeState.elementOffsets,
                                                dxPx2,
                                                dyPx2
                                            )
                                        } else schemeState.elementOffsets

                                    val newState =
                                        schemeState.copy(
                                            elements = newElements,
                                            elementOffsets = updatedOffsets,
                                            isDirty = isDirty
                                        )
                                    appState.updateState(newState)
                                },
                                baseStationSignal = schemeState.baseStationSignal,
                                frequency = schemeState.frequency,
                                considerAntennaGain = schemeState.considerAntennaGain,
                                resetKey = schemeVersion
                            )
                        }
                    }
                }
            }

            // Zoom controls
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                // Zoom controls for scheme
                if (!ExportFlag.isExporting) {
                    Column(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (schemeState.schemeScale < 4f) {
                            FloatingActionButton(onClick = {
                                appState.updateState(
                                    schemeState.copy(
                                        schemeScale = schemeState.schemeScale + 0.25f,
                                        isDirty = true
                                    )
                                )
                            }, backgroundColor = MaterialTheme.colors.primary) {
                                Icon(
                                    painter = painterResource(Res.drawable.zoom_in),
                                    contentDescription = "Zoom In"
                                )
                            }
                        }

                        if (schemeState.schemeScale > 0.25f) {
                            FloatingActionButton(onClick = {
                                appState.updateState(
                                    schemeState.copy(
                                        schemeScale = schemeState.schemeScale - 0.25f,
                                        isDirty = true
                                    )
                                )
                            }, backgroundColor = MaterialTheme.colors.primary) {
                                Icon(
                                    painter = painterResource(Res.drawable.zoom_out),
                                    contentDescription = "Zoom Out"
                                )
                            }
                        }
                    }
                }

                //  Zoom controls for background
                if (schemeState.background != null && !ExportFlag.isExporting) {
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (schemeState.backgroundScale < 4f) {
                            FloatingActionButton(onClick = {
                                appState.updateState(
                                    schemeState.copy(
                                        backgroundScale = schemeState.backgroundScale + 0.25f,
                                        isDirty = true
                                    )
                                )
                            }, backgroundColor = MaterialTheme.colors.primary) {
                                Icon(
                                    painter = painterResource(Res.drawable.zoom_in),
                                    contentDescription = "Zoom In Background"
                                )
                            }
                        }

                        if (schemeState.backgroundScale > 0.25f) {
                            FloatingActionButton(onClick = {
                                appState.updateState(
                                    schemeState.copy(
                                        backgroundScale = schemeState.backgroundScale - 0.25f,
                                        isDirty = true
                                    )
                                )
                            }, backgroundColor = MaterialTheme.colors.primary) {
                                Icon(
                                    painter = painterResource(Res.drawable.zoom_out),
                                    contentDescription = "Zoom Out Background"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Показываем диалог, если требуется выбрать подложку
        if (showBackgroundPrompt) {
            AlertDialog(
                onDismissRequest = { showBackgroundPrompt = false },
                title = { Text("Загрузка подложки") },
                text = { Text("Для схемы необходимо выбрать подложку \"${schemeState.backgroundFileName}\". Выбрать сейчас?") },
                confirmButton = {
                    TextButton(onClick = {
                        openBackgroundFromDialog(appState.mutableSchemeState)
                        showBackgroundPrompt = false
                    }) { Text("Да") }
                },
                dismissButton = {
                    TextButton(onClick = { showBackgroundPrompt = false }) { Text("Нет") }
                }
            )
        }
    }
}

// Компенсируем смещения элементов при изменении матрицы
fun compensateOffsets(
    oldMatrix: ElementMatrix,
    newMatrix: ElementMatrix,
    existingOffsets: Map<Int, Offset>,
    dx: Float,
    dy: Float
): Map<Int, Offset> {
    val updated = existingOffsets.toMutableMap()
    oldMatrix.forEachElement { rowOld, colOld, element ->
        element?.let { el ->
            newMatrix.findElementById(el.id)?.let { (rowNew, colNew) ->
                val dr = rowNew - rowOld
                val dc = colNew - colOld
                if (dr != 0 || dc != 0) {
                    val prev = updated[el.id] ?: Offset.Zero
                    updated[el.id] = prev - Offset(dc * dx, dr * dy)
                }
            }
        }
    }
    return updated
}