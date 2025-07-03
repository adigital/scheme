package com.vegatel.scheme.domain.usecase

import com.vegatel.scheme.log
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.Element.Antenna
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
import kotlin.math.log10
import kotlin.math.pow

/**
 * Рассчитывает потери в кабеле для заданной частоты (МГц)
 * attenuationMap хранит затухание в дБ на 100 метров, поэтому приводим к дБ/метр.
 */
fun calculateCableLoss(cable: Cable, frequency: Int): Double {
    // Приводим значения затухания из дБ/100м к дБ/м и умножаем на длину кабеля
    val lossPer100m = cable.type.attenuationMap[frequency] ?: 0.0
    val lossPerMeter = lossPer100m / 100.0
    return cable.length * lossPerMeter
}

/**
 * Конвертация дБм в милливатты
 */
fun dBmToMw(dBm: Double): Double {
    return 10.0.pow(dBm / 10.0)
}

/**
 * Конвертация милливатт в дБм
 */
fun mwToDBm(mW: Double): Double {
    return 10.0 * log10(mW)
}

/**
 * Рассчитывает суммарную мощность сигнала для элемента в матрице с учётом частоты
 */
fun ElementMatrix.calculateSignalPower(
    elementId: Int,
    baseStationSignal: Double,
    frequency: Int
): Double {
    // Кэш для хранения результатов расчета
    val cache = mutableMapOf<Int, Double>()
    // Множество для отслеживания элементов в процессе расчета
    val calculating = mutableSetOf<Int>()

    fun calculate(elementId: Int): Double {
        // Если результат уже в кэше, возвращаем его
        cache[elementId]?.let { return it }

        // Если элемент уже в процессе расчета, значит обнаружен цикл
        if (elementId in calculating) {
            return 0.0
        }

        // Получаем элемент по идентификатору
        val coords = findElementById(elementId) ?: return 0.0
        val element = this[coords.first, coords.second] ?: return 0.0

        // Добавляем элемент в множество обрабатываемых
        calculating.add(elementId)

        val result = when {
            // Источники: антенны и нагрузки выше репитера
            !isElementBelowRepeater(elementId) && (element is Antenna || element is Load) -> {
                element.signalPower + baseStationSignal
            }

            // Комбайнеры: суммируем сигналы от всех подключенных сверху элементов
            element is Combiner2 || element is Combiner3 || element is Combiner4 -> {
                val inputs = mutableListOf<Double>()
                forEachElement { row, col, child ->
                    if (child?.fetchEndElementId() == elementId) {
                        val src = calculate(child.id)
                        val loss = calculateCableLoss(child.fetchCable(), frequency)
                        inputs.add(src + loss)
                    }
                }
                if (inputs.isEmpty()) 0.0 else {
                    val totalMw = inputs.sumOf { dBmToMw(it) }
                    mwToDBm(totalMw) + element.signalPower
                }
            }

            // Репитер: берём максимальный входящий сигнал
            element is Repeater -> {
                val inputs = mutableListOf<Double>()
                val currentCoords = findElementById(elementId) ?: return 0.0
                val elementRow = currentCoords.first
                forEachElement { row, col, child ->
                    if (child?.fetchEndElementId() == elementId && row < elementRow) {
                        val src = calculate(child.id)
                        val loss = calculateCableLoss(child.fetchCable(), frequency)
                        inputs.add(src + loss)
                    }
                }
                val maxIn = inputs.maxOrNull() ?: 0.0
                val amplified = maxIn + element.signalPower
                if (amplified > element.maxOutputPower) element.maxOutputPower else amplified
            }

            // Бустер: усиливает сигнал максимум на signalPower, но не более maxOutputPower
            element is Booster -> {
                val inputs = mutableListOf<Double>()
                forEachElement { rowChild, colChild, child ->
                    if (child?.fetchEndElementId() == elementId) {
                        val src = calculate(child.id)
                        val loss = calculateCableLoss(child.fetchCable(), frequency)
                        inputs.add(src + loss)
                    }
                }
                val maxIn = inputs.maxOrNull() ?: 0.0
                val amplified = maxIn + element.signalPower
                if (amplified > element.maxOutputPower) element.maxOutputPower else amplified
            }

            // Сплиттер: сигнал на входе берётся от всех элементов, подключенных сверху (rowChild < row), включая родительский через fetchEndElementId()
            element is Splitter2 || element is Splitter3 || element is Splitter4 -> {
                // Находим координаты текущего элемента
                val currentCoords = findElementById(elementId) ?: return 0.0
                val elementRow = currentCoords.first
                val inputs = mutableListOf<Double>()
                // Входы от upstream элементов (те, кто подключен через endElementId и находятся выше)
                forEachElement { rowChild, colChild, child ->
                    if (child?.fetchEndElementId() == elementId && rowChild < elementRow) {
                        val src = calculate(child.id)
                        val loss = calculateCableLoss(child.fetchCable(), frequency)
                        inputs.add(src + loss)
                    }
                }
                // Вход от элемента, к которому подключен этот сплиттер (если он выше)
                val parentId = element.fetchEndElementId()
                if (parentId >= 0) {
                    findElementById(parentId)?.let { parentCoords ->
                        if (parentCoords.first < elementRow) {
                            val src = calculate(parentId)
                            val loss = calculateCableLoss(element.fetchCable(), frequency)
                            inputs.add(src + loss)
                        }
                    }
                }
                val inputSignal = inputs.maxOrNull() ?: 0.0
                inputSignal + element.signalPower
            }

            // Ответвитель: собираем входной сигнал аналогично сплиттеру
            element is Coupler -> {
                val currentCoords = findElementById(elementId) ?: return 0.0
                val elementRow = currentCoords.first
                val inputs = mutableListOf<Double>()
                forEachElement { rowChild, colChild, child ->
                    if (child?.fetchEndElementId() == elementId && rowChild < elementRow) {
                        val src = calculate(child.id)
                        val loss = calculateCableLoss(child.fetchCable(), frequency)
                        inputs.add(src + loss)
                    }
                }
                val parentIdC = element.fetchEndElementId()
                if (parentIdC >= 0) {
                    findElementById(parentIdC)?.let { parentCoordsC ->
                        if (parentCoordsC.first < elementRow) {
                            val src = calculate(parentIdC)
                            val loss = calculateCableLoss(element.fetchCable(), frequency)
                            inputs.add(src + loss)
                        }
                    }
                }
                val inputSignalC = inputs.maxOrNull() ?: 0.0
                // Общее signalPower у Coupler всегда 0.0
                inputSignalC + element.signalPower
            }

            // Антенна или нагрузка ниже репитера (линия принятия): используем endElementId для связи с родителем и учитываем signalPower
            element is Antenna || element is Load -> {
                val parentId = element.fetchEndElementId()
                if (parentId >= 0) {
                    val parentPow = calculate(parentId)
                    val loss = calculateCableLoss(element.fetchCable(), frequency)
                    // учёт затухания ответвителя
                    val parentCoords = findElementById(parentId)
                    val attenuation = if (parentCoords != null) {
                        val parentElem = this[parentCoords.first, parentCoords.second]
                        if (parentElem is Coupler) {
                            val childCoords = findElementById(element.id)
                            if (childCoords != null) {
                                if (childCoords.second == parentCoords.second) parentElem.attenuation1
                                else if (childCoords.second == parentCoords.second + 1) parentElem.attenuation2
                                else 0.0
                            } else 0.0
                        } else 0.0
                    } else 0.0
                    parentPow + loss + element.signalPower - attenuation
                } else {
                    // fallback: находим элемент, подключённый сверху
                    var signal = 0.0
                    forEachElement { rowChild, colChild, child ->
                        if (child?.fetchEndElementId() == elementId) {
                            val src = calculate(child.id)
                            val loss = calculateCableLoss(child.fetchCable(), frequency)
                            signal = src + loss + (child.signalPower)
                        }
                    }
                    signal
                }
            }

            else -> 0.0
        }

        // Удаляем элемент из множества обрабатываемых
        calculating.remove(elementId)
        // Сохраняем результат в кэше
        cache[elementId] = result

        log("TEST", "Signal for element id=$elementId (${element::class.simpleName}): $result")

        return result
    }

    return calculate(elementId)
}