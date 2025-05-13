package com.vegatel.scheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.extansion.toPx
import com.vegatel.scheme.log
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Load
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.Element.Splitter2
import com.vegatel.scheme.model.Element.Splitter3
import com.vegatel.scheme.model.Element.Splitter4
import com.vegatel.scheme.model.ElementMatrix
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SchemeConstructor(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit
) {
    elements.forEachElement { row, col, element ->
        log("Test", "init row = $row, col = $col, element = $element")
    }

    // Геометрия схемы
    val elementWidthDp = 48
    val elementHeightDp = 64

    val paddingHorizontalDp = 24
    val paddingVerticalDp = 24

    val width = elements.colCount * 2 * elementWidthDp
    val height = elements.rowCount * 2 * elementHeightDp

    // Расчет сигнала на репитере
    val signalAtRepeater = 0.0 //calculateSignalAtRepeater(elements.first(), cable)

    // Состояние: для какого элемента открыто меню (row, col)
    var elementMenuOpenedForIndex by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        Modifier
            .size(width.dp, height.dp)
            .background(Color.White)
    ) {
        elements.forEachElementComposable { row, col, element ->
            val elementOffset = IntOffset(
                paddingHorizontalDp.dp.toPx().toInt() + col * 2 * elementWidthDp.dp.toPx().toInt(),
                paddingVerticalDp.dp.toPx().toInt() + row * 2 * elementHeightDp.dp.toPx().toInt()
            )

            Box(
                modifier = Modifier.offset { elementOffset }
            ) {
                when (element) {
                    is Antenna -> {
                        AntennaView(
                            signalPower = element.signalPower,
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Load -> {
                        LoadView(
                            signalPower = element.signalPower,
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Splitter2 -> TODO()
                    is Splitter3 -> TODO()
                    is Splitter4 -> TODO()
                    is Repeater -> {
                        RepeaterView(
                            signalPower = 0.0
                        )
                    }

                    null -> Unit
                }

                // Меню для текущего элемента
                if (elementMenuOpenedForIndex == row to col) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { elementMenuOpenedForIndex = null },
                    ) {
                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Antenna(
                                id = 0,
                                signalPower = 35.0,
                                endElementId = -1,
                                cable = Cable(length = 10.0, thickness = 2, lossPerMeter = 0.5)
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Антенна (35 дБм)") }

                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Load(
                                id = 0,
                                signalPower = 0.0,
                                endElementId = -1,
                                cable = Cable(length = 10.0, thickness = 2, lossPerMeter = 0.5)
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Нагрузка") }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun preview() {
    val elements = ElementMatrix(initialRows = 2, initialCols = 1)

    elements[0, 0] = Antenna(
        id = 1,
        signalPower = 35.0,
        endElementId = 2,
        cable = Cable(length = 10.0, thickness = 2, lossPerMeter = 0.5)
    )

    elements[1, 0] = Repeater(
        id = 2,
        topElementId = 1
    )

    SchemeConstructor(
        elements = elements,
        onElementsChange = {}
    )
}