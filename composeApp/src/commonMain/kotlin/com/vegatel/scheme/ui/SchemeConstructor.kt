package com.vegatel.scheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        log("TEST", "init row = $row, col = $col, element = $element")
    }

    val isRepeaterHalfShiftRender = elements.isRepeaterHalfShiftRender()
    val nextForRepeaterElementId = elements.getNextForRepeaterElementId()

    // Геометрия схемы
    val elementWidthDp = 48
    val elementHeightDp = 64

    val paddingHorizontalDp = 24
    val paddingVerticalDp = 24

    val width = elements.colCount * 2 * elementWidthDp
    val height = elements.rowCount * 2 * elementHeightDp

    // Состояние скроллов
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Расчет сигнала на репитере
    val signalAtRepeater = 0.0 //calculateSignalAtRepeater(elements.first(), cable)

    // Состояние: для какого элемента открыто меню (row, col)
    var elementMenuOpenedForIndex by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cableMenuOpenedForIndex by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        Modifier
            .verticalScroll(verticalScrollState)
            .horizontalScroll(horizontalScrollState)
    ) {
        Box(
            Modifier
                .size(width.dp, height.dp)
                .background(Color.LightGray)
        ) {
            elements.forEachElementComposable { row, col, element ->
                val elementOffset = IntOffset(
                    paddingHorizontalDp.dp.toPx().toInt() + col * 2 * elementWidthDp.dp.toPx()
                        .toInt() +
                            if (element?.isHalfShiftRender() == true ||
                                (element?.isRepeater() == true && isRepeaterHalfShiftRender) ||
                                (element?.id == nextForRepeaterElementId && isRepeaterHalfShiftRender)
                            ) 48.dp.toPx().toInt() else 0.dp.toPx().toInt(),
                    paddingVerticalDp.dp.toPx().toInt() + row * 2 * elementHeightDp.dp.toPx()
                        .toInt()
                )

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
                            SplitterView(
                                signalPower = element.signalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter3 -> {
                            SplitterView(
                                signalPower = element.signalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter4 -> {
                            SplitterView(
                                signalPower = element.signalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Repeater -> {
                            RepeaterView(
                                signalPower = element.signalPower,
                                onClick = {
                                }
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

                                // Если мы в верхней строке, добавляем новую строку сверху
                                var currentRow = row
                                if (row == 0) {
                                    newElements.insertRow(0)
                                    currentRow = 1 // Теперь наш элемент находится в строке 1
                                }

                                // Сначала создаем сплиттер на месте кликнутого элемента
                                val splitterId = element?.id ?: newElements.generateNewId()
                                newElements[currentRow, col] = Splitter2(
                                    id = splitterId,
                                    endElementId = element?.fetchEndElementId() ?: -1,
                                    cable = element?.fetchCable() ?: Cable()
                                )

                                // Теперь сдвигаем все элементы правее позиции сплиттера
                                newElements.shiftColumnAndRightElementsRight(col + 1)

                                val targetRow = currentRow - 1
                                val leftAntennaCol = col
                                val rightAntennaCol = col + 1

                                if (rightAntennaCol >= newElements.colCount) {
                                    newElements.insertCol(newElements.colCount)
                                }

                                // Проверяем, есть ли элементы на местах антенн после сдвига
                                val leftBusy = newElements.hasElementAt(targetRow, leftAntennaCol)
                                val rightBusy = newElements.hasElementAt(targetRow, rightAntennaCol)
                                if (leftBusy && rightBusy) {
                                    newElements.shiftColumnAndRightElementsRight(leftAntennaCol)
                                } else if (rightBusy) {
                                    newElements.shiftColumnAndRightElementsRight(rightAntennaCol)
                                } else if (leftBusy) {
                                    newElements.shiftColumnAndRightElementsRight(leftAntennaCol)
                                }

                                // Создаем антенны
                                val leftAntennaId = newElements.generateNewId()
                                newElements[targetRow, leftAntennaCol] = Antenna(
                                    id = leftAntennaId,
                                    endElementId = splitterId,
                                    cable = Cable()
                                )

                                val rightAntennaId = newElements.generateNewId()
                                newElements[targetRow, rightAntennaCol] = Antenna(
                                    id = rightAntennaId,
                                    endElementId = splitterId,
                                    cable = Cable()
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

                        val startElementInstance = elements[startElement.first, startElement.second]
                        val endElementInstance = elements[endElement.first, endElement.second]

                        val isShiftLeft =
                            endElementInstance?.isHalfShiftRender() == true &&
                                    (startElement.second == endElement.second) &&
                                    startElementInstance?.isHalfShiftRender() == false
                        val isShiftRight =
                            endElementInstance?.isHalfShiftRender() == true &&
                                    (startElement.second == endElement.second + 1)
                                    && startElementInstance?.isHalfShiftRender() == false

                        log("TEST", "$startElement, - $endElement")
                        log("TEST", "isShiftLeft $isShiftLeft, - isShiftRight $isShiftRight")

                        // Вычисляем координаты центра низа и центра верха
                        val elementWidth = 48.dp.toPx()
                        val elementHeight = 64.dp.toPx()

                        val paddingHorizontal = 24.dp.toPx()
                        val paddingVertical = 24.dp.toPx()

                        // Горизонтальный сдвиг верхней точки подключения кабеля
                        val startHorizontalOffsetDp =
                            when {
                                (startElementInstance?.isHalfShiftRender() == true) or (startElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender) -> {
                                    48.dp.toPx()
                                }

                                else -> 0.dp.toPx()
                            }
                        // Горизонтальный сдвиг нижней точки подключения кабеля
                        val endHorizontalOffsetDp =
                            when {
                                endElementInstance?.isHalfShiftRender() == true ||
                                        (endElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender) ||
                                        (endElementInstance?.id == nextForRepeaterElementId && isRepeaterHalfShiftRender) -> {
                                    48.dp.toPx() +
                                            when {
                                                isShiftLeft -> {
                                                    -4.dp.toPx()
                                                }

                                                isShiftRight -> {
                                                    4.dp.toPx()
                                                }

                                                else -> 0.dp.toPx()
                                            }
                                }

                                else -> 0.dp.toPx()
                            }
                        // Вертикальный сдвиг нижней точки подключения кабеля для сплиттера
                        val endVerticalOffsetDp =
                            if (endElementInstance?.isSplitter() == true && !(isShiftLeft || isShiftRight)) 9.75.dp.toPx() else 0.0f

                        val startCenter = Offset(
                            x = paddingHorizontal + startCol * 2 * elementWidth + elementWidth / 2 + startHorizontalOffsetDp,
                            y = paddingVertical + startRow * 2 * elementHeight + elementHeight
                        )

                        val endCenter = Offset(
                            x = paddingHorizontal + endCol * 2 * elementWidth + elementWidth / 2 + endHorizontalOffsetDp,
                            y = paddingVertical + endRow * 2 * elementHeight + endVerticalOffsetDp
                        )

                        CableView(
                            start = startCenter,
                            end = endCenter,
                            isTwoCorners = isShiftLeft || isShiftRight,
                            cable = cable,
                            onClick = {
                                cableMenuOpenedForIndex = row to col
                            }
                        )

                        // Меню для текущего кабеля
                        Box(
                            modifier = Modifier
                                .absoluteOffset {
                                    IntOffset(
                                        ((startCenter.x + endCenter.x) / 2).toInt(),
                                        ((startCenter.y + endCenter.y) / 2).toInt()
                                    )
                                }
                        ) {
                            if (cableMenuOpenedForIndex == row to col) {
                                DropdownMenu(
                                    expanded = true,
                                    onDismissRequest = { cableMenuOpenedForIndex = null },
                                ) {
                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable().copy(thickness = 1)
                                            newElements[row, col] = when (oldElement) {
                                                is Antenna -> oldElement.copy(cable = newCable)
                                                is Load -> oldElement.copy(cable = newCable)
                                                is Splitter2 -> oldElement.copy(cable = newCable)
                                                is Splitter3 -> oldElement.copy(cable = newCable)
                                                is Splitter4 -> oldElement.copy(cable = newCable)
                                                is Repeater -> oldElement.copy(cable = newCable)
                                            }
                                        }
                                        cableMenuOpenedForIndex = null
                                        onElementsChange(newElements)
                                    }) { Text("Тип1 (тонкий)") }

                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable().copy(thickness = 2)
                                            newElements[row, col] = when (oldElement) {
                                                is Antenna -> oldElement.copy(cable = newCable)
                                                is Load -> oldElement.copy(cable = newCable)
                                                is Splitter2 -> oldElement.copy(cable = newCable)
                                                is Splitter3 -> oldElement.copy(cable = newCable)
                                                is Splitter4 -> oldElement.copy(cable = newCable)
                                                is Repeater -> oldElement.copy(cable = newCable)
                                            }
                                        }
                                        cableMenuOpenedForIndex = null
                                        onElementsChange(newElements)
                                    }) { Text("Тип2 (толще)") }

                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable().copy(thickness = 3)
                                            newElements[row, col] = when (oldElement) {
                                                is Antenna -> oldElement.copy(cable = newCable)
                                                is Load -> oldElement.copy(cable = newCable)
                                                is Splitter2 -> oldElement.copy(cable = newCable)
                                                is Splitter3 -> oldElement.copy(cable = newCable)
                                                is Splitter4 -> oldElement.copy(cable = newCable)
                                                is Repeater -> oldElement.copy(cable = newCable)
                                            }
                                        }
                                        cableMenuOpenedForIndex = null
                                        onElementsChange(newElements)
                                    }) { Text("Тип3 (самый толстый)") }
                                }
                            }
                        }
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