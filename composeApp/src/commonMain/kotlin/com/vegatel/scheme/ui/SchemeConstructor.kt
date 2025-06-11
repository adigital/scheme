package com.vegatel.scheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vegatel.scheme.extensions.toPx
import com.vegatel.scheme.initialElements
import com.vegatel.scheme.log
import com.vegatel.scheme.model.Cable
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.log10
import kotlin.math.pow

// Добавляем функции для расчета сигнала перед @Composable SchemeConstructor

// Рассчитывает потери в кабеле
private fun calculateCableLoss(cable: Cable): Double {
    return (cable.length * cable.lossPerMeter)
}

// Конвертация дБм в милливатты
private fun dBmToMw(dBm: Double): Double {
    return 10.0.pow(dBm / 10.0)
}

// Конвертация милливатт в дБм
private fun mwToDBm(mw: Double): Double {
    return 10.0 * log10(mw)
}

// Рассчитывает суммарную мощность сигнала для элемента
private fun ElementMatrix.calculateSignalPower(elementId: Int): Double {
    val element = findElementById(elementId)?.let { (row, col) -> this[row, col] } ?: return 0.0

    // Определяем положение элемента относительно репитера
    val isBelowRepeater = isElementBelowRepeater(elementId)

    return when {
        // Антенна или нагрузка выше репитера - берем их собственный сигнал
        !isBelowRepeater && (element is Antenna || element is Load) -> {
            element.signalPower
        }

        // Для всех остальных элементов считаем входящий сигнал
        else -> {
            // Находим все элементы, подключенные сверху
            val inputSignals = mutableListOf<Double>()
            forEachElement { row, col, connectedElement ->
                if (connectedElement?.fetchEndElementId() == elementId) {
                    // Получаем сигнал от подключенного элемента
                    val sourceSignal = calculateSignalPower(connectedElement.id)
                    // Учитываем потери в кабеле
                    val cableLoss = calculateCableLoss(connectedElement.fetchCable())
                    inputSignals.add(sourceSignal + cableLoss)
                }
            }

            // Применяем характеристики текущего элемента
            when (element) {
                is Combiner2, is Combiner3, is Combiner4,
                is Splitter2, is Splitter3, is Splitter4 -> {
                    if (inputSignals.isEmpty()) {
                        0.0
                    } else {
                        // Преобразуем все входящие сигналы из дБм в мВт
                        val inputPowersMw = inputSignals.map { dBmToMw(it) }

                        // Суммируем мощности в мВт
                        val totalInputPowerMw = inputPowersMw.sum()

                        // Преобразуем обратно в дБм
                        val totalInputPowerDBm = mwToDBm(totalInputPowerMw)

                        // Добавляем потери сумматора из его характеристики signalPower
                        totalInputPowerDBm + element.signalPower
                    }
                }

                is Repeater -> {
                    // Для репитера берем максимальный входящий сигнал
                    val maxInputSignal = inputSignals.maxOrNull() ?: 0.0
                    maxInputSignal + element.signalPower
                }

                is Antenna, is Load -> {
                    inputSignals.firstOrNull() ?: 0.0
                }
            }
        }
    }
}

@Composable
fun SchemeConstructor(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit
) {
    // Состояние для диалога длины кабеля
    var cableLengthDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cableLengthInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    // Состояние для диалога усиления репитера
    var repeaterGainDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var repeaterGainInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    elements.forEachElement { row, col, element ->
        log("TEST", "init row = $row, col = $col, element = $element")
    }

    // Геометрия схемы
    val elementWidthDp = 48
    val elementHeightDp = 64

    val paddingHorizontalDp = 24
    val paddingVerticalDp = 24

    val width = elements.colCount * 2 * elementWidthDp
    val height = elements.rowCount * 2 * elementHeightDp

    // Состояние смещения для перетаскивания
    var dragOffset: Offset by remember { mutableStateOf(Offset.Zero) }

    // Состояние: для какого элемента открыто меню (row, col)
    var elementMenuOpenedForIndex: Pair<Int, Int>? by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    var cableMenuOpenedForIndex: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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
                            if (element?.isHalfShiftRender() == true) 48.dp.toPx()
                                .toInt() else 0.dp.toPx().toInt(),
                    paddingVerticalDp.dp.toPx().toInt() + row * 2 * elementHeightDp.dp.toPx()
                        .toInt()
                )

                // Рассчитываем мощность сигнала для текущего элемента
                val calculatedSignalPower =
                    element?.let { elements.calculateSignalPower(it.id) } ?: 0.0

                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .offset { elementOffset }
                ) {
                    when (element) {
                        is Antenna -> {
                            AntennaView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Load -> {
                            LoadView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner2 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner3 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Combiner4 -> {
                            CombinerView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Repeater -> {
                            RepeaterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter2 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter3 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
                                    elementMenuOpenedForIndex = row to col
                                }
                            )
                        }

                        is Splitter4 -> {
                            SplitterView(
                                signalPower = calculatedSignalPower,
                                onClick = {
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
                            onDismissRequest = { elementMenuOpenedForIndex = null },
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
                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()
                                    val oldElement = newElements[row, col]

                                    // Если заменяем сумматор или сплиттер на не сумматор, удаляем подключенные элементы
                                    if (oldElement != null &&
                                        (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4)
                                    ) {
                                        newElements.removeConnectedElementsAbove(oldElement.id)
                                        // --- Новый блок: ищем элемент ниже (например, репитер) ---
                                        var belowElementId: Int? = null
                                        var belowCable: Cable? = null
                                        // Ищем первый элемент ниже в том же столбце
                                        for (r in (row + 1) until newElements.rowCount) {
                                            val candidate = newElements[r, col]
                                            if (candidate != null) {
                                                belowElementId = candidate.id
                                                belowCable = candidate.fetchCable()
                                                break
                                            }
                                        }
                                        newElements[row, col] = Antenna(
                                            id = oldElement.id,
                                            endElementId = belowElementId
                                                ?: oldElement.fetchEndElementId(),
                                            cable = oldElement.fetchCable()
                                        )
                                    } else {
                                        newElements[row, col] = Antenna(
                                            id = oldElement?.id ?: newElements.generateNewId(),
                                            endElementId = oldElement?.fetchEndElementId() ?: -1,
                                            cable = oldElement?.fetchCable() ?: Cable()
                                        )
                                    }

                                    // Оптимизируем пространство после замены
                                    newElements.optimizeSpace()

                                    elementMenuOpenedForIndex = null
                                    onElementsChange(newElements)
                                }) { Text("Антенна (35 дБм)") }

                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()
                                    val oldElement = newElements[row, col]

                                    // Если заменяем сумматор или сплиттер на нагрузку, удаляем подключенные элементы
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
                                        val leftBusy = newElements.hasElementAt(targetRow, leftAntennaCol)
                                        val rightBusy = newElements.hasElementAt(targetRow, rightAntennaCol)
                                        
                                        if (leftBusy && rightBusy) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        } else if (rightBusy) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
                                        } else if (leftBusy) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
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
                                            (currentCol > 0 && newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                            newElements.hasElementAt(targetRow, currentCol + 1)
                                        ) {
                                            newElements.shiftRowElementsRight(targetRow, currentCol - 1)
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
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, centerAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
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
                                            (currentCol > 0 && newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                            newElements.hasElementAt(targetRow, currentCol + 1) ||
                                            newElements.hasElementAt(targetRow, currentCol + 2)
                                        ) {
                                            newElements.shiftRowElementsRight(targetRow, currentCol - 1)
                                            currentCol += 1
                                        }

                                        // Определяем позиции для четырех антенн
                                        val leftAntennaCol = currentCol - 1    // Левая антенна
                                        val centerAntennaCol = currentCol      // Центральная антенна (над сумматором)
                                        val rightAntennaCol = currentCol + 1   // Правая антенна
                                        val farRightAntennaCol = currentCol + 2 // Крайняя правая антенна

                                        // Убеждаемся, что у нас достаточно места справа
                                        while (farRightAntennaCol >= newElements.colCount) {
                                            newElements.insertCol(newElements.colCount)
                                        }

                                        // Проверяем, есть ли элементы на местах антенн
                                        if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, centerAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, farRightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, farRightAntennaCol)
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
                                        val leftBusy = newElements.hasElementAt(targetRow, leftAntennaCol)
                                        val rightBusy = newElements.hasElementAt(targetRow, rightAntennaCol)
                                        
                                        if (leftBusy && rightBusy) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        } else if (rightBusy) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
                                        } else if (leftBusy) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
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
                                            (currentCol > 0 && newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                            newElements.hasElementAt(targetRow, currentCol + 1)
                                        ) {
                                            newElements.shiftRowElementsRight(targetRow, currentCol - 1)
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
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, centerAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
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
                                            (currentCol > 0 && newElements.hasElementAt(targetRow, currentCol - 1)) ||
                                            newElements.hasElementAt(targetRow, currentCol + 1) ||
                                            newElements.hasElementAt(targetRow, currentCol + 2)
                                        ) {
                                            newElements.shiftRowElementsRight(targetRow, currentCol - 1)
                                            currentCol += 1
                                        }

                                        // Определяем позиции для четырех антенн
                                        val leftAntennaCol = currentCol - 1    // Левая антенна
                                        val centerAntennaCol = currentCol      // Центральная антенна (над сумматором)
                                        val rightAntennaCol = currentCol + 1   // Правая антенна
                                        val farRightAntennaCol = currentCol + 2 // Крайняя правая антенна

                                        // Убеждаемся, что у нас достаточно места справа
                                        while (farRightAntennaCol >= newElements.colCount) {
                                            newElements.insertCol(newElements.colCount)
                                        }

                                        // Проверяем, есть ли элементы на местах антенн
                                        if (newElements.hasElementAt(targetRow, leftAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, leftAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, centerAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, centerAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, rightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, rightAntennaCol)
                                        }
                                        if (newElements.hasElementAt(targetRow, farRightAntennaCol)) {
                                            newElements.shiftRowElementsRight(targetRow, farRightAntennaCol)
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
                                    startElementInstance?.isHalfShiftRender() == false &&
                                    endElementInstance.isSplitter() == false) ||

                                    (startElementInstance?.isHalfShiftRender() == true &&
                                            (startElement.second == endElement.second) &&
                                            startElementInstance.isSplitter() == true)

                        val isShiftCableRight =
                            (endElementInstance?.isHalfShiftRender() == true &&
                                    (startElement.second == endElement.second + 1)
                                    && startElementInstance?.isHalfShiftRender() == false &&
                                    endElementInstance.isSplitter() == false) ||

                                    (startElementInstance?.isHalfShiftRender() == true &&
                                            (startElement.second + 1 == endElement.second) &&
                                            startElementInstance.isSplitter() == true)

                        log("TEST", "startElement: $startElement - endElement: $endElement")
                        log(
                            "TEST",
                            "startElementInstance: ${Class.forName(startElementInstance?.javaClass?.name).simpleName} (id=${startElementInstance?.id})" +
                                    " - endElementInstance: ${Class.forName(endElementInstance?.javaClass?.name).simpleName} (id=${endElementInstance?.id})"
                        )
                        log(
                            "TEST",
                            "isShiftCableLeft $isShiftCableLeft, - isShiftCableRight $isShiftCableRight"
                        )

                        // Вычисляем координаты центра низа и центра верха
                        val elementWidth = 48.dp.toPx()
                        val elementHeight = 64.dp.toPx()

                        val paddingHorizontal = 24.dp.toPx()
                        val paddingVertical = 24.dp.toPx()

                        // Горизонтальный сдвиг начальной точки подключения кабеля
                        val startHorizontalOffsetDp =
                            when {
                                (startElementInstance?.isHalfShiftRender() == true) -> {
                                    48.dp.toPx() +
                                            when {
                                                isShiftCableLeft -> {
                                                    -4.dp.toPx()
                                                }

                                                isShiftCableRight -> {
                                                    4.dp.toPx()
                                                }

                                                else -> 0.dp.toPx()
                                            }
                                }

                                else -> 0.dp.toPx()
                            }

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
                            when {
                                (endElementInstance?.isHalfShiftRender() == true) -> {
                                    48.dp.toPx() +
                                            when {
                                                isShiftCableLeft -> {
                                                    -4.dp.toPx()
                                                }

                                                isShiftCableRight -> {
                                                    4.dp.toPx()
                                                }

                                                else -> 0.dp.toPx()
                                            }
                                }

                                else -> 0.dp.toPx()
                            }

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
                            isTwoCorners = isShiftCableLeft || isShiftCableRight || (startElementInstance?.isHalfShiftRender() == true && !isElementBelowRepeater) ||
                                    (startElementInstance?.isRepeater() == true && endElementInstance?.isHalfShiftRender() == true),
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

                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable()
                                                    .copy(thickness = 1, lossPerMeter = -0.5)
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
                                    }) { Text("Тип1 (тонкий, -0.5 дБм)") }

                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable()
                                                    .copy(thickness = 2, lossPerMeter = -1.0)
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
                                    }) { Text("Тип2 (толще, -1 дБм)") }

                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable =
                                                oldElement.fetchCable()
                                                    .copy(thickness = 3, lossPerMeter = -1.5)
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
                                    }) { Text("Тип3 (самый толстый, -1.5 дБм)") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалоги выносим на верхний уровень
    // Диалог изменения длины кабеля
    cableLengthDialogState?.let { (row, col) ->
        AlertDialog(
            onDismissRequest = { cableLengthDialogState = null },
            title = { Text("Укажите длину кабеля") },
            text = {
                TextField(
                    value = cableLengthInput,
                    onValueChange = { input ->
                        // Заменяем запятую на точку и удаляем пробелы
                        val withDot = input.text.replace(",", ".").replace(" ", "")

                        // Разрешаем ввод только цифр и одной точки
                        if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                            when {
                                // Пустая строка или одна точка - разрешаем
                                withDot.isEmpty() || withDot == "." -> {
                                    cableLengthInput = TextFieldValue(
                                        text = withDot,
                                        selection = input.selection
                                    )
                                }
                                // Если есть число после точки или целое число
                                else -> {
                                    withDot.toDoubleOrNull()?.let { value ->
                                        if (value in 0.0..100.0) {
                                            cableLengthInput = TextFieldValue(
                                                text = withDot,
                                                selection = input.selection
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            handleCableLengthUpdate(
                                cableLengthInput.text,
                                elements,
                                row,
                                col,
                                onElementsChange
                            )
                            cableLengthDialogState = null
                        }
                    ),
                    placeholder = { Text("0.0 - 100.0") },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        handleCableLengthUpdate(
                            cableLengthInput.text,
                            elements,
                            row,
                            col,
                            onElementsChange
                        )
                        cableLengthDialogState = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { cableLengthDialogState = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог изменения усиления репитера
    repeaterGainDialogState?.let { (row, col) ->
        AlertDialog(
            onDismissRequest = { repeaterGainDialogState = null },
            title = { Text("Укажите усиление репитера") },
            text = {
                TextField(
                    value = repeaterGainInput,
                    onValueChange = { input ->
                        // Заменяем запятую на точку и удаляем пробелы
                        val withDot = input.text.replace(",", ".").replace(" ", "")

                        // Разрешаем ввод только цифр и одной точки
                        if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                            when {
                                // Пустая строка или одна точка - разрешаем
                                withDot.isEmpty() || withDot == "." -> {
                                    repeaterGainInput = TextFieldValue(
                                        text = withDot,
                                        selection = input.selection
                                    )
                                }
                                // Если есть число после точки или целое число
                                else -> {
                                    withDot.toDoubleOrNull()?.let { value ->
                                        if (value in 0.0..100.0) {
                                            repeaterGainInput = TextFieldValue(
                                                text = withDot,
                                                selection = input.selection
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            handleRepeaterGainUpdate(
                                repeaterGainInput.text,
                                elements,
                                row,
                                col,
                                onElementsChange
                            )
                            repeaterGainDialogState = null
                        }
                    ),
                    placeholder = { Text("0.0 - 100.0") },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        handleRepeaterGainUpdate(
                            repeaterGainInput.text,
                            elements,
                            row,
                            col,
                            onElementsChange
                        )
                        repeaterGainDialogState = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { repeaterGainDialogState = null }) {
                    Text("Отмена")
                }
            }
        )
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

// Функция для рекурсивного удаления элементов, подключенных к указанному элементу
fun ElementMatrix.removeConnectedElementsAbove(elementId: Int) {
    // Находим все элементы, которые подключены к данному элементу (endElementId == elementId)
    forEachElement { row, col, element ->
        if (element?.fetchEndElementId() == elementId) {
            // Если найденный элемент - сумматор или сплиттер, рекурсивно удаляем его подключения
            if (element is Combiner2 || element is Combiner3 || element is Combiner4 ||
                element is Splitter2 || element is Splitter3 || element is Splitter4
            ) {
                removeConnectedElementsAbove(element.id)
            }
            // Не удаляем репитер!
            if (element !is Repeater) {
                this[row, col] = null
            }
        }
    }
}

// Функция для оптимизации пустого пространства в матрице
fun ElementMatrix.optimizeSpace() {
    // Находим пустые столбцы
    val emptyColumns = mutableListOf<Int>()
    for (col in 0 until colCount) {
        var isEmpty = true
        for (row in 0 until rowCount) {
            if (this[row, col] != null) {
                isEmpty = false
                break
            }
        }
        if (isEmpty) {
            emptyColumns.add(col)
        }
    }

    // Удаляем пустые столбцы справа налево
    emptyColumns.sortedDescending().forEach { col ->
        // Проверяем, не является ли этот столбец единственным
        if (colCount > 1) {
            removeCol(col)
        }
    }

    // Находим пустые строки
    val emptyRows = mutableListOf<Int>()
    for (row in 0 until rowCount) {
        var isEmpty = true
        for (col in 0 until colCount) {
            if (this[row, col] != null) {
                isEmpty = false
                break
            }
        }
        if (isEmpty) {
            emptyRows.add(row)
        }
    }

    // Удаляем пустые строки снизу вверх
    emptyRows.sortedDescending().forEach { row ->
        // Проверяем, не является ли эта строка единственной
        if (rowCount > 1) {
            removeRow(row)
        }
    }
}

// Вынесем логику обновления длины кабеля в отдельную функцию
private fun handleCableLengthUpdate(
    input: String,
    elements: ElementMatrix,
    row: Int,
    col: Int,
    onElementsChange: (ElementMatrix) -> Unit
) {
    input.toDoubleOrNull()?.let { length ->
        if (length in 0.0..100.0) {
            val newElements = elements.copy()
            val oldElement = newElements[row, col]
            if (oldElement != null) {
                val newCable = oldElement.fetchCable().copy(length = length)
                log("TEST", "Updating cable length to $length")
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
                onElementsChange(newElements)
            }
        }
    }
}

// Вынесем логику обновления усиления репитера в отдельную функцию
private fun handleRepeaterGainUpdate(
    input: String,
    elements: ElementMatrix,
    row: Int,
    col: Int,
    onElementsChange: (ElementMatrix) -> Unit
) {
    input.toDoubleOrNull()?.let { gain ->
        if (gain in 0.0..100.0) {
            val newElements = elements.copy()
            val oldElement = newElements[row, col]
            if (oldElement is Repeater) {
                log("TEST", "Updating repeater gain to $gain")
                newElements[row, col] = oldElement.copy(signalPower = gain)
                onElementsChange(newElements)
            }
        }
    }
}