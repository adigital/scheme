package com.vegatel.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.model.REPEATER_ID
import com.vegatel.scheme.model.saveElementMatrixToFile
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
    val frequency: Int = 800
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
        var scale by remember { mutableStateOf(1f) }
        var schemeVersion by remember { mutableStateOf(0) }

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
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
                            saveElementMatrixFromDialog(appState.mutableSchemeState)
                        } else {
                            saveElementMatrixToFile(schemeState.elements, schemeState.fileName!!)
                            appState.updateState(schemeState.copy(isDirty = false))
                        }
                    },
                    onSaveAs = {
                        saveElementMatrixFromDialog(appState.mutableSchemeState)
                    },
                    onUndo = { appState.undo() },
                    onRedo = { appState.redo() }
                )

                Divider()

                Box(Modifier.graphicsLayer(scaleX = scale, scaleY = scale)) {
                    SchemeConstructor(
                        elements = schemeState.elements,
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

            // Zoom controls
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                Column(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (scale < 4f) {
                        FloatingActionButton(onClick = {
                            if (scale < 4f) scale += 0.25f
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.zoom_in),
                                contentDescription = "Zoom In"
                            )
                        }
                    }

                    if (scale > 1f) {
                        FloatingActionButton(onClick = {
                            if (scale > 1f) scale -= 0.25f
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.zoom_out),
                                contentDescription = "Zoom Out"
                            )
                        }
                    }
                }
            }
        }
    }
}