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
import com.vegatel.scheme.model.Element.Splitter4
import com.vegatel.scheme.model.ElementMatrix
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

val initialElements = buildElementMatrix(rows = 5, cols = 5) {
    this[1, 0] = Antenna(
        id = 1,
        endElementId = 5
    )

    this[1, 1] = Antenna(
        id = 2,
        endElementId = 5
    )

    this[1, 2] = Antenna(
        id = 3,
        endElementId = 5
    )

    this[2, 1] = Splitter4(
        id = 5,
        endElementId = 0,
    )

    this[0, 3] = Antenna(
        id = 6,
        endElementId = 8
    )

    this[0, 4] = Antenna(
        id = 7,
        endElementId = 8
    )

    this[1, 3] = Splitter4(
        id = 8,
        endElementId = 5,
    )

    this[3, 1] = Repeater(
        endElementId = 9
    )

    this[4, 1] = Antenna(
        id = 9
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