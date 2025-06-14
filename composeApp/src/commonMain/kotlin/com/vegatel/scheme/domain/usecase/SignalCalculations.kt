package com.vegatel.scheme.domain.usecase

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
import kotlin.math.log10
import kotlin.math.pow

/**
 * Рассчитывает потери в кабеле
 */
fun calculateCableLoss(cable: Cable): Double {
    return cable.length * cable.lossPerMeter
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
fun mwToDBm(mw: Double): Double {
    return 10.0 * log10(mw)
}

/**
 * Расчитывает суммарную мощность сигнала для элемента в матрице
 */
fun ElementMatrix.calculateSignalPower(elementId: Int): Double {
    // Получаем элемент по идентификатору
    val coords = findElementById(elementId) ?: return 0.0
    val element = this[coords.first, coords.second] ?: return 0.0

    // Источники: антенны и нагрузки выше репитера
    if (!isElementBelowRepeater(elementId) && (element is Antenna || element is Load)) {
        return element.signalPower
    }

    // Комбайнеры: суммируем сигналы от всех подключенных сверху элементов
    if (element is Combiner2 || element is Combiner3 || element is Combiner4) {
        val inputs = mutableListOf<Double>()
        forEachElement { row, col, child ->
            if (child?.fetchEndElementId() == elementId) {
                val src = calculateSignalPower(child.id)
                val loss = calculateCableLoss(child.fetchCable())
                inputs.add(src + loss)
            }
        }
        if (inputs.isEmpty()) return 0.0
        val totalMw = inputs.sumOf { dBmToMw(it) }
        return mwToDBm(totalMw) + element.signalPower
    }

    // Репитер: берём максимальный входящий сигнал
    if (element is Repeater) {
        val inputs = mutableListOf<Double>()
        forEachElement { row, col, child ->
            if (child?.fetchEndElementId() == elementId) {
                val src = calculateSignalPower(child.id)
                val loss = calculateCableLoss(child.fetchCable())
                inputs.add(src + loss)
            }
        }
        val maxIn = inputs.maxOrNull() ?: 0.0
        return maxIn + element.signalPower
    }

    // Сплиттер: сигнал идёт из родительского элемента
    if (element is Splitter2 || element is Splitter3 || element is Splitter4) {
        var parentSignal = 0.0
        outer@ for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                val parent = this[r, c]
                if (parent != null && parent.fetchEndElementId() == elementId &&
                    (parent is Repeater || parent is Combiner2 || parent is Combiner3 || parent is Combiner4 ||
                            parent is Splitter2 || parent is Splitter3 || parent is Splitter4)
                ) {
                    val src = calculateSignalPower(parent.id)
                    val loss = calculateCableLoss(parent.fetchCable())
                    parentSignal = src + loss
                    break@outer
                }
            }
        }
        return parentSignal + element.signalPower
    }

    // Антенна или нагрузка ниже репитера (линия принятия): используем endElementId для связи с родителем
    if (element is Antenna || element is Load) {
        val parentId = element.fetchEndElementId()
        if (parentId >= 0) {
            val parentPow = calculateSignalPower(parentId)
            val loss = calculateCableLoss(element.fetchCable())
            return parentPow + loss
        }
        // fallback: находим элемент, подключённый сверху
        outer@ for (r in 0 until rowCount) {
            for (c in 0 until colCount) {
                val child = this[r, c]
                if (child?.fetchEndElementId() == elementId) {
                    return calculateSignalPower(child.id) + calculateCableLoss(child.fetchCable())
                }
            }
        }
        return 0.0
    }

    // Все остальные случаи
    return 0.0
} 