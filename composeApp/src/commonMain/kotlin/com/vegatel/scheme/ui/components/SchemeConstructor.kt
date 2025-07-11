package com.vegatel.scheme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vegatel.scheme.AppConfig
import com.vegatel.scheme.domain.usecase.calculateSignalPower
import com.vegatel.scheme.extensions.toPx
import com.vegatel.scheme.initialElements
import com.vegatel.scheme.log
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.CableType
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Attenuator
import com.vegatel.scheme.model.Element.Booster
import com.vegatel.scheme.model.Element.Combiner2
import com.vegatel.scheme.model.Element.Combiner3
import com.vegatel.scheme.model.Element.Combiner4
import com.vegatel.scheme.model.Element.Coupler
import com.vegatel.scheme.model.Element.Load
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.Element.Splitter2
import com.vegatel.scheme.model.Element.Splitter3
import com.vegatel.scheme.model.Element.Splitter4
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.ui.views.AntennaView
import com.vegatel.scheme.ui.views.AttenuatorView
import com.vegatel.scheme.ui.views.BoosterView
import com.vegatel.scheme.ui.views.CableView
import com.vegatel.scheme.ui.views.CombinerView
import com.vegatel.scheme.ui.views.CouplerView
import com.vegatel.scheme.ui.views.LoadView
import com.vegatel.scheme.ui.views.RepeaterView
import com.vegatel.scheme.ui.views.SplitterView
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SchemeConstructor(
    elements: ElementMatrix,
    schemeOffset: Offset,
    elementOffsets: Map<Int, Offset>,
    onSchemeOffsetChange: (Offset) -> Unit,
    onElementOffsetChange: (Int, Offset) -> Unit,
    onElementsChange: (ElementMatrix) -> Unit,
    baseStationSignal: Double = AppConfig.DEFAULT_BASE_STATION_SIGNAL_DBM,
    frequency: Int = 800,
    considerAntennaGain: Boolean = true,
    resetKey: Int = 0
) {
    // Состояние для диалога длины кабеля
    var cableLengthDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cableLengthInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // Состояние для диалога усиления репитера
    var repeaterGainDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var repeaterGainInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // Состояние для диалога усиления бустера
    var boosterGainDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var boosterGainInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    var attenuatorDialogState: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var attenuatorLossInput: TextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    val focusRequester = remember { FocusRequester() }

    elements.forEachElement { row, col, element ->
        log("TEST", "Element: ($row, $col) $element")
    }

    // Геометрия схемы
    val elementWidthDp = 48
    val elementHeightDp = 64

    val paddingHorizontalDp = 24
    val paddingVerticalDp = 24

    // Состояние: для какого элемента открыто меню (row, col)
    var elementMenuOpenedForIndex: Pair<Int, Int>? by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    var cableMenuOpenedForIndex: Pair<Int, Int>? by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var antennasMenuExpanded by remember { mutableStateOf(false) }
    var combinersMenuExpanded by remember { mutableStateOf(false) }
    var splittersMenuExpanded by remember { mutableStateOf(false) }
    var couplersMenuExpanded by remember { mutableStateOf(false) }
    var boostersMenuExpanded by remember { mutableStateOf(false) }

    // Локальное смещение схемы во время drag
    var localSchemeDrag by remember { mutableStateOf(Offset.Zero) }
    // Стабильный state для актуальных schemeOffset и колбэка
    val schemeOffsetState = rememberUpdatedState(schemeOffset)
    val onSchemeOffsetChangeState = rememberUpdatedState(onSchemeOffsetChange)

    // Локальный стейт для перетаскивания элементов
    val localDragOffsets = remember(resetKey) { mutableStateMapOf<Int, Offset>() }
    // Стабильный state для актуальных elementOffsets и onElementOffsetChange
    val elementOffsetsState = rememberUpdatedState(elementOffsets)
    val onElementOffsetChangeState = rememberUpdatedState(onElementOffsetChange)

    val elementLabels = remember(elements) {
        val counters = mutableMapOf<String, Int>()
        val map = mutableMapOf<Int, String>()
        elements.forEachElement { _, _, el ->
            el?.let { element ->
                val prefix = when (element) {
                    is Antenna -> "A"
                    is Load -> "L"
                    is Combiner2, is Combiner3, is Combiner4 -> "SM"
                    is Coupler -> "DC"
                    is Splitter2, is Splitter3, is Splitter4 -> "SW"
                    is Booster -> "VTL"
                    is Attenuator -> "AT"
                    else -> null
                }
                prefix?.let {
                    val index = (counters[it] ?: 0) + 1
                    counters[it] = index
                    map[element.id] = "$it$index"
                }
            }
        }
        map
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(resetKey) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localSchemeDrag += dragAmount
                    },
                    onDragEnd = {
                        // сбрасываем и обновляем внешнее состояние
                        val total = schemeOffsetState.value + localSchemeDrag
                        onSchemeOffsetChangeState.value(total)
                        localSchemeDrag = Offset.Zero
                    },
                    onDragCancel = {
                        localSchemeDrag = Offset.Zero
                    }
                )
            }
            .graphicsLayer(
                transformOrigin = TransformOrigin(0f, 0f),
                translationX = schemeOffsetState.value.x + localSchemeDrag.x,
                translationY = schemeOffsetState.value.y + localSchemeDrag.y
            )
    ) {
        elements.forEachElementComposable { row, col, element ->
            // Рассчитываем мощность сигнала для текущего элемента
            val calculatedSignalPower =
                element?.let {
                    elements.calculateSignalPower(
                        it.id,
                        baseStationSignal,
                        frequency,
                        considerAntennaGain
                    )
                } ?: 0.0

            // Добавляю перетаскивание: корректирую pixel-координаты для элемента с float-позиционированием
            val elementOffsetRaw = Offset(
                paddingHorizontalDp.dp.toPx() + col * 2 * elementWidthDp.dp.toPx(),
                paddingVerticalDp.dp.toPx() + row * 2 * elementHeightDp.dp.toPx()
            )

            val externalOffset =
                element?.let { elementOffsetsState.value[it.id] ?: Offset.Zero } ?: Offset.Zero
            val localOffset = element?.let { localDragOffsets[it.id] ?: Offset.Zero } ?: Offset.Zero

            val currentDragOffset = externalOffset + localOffset

            // Рисуем элементы
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = elementOffsetRaw.x + currentDragOffset.x
                        translationY = elementOffsetRaw.y + currentDragOffset.y
                    }
                    .pointerInput(element?.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                element?.let {
                                    val prev = localDragOffsets[it.id] ?: Offset.Zero
                                    localDragOffsets[it.id] = prev + dragAmount
                                }
                            },
                            onDragEnd = {
                                element?.let {
                                    val prevExternal =
                                        elementOffsetsState.value[it.id] ?: Offset.Zero
                                    val prevLocal = localDragOffsets[it.id] ?: Offset.Zero
                                    val total = prevExternal + prevLocal
                                    onElementOffsetChangeState.value(it.id, total)
                                    localDragOffsets.remove(it.id)
                                }
                            },
                            onDragCancel = {
                                element?.let { localDragOffsets.remove(it.id) }
                            }
                        )
                    }
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
                        val isOverloaded = calculatedSignalPower >= element.maxOutputPower - 1e-6
                        RepeaterView(
                            signalPower = calculatedSignalPower,
                            isOverloaded = isOverloaded,
                            onClick = {
                                antennasMenuExpanded = false
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Booster -> {
                        val isOverloaded = calculatedSignalPower >= element.maxOutputPower - 1e-6
                        BoosterView(
                            signalPower = calculatedSignalPower,
                            isOverloaded = isOverloaded,
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

                    is Coupler -> {
                        // Находим выходные мощности ветвей
                        val leftCoords = row + 1 to col
                        val rightCoords = row + 1 to col + 1
                        val leftElem = elements[leftCoords.first, leftCoords.second]
                        val rightElem = elements[rightCoords.first, rightCoords.second]
                        // Отображаем выходы самого ответвителя (до усиления антенн).
                        // 1. Сигнал на основном выходе уже хранится в calculatedSignalPower у Coupler.
                        val power1 = elements.calculateSignalPower(
                            element.id,
                            baseStationSignal,
                            frequency,
                            considerAntennaGain
                        )

                        // 2. Рассчитываем входную мощность в ответвитель = основной выход + attenuation1
                        val inputPower = power1 + element.attenuation1

                        // 3. Сигнал на ответвлении = вход − attenuation2
                        val power2 = inputPower - element.attenuation2
                        CouplerView(
                            attenuations = listOf(element.attenuation1, element.attenuation2),
                            signalPowers = listOf(power1, power2),
                            onClick = {
                                elementMenuOpenedForIndex = row to col
                            }
                        )
                    }

                    is Attenuator -> {
                        AttenuatorView(
                            signalPower = calculatedSignalPower,
                            attenuation = element.signalPower,
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
                            // Пункт Усиление для репитера
                            DropdownMenuItem(onClick = {
                                elementMenuOpenedForIndex = null
                                repeaterGainDialogState = row to col
                                val text = element.signalPower.toString()
                                repeaterGainInput = TextFieldValue(
                                    text = text,
                                    selection = TextRange(0, text.length)
                                )
                            }) { Text("Параметры") }
                            return@DropdownMenu
                        } else {
                            // Параметры бустера (первым пунктом)
                            if (element is Booster) {
                                DropdownMenuItem(onClick = {
                                    elementMenuOpenedForIndex = null
                                    boosterGainDialogState = row to col
                                    val text = element.signalPower.toString()
                                    boosterGainInput = TextFieldValue(
                                        text = text,
                                        selection = TextRange(0, text.length)
                                    )
                                }) { Text("Параметры") }

                                Divider()
                            }

                            // Параметры аттенюатора (первым пунктом)
                            if (element is Attenuator) {
                                DropdownMenuItem(onClick = {
                                    elementMenuOpenedForIndex = null
                                    attenuatorDialogState = row to col
                                    val text = element.signalPower.toString()
                                    attenuatorLossInput = TextFieldValue(
                                        text = text,
                                        selection = TextRange(0, text.length)
                                    )
                                }) { Text("Параметры") }

                                Divider()
                            }

                            // Антенны
                            DropdownMenuItem(onClick = { antennasMenuExpanded = true }) {
                                Text("Антенны")
                                DropdownMenu(
                                    expanded = antennasMenuExpanded,
                                    onDismissRequest = { antennasMenuExpanded = false }
                                ) {
                                    val antennaOptions = listOf(
                                        "Антенна FI (6 дБ)" to 6.0,
                                        "Антенна PI (9 дБ)" to 9.0,
                                        "Антенна 15PO (15 дБ)" to 15.0,
                                        "Антенна 16S (16 дБ)" to 16.0,
                                        "Антенна 11Y (11 дБ)" to 11.0,
                                        "Антенна Wi (3 дБ)" to 3.0
                                    )
                                    antennaOptions.forEach { (label, power) ->
                                        DropdownMenuItem(onClick = {
                                            antennasMenuExpanded = false
                                            val newElements = elements.copy()
                                            val oldElement = newElements[row, col]

                                            if (oldElement != null && (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                        oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                        oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                            elementMenuOpenedForIndex = null
                                            newElements.optimizeSpace()
                                            onElementsChange(newElements)
                                        }) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                        }

                        // Нагрузка
                        DropdownMenuItem(onClick = {
                            val newElements = elements.copy()
                            val oldElement = newElements[row, col]

                            if (oldElement != null &&
                                (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                        oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                        oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
                            ) {
                                newElements.removeConnectedElementsAbove(oldElement.id)
                            }

                            newElements[row, col] = Load(
                                id = oldElement?.id ?: newElements.generateNewId(),
                                endElementId = oldElement?.fetchEndElementId() ?: -1,
                                cable = oldElement?.fetchCable() ?: Cable()
                            )

                            elementMenuOpenedForIndex = null
                            newElements.optimizeSpace()
                            onElementsChange(newElements)
                        }) { Text("Нагрузка") }

                        // Аттенюатор
                        if (element !is Attenuator) {
                            DropdownMenuItem(onClick = {
                                val newElements = elements.copy()
                                val clickedElement =
                                    newElements[row, col] ?: return@DropdownMenuItem

                                val isBelow = newElements.isElementBelowRepeater(clickedElement.id)
                                val newAttenuatorId = newElements.generateNewId()

                                val insertIndex = if (isBelow) row else row + 1
                                newElements.insertRow(insertIndex)

                                val attenuatorRow = if (isBelow) row else row + 1
                                val clickedRowAfter = if (isBelow) row + 1 else row

                                val oldEndId = clickedElement.fetchEndElementId()
                                val parentForAttenuator = if (isBelow) oldEndId else oldEndId
                                // Помещаем аттенюатор
                                newElements[attenuatorRow, col] = Attenuator(
                                    id = newAttenuatorId,
                                    signalPower = 0.0,
                                    endElementId = parentForAttenuator,
                                    cable = Cable()
                                )

                                // Если элемент был ниже репитера, меняем его parent на аттенюатор
                                if (isBelow) {
                                    when (val old = newElements[clickedRowAfter, col]) {
                                        is Antenna -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Load -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner2 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner3 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner4 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter2 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter3 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter4 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Coupler -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Booster -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Attenuator -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Repeater -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        null -> Unit
                                    }
                                } else {
                                    // Обновляем parent самого кликнутого элемента
                                    when (val old = newElements[clickedRowAfter, col]) {
                                        is Antenna -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Load -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner2 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner3 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Combiner4 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter2 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter3 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Splitter4 -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Coupler -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Booster -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Attenuator -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        is Repeater -> newElements[clickedRowAfter, col] =
                                            old.copy(endElementId = newAttenuatorId)

                                        null -> Unit
                                    }
                                }

                                elementMenuOpenedForIndex = null
                                newElements.optimizeSpace()
                                onElementsChange(newElements)
                            }) { Text("Аттенюатор") }
                        }

                        if (element != null && !elements.isElementBelowRepeater(element.id)) {
                            Divider()

                            // Сумматоры
                            DropdownMenuItem(onClick = { combinersMenuExpanded = true }) {
                                Text("Сумматоры")
                                DropdownMenu(
                                    expanded = combinersMenuExpanded,
                                    onDismissRequest = { combinersMenuExpanded = false }
                                ) {
                                    // Сумматор 2
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        combinersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сумматор 2") }

                                    // Сумматор 3
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                rightAntennaCol
                                            )
                                        ) {
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
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val centerAntennaId = newElements.generateNewId()
                                        newElements[targetRow, centerAntennaCol] = Antenna(
                                            id = centerAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        combinersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сумматор 3") }

                                    // Сумматор 4
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                            (newElements.hasElementAt(
                                                targetRow,
                                                currentCol - 1
                                            )) ||
                                            newElements.hasElementAt(
                                                targetRow,
                                                currentCol + 1
                                            ) ||
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
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                rightAntennaCol
                                            )
                                        ) {
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
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val centerAntennaId = newElements.generateNewId()
                                        newElements[targetRow, centerAntennaCol] = Antenna(
                                            id = centerAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        val farRightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, farRightAntennaCol] = Antenna(
                                            id = farRightAntennaId,
                                            signalPower = 11.0,
                                            endElementId = combinerId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        combinersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сумматор 4") }
                                }
                            }
                        }

                        // Контекстное меню для элементов ниже репитера
                        if (element != null && elements.isElementBelowRepeater(element.id)) {
                            Divider()

                            // Бустеры
                            DropdownMenuItem(onClick = { boostersMenuExpanded = true }) {
                                Text("Бустеры")
                                DropdownMenu(
                                    expanded = boostersMenuExpanded,
                                    onDismissRequest = { boostersMenuExpanded = false }
                                ) {
                                    val boosterOptions = listOf(
                                        "VTL40" to (40.0 to 31.0),
                                        "VTL33" to (33.0 to 31.0),
                                        "VTL 20" to (20.0 to 31.0)
                                    )
                                    boosterOptions.forEach { (label, params) ->
                                        DropdownMenuItem(onClick = {
                                            boostersMenuExpanded = false
                                            val newElements = elements.copy()
                                            val oldElement = newElements[row, col]!!
                                            val boosterId = oldElement.id
                                            val piId = newElements.generateNewId()

                                            // Удаляем старые подключенные элементы, если они есть
                                            if ((oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                        oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                        oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
                                            ) {
                                                newElements.removeConnectedElementsAbove(
                                                    oldElement.id
                                                )
                                            }

                                            // Вставляем бустер на место старого элемента
                                            newElements[row, col] = Booster(
                                                id = boosterId,
                                                maxOutputPower = params.first,
                                                maxGain = params.second,
                                                signalPower = params.second,
                                                endElementId = element.fetchEndElementId(),
                                                cable = oldElement.fetchCable()
                                            )

                                            // Добавляем антенну PI под бустером
                                            newElements[row + 1, col] = Antenna(
                                                id = piId,
                                                signalPower = 9.0,
                                                endElementId = boosterId,
                                                cable = Cable()
                                            )

                                            elementMenuOpenedForIndex = null
                                            newElements.optimizeSpace()
                                            onElementsChange(newElements)
                                        }) {
                                            Text("$label (${params.first} дБм; ${params.second} дБ)")
                                        }
                                    }
                                }
                            }

                            // Ответвители
                            DropdownMenuItem(onClick = { couplersMenuExpanded = true }) {
                                Text("Ответвители")
                                DropdownMenu(
                                    expanded = couplersMenuExpanded,
                                    onDismissRequest = { couplersMenuExpanded = false }
                                ) {
                                    val couplerModels = listOf(
                                        "DC25" to (0.3 to 25.0),
                                        "DC20" to (0.4 to 20.0),
                                        "DC15" to (0.8 to 15.0),
                                        "DC10" to (1.0 to 10.0),
                                        "DC5" to (2.1 to 5.0)
                                    )
                                    couplerModels.forEach { (label, atten) ->
                                        DropdownMenuItem(onClick = {
                                            val newElements = elements.copy()
                                            if (row == elements.rowCount) newElements.insertRow(
                                                elements.rowCount
                                            )
                                            val oldElement = newElements[row, col]
                                            if (oldElement != null &&
                                                (oldElement is Combiner2 || oldElement is Combiner3 || oldElement is Combiner4 ||
                                                        oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                        oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
                                            ) {
                                                newElements.removeConnectedElementsAbove(
                                                    oldElement.id
                                                )
                                            }
                                            val couplerId = element.id
                                            newElements[row, col] = Coupler(
                                                id = couplerId,
                                                attenuation1 = atten.first,
                                                attenuation2 = atten.second,
                                                endElementId = element.fetchEndElementId(),
                                                cable = element.fetchCable()
                                            )
                                            val targetRow = row + 1
                                            val leftCol = col
                                            val rightCol = col + 1
                                            if (rightCol >= newElements.colCount) newElements.insertCol(
                                                newElements.colCount
                                            )
                                            val leftBusy =
                                                newElements.hasElementAt(targetRow, leftCol)
                                            val rightBusy =
                                                newElements.hasElementAt(targetRow, rightCol)
                                            if (leftBusy && rightBusy) newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftCol
                                            )
                                            else if (rightBusy) newElements.shiftRowElementsRight(
                                                targetRow,
                                                rightCol
                                            )
                                            else if (leftBusy) newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftCol
                                            )
                                            val leftId = newElements.generateNewId()
                                            newElements[targetRow, leftCol] = Antenna(
                                                id = leftId,
                                                signalPower = 9.0,
                                                endElementId = couplerId,
                                                cable = Cable()
                                            )
                                            val rightId = newElements.generateNewId()
                                            newElements[targetRow, rightCol] = Antenna(
                                                id = rightId,
                                                signalPower = 9.0,
                                                endElementId = couplerId,
                                                cable = Cable()
                                            )
                                            elementMenuOpenedForIndex = null
                                            couplersMenuExpanded = false
                                            newElements.optimizeSpace()
                                            onElementsChange(newElements)
                                        }) { Text(label) }
                                    }
                                }
                            }

                            // Сплиттеры
                            DropdownMenuItem(onClick = { splittersMenuExpanded = true }) {
                                Text("Сплиттеры")
                                DropdownMenu(
                                    expanded = splittersMenuExpanded,
                                    onDismissRequest = { splittersMenuExpanded = false }
                                ) {
                                    // Сплиттер SW2
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        splittersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сплиттер SW2") }

                                    // Сплиттер SW3
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                rightAntennaCol
                                            )
                                        ) {
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
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val centerAntennaId = newElements.generateNewId()
                                        newElements[targetRow, centerAntennaCol] = Antenna(
                                            id = centerAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        splittersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сплиттер SW3") }

                                    // Сплиттер SW4
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
                                                    oldElement is Splitter2 || oldElement is Splitter3 || oldElement is Splitter4 ||
                                                    oldElement is Coupler || oldElement is Booster || oldElement is Attenuator)
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
                                            (newElements.hasElementAt(
                                                targetRow,
                                                currentCol - 1
                                            )) ||
                                            newElements.hasElementAt(
                                                targetRow,
                                                currentCol + 1
                                            ) ||
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
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                leftAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        ) {
                                            newElements.shiftRowElementsRight(
                                                targetRow,
                                                centerAntennaCol
                                            )
                                        }
                                        if (newElements.hasElementAt(
                                                targetRow,
                                                rightAntennaCol
                                            )
                                        ) {
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
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val centerAntennaId = newElements.generateNewId()
                                        newElements[targetRow, centerAntennaCol] = Antenna(
                                            id = centerAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val rightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, rightAntennaCol] = Antenna(
                                            id = rightAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        val farRightAntennaId = newElements.generateNewId()
                                        newElements[targetRow, farRightAntennaCol] = Antenna(
                                            id = farRightAntennaId,
                                            signalPower = 9.0,
                                            endElementId = splitterId,
                                            cable = Cable()
                                        )

                                        elementMenuOpenedForIndex = null
                                        splittersMenuExpanded = false
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) { Text("Сплиттер SW4") }
                                }
                            }
                        }
                    }
                }

                // Отрисовка маркера (этикетки) над элементом
                element?.let { el ->
                    val label = elementLabels[el.id]
                    label?.let { textLabel ->
                        Text(
                            text = textLabel,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-16).dp)
                                .background(Color.White.copy(alpha = 0.7f)),
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }

            // Рисуем кабель
            if (element != null && element !is Repeater) {
                val cable = element.fetchCable()

                val isElementBelowRepeater = elements.isElementBelowRepeater(element.id)

                log(
                    "TEST",
                    "element: $element isElementBelowRepeater = $isElementBelowRepeater"
                )

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
                                endElementInstance.isSplitterOrCoupler() == false) ||

                                (startElementInstance?.isHalfShiftRender() == true &&
                                        (startElement.second == endElement.second) &&
                                        startElementInstance.isSplitterOrCoupler() == true)

                    val isShiftCableRight =
                        (endElementInstance?.isHalfShiftRender() == true &&
                                (startElement.second == endElement.second + 1) &&
                                endElementInstance.isSplitterOrCoupler() == false) ||

                                (startElementInstance?.isHalfShiftRender() == true &&
                                        (startElement.second + 1 == endElement.second) &&
                                        startElementInstance.isSplitterOrCoupler() == true)

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
                        if (startElementInstance?.isHalfShiftRender() == true) {
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
                        if (endElementInstance?.isHalfShiftRender() == true) {
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

                    // базовые Y-координаты верхней стороны элементов
                    val startBaseY = paddingVertical + startRow * 2 * elementHeight
                    val endBaseY = paddingVertical + endRow * 2 * elementHeight

                    val startCenterRaw = Offset(
                        x = paddingHorizontal + startCol * 2 * elementWidth + elementWidth / 2 + startHorizontalOffsetDp,
                        y = startBaseY + (if (cable.isStartFromTop) 0f else elementHeight) + startVerticalOffsetDp
                    )
                    val endCenterRaw = Offset(
                        x = paddingHorizontal + endCol * 2 * elementWidth + elementWidth / 2 + endHorizontalOffsetDp,
                        y = endBaseY + (if (cable.isEndFromTop) 0f else elementHeight) + endVerticalOffsetDp
                    )

                    // apply element drag offsets (external + local) for cables
                    val startDragOffset = startElementInstance?.let {
                        val external = elementOffsetsState.value[it.id] ?: Offset.Zero
                        val local = localDragOffsets[it.id] ?: Offset.Zero
                        external + local
                    } ?: Offset.Zero
                    val endDragOffset = endElementInstance?.let {
                        val external = elementOffsetsState.value[it.id] ?: Offset.Zero
                        val local = localDragOffsets[it.id] ?: Offset.Zero
                        external + local
                    } ?: Offset.Zero

                    val startCenter = startCenterRaw + startDragOffset
                    val endCenter = endCenterRaw + endDragOffset

                    CableView(
                        start = startCenter,
                        end = endCenter,
                        isTwoCorners = cable.isTwoCorners,
                        isSideThenDown = cable.isSideThenDown,
                        isStraightLine = cable.isStraightLine,
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
                                                is Coupler -> oldElement.copy(cable = newCable)
                                                is Booster -> oldElement.copy(cable = newCable)
                                                is Attenuator -> oldElement.copy(cable = newCable)
                                            }
                                        }
                                        cableMenuOpenedForIndex = null
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) {
                                        Text(type.displayName)
                                    }
                                }

                                Divider()

                                // Переключение начала кабеля (снизу/сверху)
                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null) {
                                        val newCable = oldElement.fetchCable().copy(
                                            isStartFromTop = !oldElement.fetchCable().isStartFromTop
                                        )
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
                                            is Coupler -> oldElement.copy(cable = newCable)
                                            is Booster -> oldElement.copy(cable = newCable)
                                            is Attenuator -> oldElement.copy(cable = newCable)
                                        }
                                    }
                                    cableMenuOpenedForIndex = null
                                    newElements.optimizeSpace()
                                    onElementsChange(newElements)
                                }) {
                                    Text("Перевернуть начало")
                                }

                                // Переключение конца кабеля (сверху/снизу)
                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null) {
                                        val newCable = oldElement.fetchCable().copy(
                                            isEndFromTop = !oldElement.fetchCable().isEndFromTop
                                        )
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
                                            is Coupler -> oldElement.copy(cable = newCable)
                                            is Booster -> oldElement.copy(cable = newCable)
                                            is Attenuator -> oldElement.copy(cable = newCable)
                                        }
                                    }
                                    cableMenuOpenedForIndex = null
                                    newElements.optimizeSpace()
                                    onElementsChange(newElements)
                                }) {
                                    Text("Перевернуть конец")
                                }

                                // Форма
                                DropdownMenuItem(onClick = {
                                    val newElements = elements.copy()
                                    val oldElement = newElements[row, col]
                                    if (oldElement != null) {
                                        val newCable = oldElement.fetchCable().copy(
                                            isStraightLine = !oldElement.fetchCable().isStraightLine
                                        )
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
                                            is Coupler -> oldElement.copy(cable = newCable)
                                            is Booster -> oldElement.copy(cable = newCable)
                                            is Attenuator -> oldElement.copy(cable = newCable)
                                        }
                                    }
                                    cableMenuOpenedForIndex = null
                                    newElements.optimizeSpace()
                                    onElementsChange(newElements)
                                }) {
                                    Text(if (element.fetchCable().isStraightLine) "Угол" else "Диагональ")
                                }

                                if (!element.fetchCable().isStraightLine) {
                                    // Углы
                                    DropdownMenuItem(onClick = {
                                        val newElements = elements.copy()
                                        val oldElement = newElements[row, col]
                                        if (oldElement != null) {
                                            val newCable = oldElement.fetchCable().copy(
                                                isTwoCorners = !oldElement.fetchCable().isTwoCorners
                                            )
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
                                                is Coupler -> oldElement.copy(cable = newCable)
                                                is Booster -> oldElement.copy(cable = newCable)
                                                is Attenuator -> oldElement.copy(cable = newCable)
                                            }
                                        }
                                        cableMenuOpenedForIndex = null
                                        newElements.optimizeSpace()
                                        onElementsChange(newElements)
                                    }) {
                                        Text(if (element.fetchCable().isTwoCorners) "Один угол" else "Два угла")
                                    }

                                    if (!element.fetchCable().isTwoCorners) {
                                        // Наклон
                                        DropdownMenuItem(onClick = {
                                            val newElements = elements.copy()
                                            val oldElement = newElements[row, col]
                                            if (oldElement != null) {
                                                val newCable = oldElement.fetchCable().copy(
                                                    isSideThenDown = !oldElement.fetchCable().isSideThenDown
                                                )
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
                                                    is Coupler -> oldElement.copy(cable = newCable)
                                                    is Booster -> oldElement.copy(cable = newCable)
                                                    is Attenuator -> oldElement.copy(cable = newCable)
                                                }
                                            }
                                            cableMenuOpenedForIndex = null
                                            newElements.optimizeSpace()
                                            onElementsChange(newElements)
                                        }) {
                                            Text(if (element.fetchCable().isSideThenDown) "Ниже" else "Выше")
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

    BoosterGainDialog(
        elements = elements,
        onElementsChange = onElementsChange,
        boosterGainDialogState = boosterGainDialogState,
        onBoosterGainDialogStateChange = { boosterGainDialogState = it },
        boosterGainInput = boosterGainInput,
        onBoosterGainInputChange = { boosterGainInput = it },
        focusRequester = focusRequester
    )

    AttenuatorLossDialog(
        elements = elements,
        onElementsChange = onElementsChange,
        attenuatorDialogState = attenuatorDialogState,
        onDialogStateChange = { attenuatorDialogState = it },
        lossInput = attenuatorLossInput,
        onLossInputChange = { attenuatorLossInput = it },
        focusRequester = focusRequester
    )
}

@Composable
@Preview
private fun preview() {
    SchemeConstructor(
        elements = initialElements,
        schemeOffset = Offset.Zero,
        elementOffsets = mapOf(),
        onSchemeOffsetChange = {},
        onElementOffsetChange = { _, _ -> },
        onElementsChange = {},
    )
}