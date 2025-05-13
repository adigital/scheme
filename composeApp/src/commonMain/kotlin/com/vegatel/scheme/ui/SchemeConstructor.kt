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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.extensions.toPx
import com.vegatel.scheme.initialElements
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

            // Рисуем кабель
            val cable = element?.fetchCable()

            if (cable != null) {
                // Получаем координаты первого и второго элемента по id
                val topElementId = element.fetchTopElementId()
                val endElementId = element.fetchEndElementId()

                val startElement = elements.findElementById(topElementId)
                val endElement = elements.findElementById(endElementId)

                if (startElement != null && endElement != null) {
                    val (startRow, startCol) = startElement
                    val (endRow, endCol) = endElement

                    // Вычисляем координаты центра низа и центра верха
                    val elementWidth = 48.dp.toPx()
                    val elementHeight = 64.dp.toPx()
                    val paddingHorizontal = 24.dp.toPx()
                    val paddingVertical = 24.dp.toPx()
                    val endElementInstance = elements[endElement.first, endElement.second]
                    val endVerticalOffsetDp =
                        if (endElementInstance is Splitter2 ||
                            endElementInstance is Splitter3 ||
                            endElementInstance is Splitter4
                        ) 9.75.dp.toPx() else 0.0f // Сдвиг верхней точки подключения кабеля в нижнем элементе

                    val startCenter = Offset(
                        x = paddingHorizontal + startCol * 2 * elementWidth + elementWidth / 2,
                        y = paddingVertical + startRow * 2 * elementHeight + elementHeight
                    )

                    val endCenter = Offset(
                        x = paddingHorizontal + endCol * 2 * elementWidth + elementWidth / 2,
                        y = paddingVertical + endRow * 2 * elementHeight + endVerticalOffsetDp
                    )

                    CableView(
                        start = startCenter,
                        end = endCenter,
                        cable = cable
                    )
                }
            }

            // Рисуем элементы
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

                    is Splitter2 -> {
                        Splitter2View(
                            signalPower = element.signalPower,
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Splitter3 -> {
                        Splitter3View(
                            signalPower = element.signalPower,
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Splitter4 -> {
                        Splitter4View(
                            signalPower = element.signalPower,
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

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
                                id = element?.id ?: -1,
                                endElementId = element?.fetchEndElementId() ?: -1,
                                cable = element?.fetchCable() ?: Cable()
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Антенна (35 дБм)") }

                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Load(
                                id = element?.id ?: -1,
                                endElementId = element?.fetchEndElementId() ?: -1,
                                cable = element?.fetchCable() ?: Cable()
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Нагрузка") }

                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Splitter2(
                                id = element?.id ?: -1,
                                endElementId = element?.fetchEndElementId() ?: -1,
                                cable = element?.fetchCable() ?: Cable()
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Сплиттер 2") }

                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Splitter3(
                                id = element?.id ?: -1,
                                endElementId = element?.fetchEndElementId() ?: -1,
                                cable = element?.fetchCable() ?: Cable()
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Сплиттер 3") }

                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            newElements[row, col] = Splitter4(
                                id = element?.id ?: -1,
                                endElementId = element?.fetchEndElementId() ?: -1,
                                cable = element?.fetchCable() ?: Cable()
                            )
                            elementMenuOpenedForIndex = null

                            onElementsChange(newElements)
                        }) { Text("Сплиттер 4") }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun preview() {
    SchemeConstructor(
        elements = initialElements,
        onElementsChange = {}
    )
}