package com.vegatel.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.model.REPEATER_ID
import com.vegatel.scheme.model.saveElementMatrixToFile
import com.vegatel.scheme.ui.MainMenu
import com.vegatel.scheme.ui.SchemeConstructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

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
        endElementId = REPEATER_ID
    )

    this[1, 0] = Repeater(
        endElementId = 2
    )

    this[2, 0] = Antenna(
        id = 2
    )
}
//

// SchemeState
data class SchemeState(
    val elements: ElementMatrix,
    val fileName: String? = null,
    val isDirty: Boolean = false
)

val initialSchemeState = SchemeState(
    elements = initialElements,
    fileName = null,
    isDirty = false
)
//

private val _schemeState = MutableStateFlow(initialSchemeState)
val schemeState: StateFlow<SchemeState> = _schemeState.asStateFlow()

@Composable
@Preview
fun App() {
    MaterialTheme {
        val schemeState by schemeState.collectAsState()

        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            MainMenu(
                fileName = schemeState.fileName,
                isDirty = schemeState.isDirty,
                onNew = {
                    _schemeState.value = initialSchemeState
                },
                onOpen = {
                    openElementMatrixFromDialog(_schemeState)
                },
                onSave = {
                    if (schemeState.fileName == null) {
                        saveElementMatrixFromDialog(_schemeState)
                    } else {
                        saveElementMatrixToFile(schemeState.elements, schemeState.fileName!!)
                        _schemeState.value = schemeState.copy(isDirty = false)
                    }
                },
                onSaveAs = {
                    saveElementMatrixFromDialog(_schemeState)
                }
            )

            Divider()

            SchemeConstructor(
                elements = schemeState.elements,
                onElementsChange = { newElements ->
                    val isDirty = newElements != schemeState.elements || schemeState.isDirty.not()
                    _schemeState.value = schemeState.copy(elements = newElements, isDirty = isDirty)
                }
            )
        }
    }
}