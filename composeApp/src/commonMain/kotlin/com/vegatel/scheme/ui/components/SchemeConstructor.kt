package com.vegatel.scheme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vegatel.scheme.domain.usecase.calculateSignalPower
import com.vegatel.scheme.extensions.toPx
import com.vegatel.scheme.initialElements
import com.vegatel.scheme.log
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.CableType
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Combiner2
import com.vegatel.scheme.model.Element.Combiner3
import com.vegatel.scheme.model.Element.Combiner4
import com.vegatel.scheme.model.Element.Load
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.Element.Splitter2
import com.vegatel.scheme.model.Element.Splitter3
import com.vegatel.scheme.model.Element.Splitter4
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.ui.views.AntennaView
import com.vegatel.scheme.ui.views.CableView
import com.vegatel.scheme.ui.views.CombinerView
import com.vegatel.scheme.ui.views.LoadView
import com.vegatel.scheme.ui.views.RepeaterView
import com.vegatel.scheme.ui.views.SplitterView
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SchemeConstructor(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit,
    baseStationSignal: Double = 30.0,
    frequency: Int = 800
) {
    // Состояние для диалога длины кабеля
    var cableLengthDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cableLengthInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // Состояние для диалога усиления репитера
    var repeaterGainDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var repeaterGainInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    val focusRequester = remember { FocusRequester() }

    elements.forEachElement { row, col, element ->
        log("TEST", "Element: ($row, $col) $element")
    }

    val isRepeaterHalfShiftRender = elements.isRepeaterHalfShiftRender()

    // Геометрия схемы
    val elementWidthDp = 48
    val elementHeightDp = 64

    val paddingHorizontalDp = 24
    val paddingVerticalDp = 24

    val width = elements.colCount * 2 * elementWidthDp
    val height = elements.rowCount * 2 * elementHeightDp

    // Состояние: для какого элемента открыто меню (row, col)
    var elementMenuOpenedForIndex: Pair<Int, Int>? by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    var cableMenuOpenedForIndex: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var antennasMenuExpanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .zIndex(0f)  // Схема будет находиться на нижнем слое
            .background(Color.White)
    ) {
        Box(
            Modifier.size(width.dp, height.dp)
        ) {
            elements.forEachElementComposable { row, col, element ->
                // Рисуем элементы
                val elementOffset = IntOffset(
                    paddingHorizontalDp.dp.toPx().toInt() + col * 2 * elementWidthDp.dp.toPx()
                        .toInt() +
                            if (element?.isHalfShiftRender() == true ||
                                (element?.isRepeater() == true && isRepeaterHalfShiftRender)
                            ) 48.dp.toPx()
                                .toInt() else 0.dp.toPx().toInt(),
                    paddingVerticalDp.dp.toPx().toInt() + row * 2 * elementHeightDp.dp.toPx()
                        .toInt()
                )

                // Рассчитываем мощность сигнала для текущего элемента
                val calculatedSignalPower =
                    element?.let {
                        elements.calculateSignalPower(
                            it.id,
                            baseStationSignal,
                            frequency
                        )
                    } ?: 0.0

                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .offset { elementOffset }
                ) {
                    when (element) {
                        is Antenna -> {
                            AntennaView(
                                antenna = element,
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Load -> {
                            LoadView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner2 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner3 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner4 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Repeater -> {
                            RepeaterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter2 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter3 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter4 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    antennasMenuExpanded = false
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        null -> Unit
                    }

                    // Меню для текущего элемента
                    if (elementMenuOpenedForIndex == row to col) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = {
                                elementMenuOpenedForIndex = null; antennasMenuExpanded = false
                            },
                        ) {
                            if (element is Repeater) {
                                DropdownMenuItem(onClick = {
                                    elementMenuOpenedForIndex = null
                                    repeaterGainDialogState = row to col
                                    val text = element.signalPower.toString()
                                    repeaterGainInput = TextFieldValue(
                                        text = text,
                                        selection = TextRange(0, text.length)
                                    )
                                }) { Text("Усиление") }
                            } else {
                                // Nested Antennas submenu
                                DropdownMenuItem(onClick = { antennasMenuExpanded = true }) {
                                    Text("Антенны")
                                    DropdownMenu(
                                        expanded = antennasMenuExpanded,
                                        onDismissRequest = { antennasMenuExpanded = false }
                                    ) {
                                        val antennaOptions = listOf(
                                            "Антенна FI (6 дБм)" to 6.0,
                                            "Антенна PI (9 дБм)" to 9.0,
                                            "Антенна 15PO (15 дБм)" to 15.0,
                                            "Антенна 16S (16 дБм)" to 16.0,
                                            "Антенна 11Y (11 дБм)" to 11.0,
                                            "Антенна Wi (3 дБм)" to 3.0
                                        )
                                        antennaOptions.forEach { (label, power) ->
                                            DropdownMenuItem(onClick = {
                                                antennasMenuExpanded = false
                                                val newElements = elements.copy()
                                                val oldElement = newElements[row, col]

                                                if (oldElement != null && (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                            oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                                ) {
                                                    newElements.removeConnectedElementsAbove(
                                                        oldElement.id
                                                    )
                                                    var belowElementId: Int? = null
                                                    for (r in (row + 1) until newElements.rowCount) {
                                                        val candidate = newElements[r, col]
                                                        if (candidate != null) {
                                                            belowElementId = candidate.id
                                                            break
                                                        }
                                                    }
                                                    newElements[row, col] = Antenna(
                                                        id = oldElement.id,
                                                        signalPower = power,
                                                        endElementId = belowElementId
                                                            ?: oldElement.fetchEndElementId(),
                                                        cable = oldElement.fetchCable()
                                                    )
                                                } else {
                                                    newElements[row, col] = Antenna(
                                                        id = oldElement?.id
                                                            ?: newElements.generateNewId(),
                                                        signalPower = power,
                                                        endElementId = oldElement?.fetchEndElementId()
                                                            ?: -1,
                                                        cable = oldElement?.fetchCable() ?: Cable()
                                                    )
                                                }
                                                newElements.optimizeSpace()
                                                elementMenuOpenedForIndex = null
                                                onElementsChange(newElements)
                                            }) {
                                                Text(label)
                                            }
                                        }
                                    }
                                }
                            }

                            DropdownMenuItem(onClick = {
                                val newElements = elements.copy()
                                val oldElement = newElements[row, col]

                                if (oldElement != null &&
                                    (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                            oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                ) {
                                    newElements.removeConnectedElementsAbove(oldElement.id)
                                }

                                newElements[row, col] = Load(
                                    id = oldElement?.id ?: newElements.generateNewId(),
                                    endElementId = oldElement?.fetchEndElementId() ?: -1,
                                    cable = oldElement?.fetchCable() ?: Cable()
                                )

                                // Оптимизируем пространство после замены
                                newElements.optimizeSpace()

                                elementMenuOpenedForIndex = null
                                onElementsChange(newElements)
                            }) { Text("Нагрузка") }

                            if (element != null && !elements.isElementBelowRepeater(element.id)) {
                                Divider()

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в верхней строке, добавляем новую строку сверху
                                    var currentRow = row
                                    if (row == 0) {
                                        newElements.insertRow(0)
                                        currentRow =
                                            1 // Теперь наш элемент находится в строке 1
                                    }

                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[currentRow, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сумматор на месте кликнутого элемента
                                    val combinerId = element.id
                                    newElements[currentRow, col] = Combiner2(
                                        id = combinerId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    val targetRow = currentRow - 1
                                    val leftAntennaCol = col
                                    val rightAntennaCol = col + 1

                                    if (rightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    val leftBusy =
                                        newElements.hasElementAt(targetRow, leftAntennaCol)
                                    val rightBusy =
                                        newElements.hasElementAt(targetRow, rightAntennaCol)

                                    if (leftBusy && rightBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    } else if (rightBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    } else if (leftBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    }

                                    // Создаем антенны
                                    val leftAntennaId = newElements.generateNewId()
                                    newElements[targetRow, leftAntennaCol] = Antenna(
                                        id = leftAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val rightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, rightAntennaCol] = Antenna(
                                        id = rightAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сумматор 2") }

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в верхней строке, добавляем новую строку сверху
                                    var currentRow = row
                                    if (row == 0) {
                                        newElements.insertRow(0)
                                        currentRow =
                                            1 // Теперь наш элемент находится в строке 1
                                    }

                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[currentRow, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сумматор на месте кликнутого элемента
                                    val combinerId = element.id
                                    newElements[currentRow, col] = Combiner3(
                                        id = combinerId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Проверяем, нужно ли сдвинуть сумматор и элементы правее
                                    val targetRow = currentRow - 1
                                    var currentCol = col

                                    // Если мы в крайней левой колонке, добавляем новую колонку слева
                                    if (col == 0) {
                                        newElements.insertCol(0)
                                        currentCol = 1
                                    }

                                    // Проверяем наличие элементов в целевой строке
                                    if (newElements.hasElementAt(targetRow, currentCol) ||
                                        (currentCol > 0 && newElements.hasElementAt(
                                            targetRow,
                                            currentCol - 1
                                        )) ||
                                        newElements.hasElementAt(targetRow, currentCol + 1)
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            currentCol - 1
                                        )
                                        currentCol += 1
                                    }

                                    // Определяем позиции для трех антенн
                                    val leftAntennaCol = currentCol - 1
                                    val centerAntennaCol = currentCol
                                    val rightAntennaCol = currentCol + 1

                                    // Убеждаемся, что у нас достаточно места справа
                                    if (rightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            centerAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    }

                                    // Обновляем позицию сумматора после всех сдвигов
                                    newElements[currentRow, currentCol] = Combiner3(
                                        id = combinerId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Создаем три антенны
                                    val leftAntennaId = newElements.generateNewId()
                                    newElements[targetRow, leftAntennaCol] = Antenna(
                                        id = leftAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val centerAntennaId = newElements.generateNewId()
                                    newElements[targetRow, centerAntennaCol] = Antenna(
                                        id = centerAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val rightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, rightAntennaCol] = Antenna(
                                        id = rightAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сумматор 3") }

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в верхней строке, добавляем новую строку сверху
                                    var currentRow = row
                                    if (row == 0) {
                                        newElements.insertRow(0)
                                        currentRow =
                                            1 // Теперь наш элемент находится в строке 1
                                    }

                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[currentRow, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сумматор на месте кликнутого элемента
                                    val combinerId = element.id
                                    newElements[currentRow, col] = Combiner4(
                                        id = combinerId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Проверяем, нужно ли сдвинуть сумматор и элементы правее
                                    val targetRow = currentRow - 1
                                    var currentCol = col

                                    // Если мы в крайней левой колонке или рядом с ней, добавляем новые колонки слева
                                    while (currentCol < 1) {
                                        newElements.insertCol(0)
                                        currentCol += 1
                                    }

                                    // Проверяем наличие элементов в целевой строке
                                    if (newElements.hasElementAt(targetRow, currentCol) ||
                                        (newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                        newElements.hasElementAt(targetRow, currentCol + 1) ||
                                        newElements.hasElementAt(targetRow, currentCol + 2)
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            currentCol - 1
                                        )
                                        currentCol += 1
                                    }

                                    // Определяем позиции для четырех антенн
                                    val leftAntennaCol = currentCol - 1    // Левая антенна
                                    val centerAntennaCol =
                                        currentCol      // Центральная антенна (над сумматором)
                                    val rightAntennaCol = currentCol + 1   // Правая антенна
                                    val farRightAntennaCol =
                                        currentCol + 2 // Крайняя правая антенна

                                    // Убеждаемся, что у нас достаточно места справа
                                    while (farRightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            centerAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(
                                            targetRow,
                                            farRightAntennaCol
                                        )
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            farRightAntennaCol
                                        )
                                    }

                                    // Обновляем позицию сумматора после всех сдвигов
                                    newElements[currentRow, currentCol] = Combiner4(
                                        id = combinerId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Создаем четыре антенны
                                    val leftAntennaId = newElements.generateNewId()
                                    newElements[targetRow, leftAntennaCol] = Antenna(
                                        id = leftAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val centerAntennaId = newElements.generateNewId()
                                    newElements[targetRow, centerAntennaCol] = Antenna(
                                        id = centerAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val rightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, rightAntennaCol] = Antenna(
                                        id = rightAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    val farRightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, farRightAntennaCol] = Antenna(
                                        id = farRightAntennaId,
                                        endElementId = combinerId,
                                        cable = Cable()
                                    )

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сумматор 4") }
                            }

                            if (element != null && elements.isElementBelowRepeater(element.id)) {
                                Divider()

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в нижней строке, добавляем новую строку снизу
                                    if (row == elements.rowCount) {
                                        newElements.insertRow(elements.rowCount)
                                    }

                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сплиттер на месте кликнутого элемента
                                    val splitterId = element.id
                                    newElements[row, col] = Splitter2(
                                        id = splitterId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    val targetRow = row + 1
                                    val leftAntennaCol = col
                                    val rightAntennaCol = col + 1

                                    if (rightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    val leftBusy =
                                        newElements.hasElementAt(targetRow, leftAntennaCol)
                                    val rightBusy =
                                        newElements.hasElementAt(targetRow, rightAntennaCol)

                                    if (leftBusy && rightBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    } else if (rightBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    } else if (leftBusy) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
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

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сплиттер 2") }

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в нижней строке, добавляем новую строку снизу
                                    if (row == elements.rowCount) {
                                        newElements.insertRow(elements.rowCount)
                                    }


                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сплиттер на месте кликнутого элемента
                                    val splitterId = element.id
                                    newElements[row, col] = Splitter3(
                                        id = splitterId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Проверяем, нужно ли сдвинуть сплиттер и элементы правее
                                    val targetRow = row + 1
                                    var currentCol = col

                                    // Если мы в крайней левой колонке, добавляем новую колонку слева
                                    if (col == 0) {
                                        newElements.insertCol(0)
                                        currentCol = 1
                                    }

                                    // Проверяем наличие элементов в целевой строке
                                    if (newElements.hasElementAt(targetRow, currentCol) ||
                                        (currentCol > 0 && newElements.hasElementAt(
                                            targetRow,
                                            currentCol - 1
                                        )) ||
                                        newElements.hasElementAt(targetRow, currentCol + 1)
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            currentCol - 1
                                        )
                                        currentCol += 1
                                    }

                                    // Определяем позиции для трех антенн
                                    val leftAntennaCol = currentCol - 1
                                    val centerAntennaCol = currentCol
                                    val rightAntennaCol = currentCol + 1

                                    // Убеждаемся, что у нас достаточно места справа
                                    if (rightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            centerAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    }

                                    // Обновляем позицию сплиттера после всех сдвигов
                                    newElements[row, currentCol] = Splitter3(
                                        id = splitterId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Создаем три антенны
                                    val leftAntennaId = newElements.generateNewId()
                                    newElements[targetRow, leftAntennaCol] = Antenna(
                                        id = leftAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    val centerAntennaId = newElements.generateNewId()
                                    newElements[targetRow, centerAntennaCol] = Antenna(
                                        id = centerAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    val rightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, rightAntennaCol] = Antenna(
                                        id = rightAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сплиттер 3") }

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()

                                    // Если мы в нижней строке, добавляем новую строку снизу
                                    if (row == elements.rowCount) {
                                        newElements.insertRow(elements.rowCount)
                                    }

                                    // Удаляем старые подключенные элементы, если они есть
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                    }

                                    // Сначала создаем сплиттер на месте кликнутого элемента
                                    val splitterId = element.id
                                    newElements[row, col] = Splitter4(
                                        id = splitterId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Проверяем, нужно ли сдвинуть сплиттер и элементы правее
                                    val targetRow = row + 1
                                    var currentCol = col

                                    // Если мы в крайней левой колонке или рядом с ней, добавляем новые колонки слева
                                    while (currentCol < 1) {
                                        newElements.insertCol(0)
                                        currentCol += 1
                                    }

                                    // Проверяем наличие элементов в целевой строке
                                    if (newElements.hasElementAt(targetRow, currentCol) ||
                                        (newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                        newElements.hasElementAt(targetRow, currentCol + 1) ||
                                        newElements.hasElementAt(targetRow, currentCol + 2)
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            currentCol - 1
                                        )
                                        currentCol += 1
                                    }

                                    // Определяем позиции для четырех антенн
                                    val leftAntennaCol = currentCol - 1    // Левая антенна
                                    val centerAntennaCol =
                                        currentCol      // Центральная антенна (над сумматором)
                                    val rightAntennaCol = currentCol + 1   // Правая антенна
                                    val farRightAntennaCol =
                                        currentCol + 2 // Крайняя правая антенна

                                    // Убеждаемся, что у нас достаточно места справа
                                    while (farRightAntennaCol >= newElements.colCount) {
                                        newElements.insertCol(newElements.colCount)
                                    }

                                    // Проверяем, есть ли элементы на местах антенн
                                    if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            leftAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            centerAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            rightAntennaCol
                                        )
                                    }
                                    if (newElements.hasElementAt(
                                            targetRow,
                                            farRightAntennaCol
                                        )
                                    ) {
                                        newElements.shiftRowElementsRight(
                                            targetRow,
                                            farRightAntennaCol
                                        )
                                    }

                                    // Обновляем позицию сплиттера после всех сдвигов
                                    newElements[row, currentCol] = Splitter4(
                                        id = splitterId,
                                        endElementId = element.fetchEndElementId(),
                                        cable = element.fetchCable()
                                    )

                                    // Создаем четыре антенны
                                    val leftAntennaId = newElements.generateNewId()
                                    newElements[targetRow, leftAntennaCol] = Antenna(
                                        id = leftAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    val centerAntennaId = newElements.generateNewId()
                                    newElements[targetRow, centerAntennaCol] = Antenna(
                                        id = centerAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    val rightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, rightAntennaCol] = Antenna(
                                        id = rightAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    val farRightAntennaId = newElements.generateNewId()
                                    newElements[targetRow, farRightAntennaCol] = Antenna(
                                        id = farRightAntennaId,
                                        endElementId = splitterId,
                                        cable = Cable()
                                    )

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Сплиттер 4") }
                            }
                        }
                    }
                }

                // Рисуем кабель
                val cable = element?.fetchCable()

                if (cable != null) {
                    val isElementBelowRepeater = elements.isElementBelowRepeater(element.id)

                    // Получаем координаты первого и второго элемента по id
                    val startElement =
                        elements.findElementById(if (isElementBelowRepeater) element.fetchEndElementId() else element.fetchTopElementId())
                    val endElement =
                        elements.findElementById(if (isElementBelowRepeater) element.fetchTopElementId() else element.fetchEndElementId())

                    if (startElement != null && endElement != null) {
                        val (startRow, startCol) = startElement
                        val (endRow, endCol) = endElement

                        val startElementInstance = elements[startElement.first, startElement.second]
                        val endElementInstance = elements[endElement.first, endElement.second]

                        val isShiftCableLeft =
                            (endElementInstance?.isHalfShiftRender() == true &&
                                    (startElement.second == endElement.second) &&
                                    endElementInstance.isSplitter() == false) ||

                                    (startElementInstance?.isHalfShiftRender() == true &&
                                            (startElement.second == endElement.second) &&
                                            startElementInstance.isSplitter() == true)

                        val isShiftCableRight =
                            (endElementInstance?.isHalfShiftRender() == true &&
                                    (startElement.second == endElement.second + 1) &&
                                    endElementInstance.isSplitter() == false) ||

                                    (startElementInstance?.isHalfShiftRender() == true &&
                                            (startElement.second + 1 == endElement.second) &&
                                            startElementInstance.isSplitter() == true)

                        log(
                            "TEST",
                            "Cable: $startElement ${Class.forName(startElementInstance?.javaClass?.name).simpleName} (id=${startElementInstance?.id}) isShiftCableLeft $isShiftCableLeft" +
                                    " - $endElement ${Class.forName(endElementInstance?.javaClass?.name).simpleName} (id=${endElementInstance?.id}) isShiftCableRight $isShiftCableRight"
                        )

                        // Вычисляем координаты центра низа и центра верха
                        val elementWidth = 48.dp.toPx()
                        val elementHeight = 64.dp.toPx()

                        val paddingHorizontal = 24.dp.toPx()
                        val paddingVertical = 24.dp.toPx()

                        // Горизонтальный сдвиг начальной точки подключения кабеля
                        val startHorizontalOffsetDp =
                            if (startElementInstance?.isHalfShiftRender() == true ||
                                (startElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender)
                            ) {
                                48.dp.toPx() +
                                        when {
                                            isElementBelowRepeater && isShiftCableLeft -> {
                                                -4.dp.toPx()
                                            }

                                            isElementBelowRepeater && isShiftCableRight -> {
                                                4.dp.toPx()
                                            }

                                            else -> 0.dp.toPx()
                                        }
                            } else 0.dp.toPx()

                        // Вертикальный сдвиг начальной точки подключения кабеля
                        val startVerticalOffsetDp =
                            if ((startElementInstance is Splitter3 && startElement.second != endElement.second) ||
                                (startElementInstance is Splitter4 && startElement.second != endElement.second - 1 && startElement.second != endElement.second)
                            ) {
                                (-64 + 9.75).dp.toPx()
                            } else {
                                0.dp.toPx()
                            }

                        // Горизонтальный сдвиг конечной точки подключения кабеля
                        val endHorizontalOffsetDp =
                            if (endElementInstance?.isHalfShiftRender() == true ||
                                (endElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender)
                            ) {
                                48.dp.toPx() +
                                        when {
                                            !isElementBelowRepeater && isShiftCableLeft -> {
                                                -4.dp.toPx()
                                            }

                                            !isElementBelowRepeater && isShiftCableRight -> {
                                                4.dp.toPx()
                                            }

                                            else -> 0.dp.toPx()
                                        }
                            } else 0.dp.toPx()

                        // Вертикальный сдвиг конечной точки подключения кабеля
                        val endVerticalOffsetDp =
                            if ((endElementInstance?.isCombiner() == true && !(isShiftCableLeft || isShiftCableRight))) 9.75.dp.toPx() else 0.0f

                        val startCenter = Offset(
                            x = paddingHorizontal + startCol * 2 * elementWidth + elementWidth / 2 + startHorizontalOffsetDp,
                            y = paddingVertical + startRow * 2 * elementHeight + elementHeight + startVerticalOffsetDp
                        )

                        val endCenter = Offset(
                            x = paddingHorizontal + endCol * 2 * elementWidth + elementWidth / 2 + endHorizontalOffsetDp,
                            y = paddingVertical + endRow * 2 * elementHeight + endVerticalOffsetDp
                        )

                        CableView(
                            start = startCenter,
                            end = endCenter,
                            isTwoCorners = isShiftCableLeft || isShiftCableRight ||
                                    (endElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender) ||
                                    (startElementInstance?.isRepeater() == true && isRepeaterHalfShiftRender),
                            isSideThenDown = startElementInstance?.isSplitter() == true &&
                                    (startElement.second != endElement.second || startElementInstance.isHalfShiftRender() == true),
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
                                        cableMenuOpenedForIndex = null
                                        cableLengthDialogState = row to col
                                        val text = cable.length.toString()
                                        cableLengthInput = TextFieldValue(
                                            text = text,
                                            selection = TextRange(0, text.length)
                                        )
                                    }) { Text("Длина") }

                                    Divider()

                                    // Выбор нового типа кабеля
                                    listOf(
                                        CableType.CF_HALF,
                                        CableType.TEN_D_FB,
                                        CableType.EIGHT_D_FB,
                                        CableType.FIVE_D_FB,
                                        CableType.OPTICAL
                                    ).forEach { type ->
                                        DropdownMenuItem(onClick = {
                                            val newElements = elements.copy()
                                            val oldElement = newElements[row, col]
                                            if (oldElement != null) {
                                                val newCable =
                                                    oldElement.fetchCable().copy(type = type)
                                                newElements[row, col] = when (oldElement) {
                                                    is Antenna -> oldElement.copy(cable = newCable)
                                                    is Load -> oldElement.copy(cable = newCable)
                                                    is Combiner2 -> oldElement.copy(cable = newCable)
                                                    is Combiner3 -> oldElement.copy(cable = newCable)
                                                    is Combiner4 -> oldElement.copy(cable = newCable)
                                                    is Repeater -> oldElement.copy(cable = newCable)
                                                    is Splitter2 -> oldElement.copy(cable = newCable)
                                                    is Splitter3 -> oldElement.copy(cable = newCable)
                                                    is Splitter4 -> oldElement.copy(cable = newCable)
                                                }
                                            }
                                            cableMenuOpenedForIndex = null
                                            onElementsChange(newElements)
                                        }) {
                                            Text(type.displayName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Добавляем диалоги
    CableLengthDialog(
        elements = elements,
        onElementsChange = onElementsChange,
        cableLengthDialogState = cableLengthDialogState,
        onCableLengthDialogStateChange = { cableLengthDialogState = it },
        cableLengthInput = cableLengthInput,
        onCableLengthInputChange = { cableLengthInput = it },
        focusRequester = focusRequester
    )

    RepeaterGainDialog(
        elements = elements,
        onElementsChange = onElementsChange,
        repeaterGainDialogState = repeaterGainDialogState,
        onRepeaterGainDialogStateChange = { repeaterGainDialogState = it },
        repeaterGainInput = repeaterGainInput,
        onRepeaterGainInputChange = { repeaterGainInput = it },
        focusRequester = focusRequester
    )
}

@Composable
@Preview
private fun preview() {
    SchemeConstructor(
        elements = initialElements,
        onElementsChange = {}
    )
}