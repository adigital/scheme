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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
    val baseStationSignal: Double = 30.0,
    val frequency: Int = 800,
    val schemeOffset: Offset = Offset.Zero,
    val elementOffsets: Map<Int, Offset> = emptyMap(),
    val background: ImageBitmap? = null,
    val schemeScale: Float = 1f,
    val backgroundScale: Float = 1f,
    val backgroundFileName: String? = null
)

val initialSchemeState = SchemeState(
    elements = initialElements,
    fileName = null,
    isDirty = false,
    baseStationSignal = 30.0,
    frequency = 800
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

private const val MAX_HISTORY_SIZE = 10

private val appState = AppState()

@Composable
@Preview
fun App() {
    MaterialTheme {
        val schemeState by appState.schemeState.collectAsState()
        val scale = schemeState.schemeScale
        var schemeVersion by remember { mutableStateOf(0) }
        val bgScale = schemeState.backgroundScale

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
                    onUndo = { appState.undo() },
                    onRedo = { appState.redo() }
                )

                Box(modifier = Modifier.background(if (schemeState.background != null) Color.Gray else Color.White)) {
                    schemeState.background?.let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.None,
                            alignment = Alignment.TopStart,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = bgScale,
                                    scaleY = bgScale,
                                    transformOrigin = TransformOrigin(0f, 0f)
                                )
                                .align(Alignment.TopStart)
                        )
                    }

                    Box(Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) {
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
                                val newState =
                                    schemeState.copy(elements = newElements, isDirty = isDirty)
                                appState.updateState(newState)
                            },
                            baseStationSignal = schemeState.baseStationSignal,
                            frequency = schemeState.frequency,
                            resetKey = schemeVersion
                        )
                    }
                }
            }

            // Zoom controls
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                // Zoom controls for scheme
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

                //  Zoom controls for background
                if (schemeState.background != null) {
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