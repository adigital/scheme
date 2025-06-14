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
    val element = findElementById(elementId)?.let { (row, col) -> this[row, col] } ?: return 0.0

    // Определяем положение элемента относительно репитера
    val isBelowRepeater = isElementBelowRepeater(elementId)

    return when {
        // Антенна или нагрузка выше репитера - берем их собственный сигнал
        !isBelowRepeater && (element is Antenna || element is Load) -> element.signalPower

        // Для всех остальных элементов считаем входящий сигнал
        else -> {
            val inputSignals = mutableListOf<Double>()
            forEachElement { row, col, connectedElement ->
                if (connectedElement?.fetchEndElementId() == elementId) {
                    val sourceSignal = calculateSignalPower(connectedElement.id)
                    val loss = calculateCableLoss(connectedElement.fetchCable())
                    inputSignals.add(sourceSignal + loss)
                }
            }

            when (element) {
                is Combiner2, is Combiner3, is Combiner4,
                is Splitter2, is Splitter3, is Splitter4 -> {
                    if (inputSignals.isEmpty()) {
                        0.0
                    } else {
                        val totalInputMw = inputSignals.sumOf { dBmToMw(it) }
                        val totalInputDbm = mwToDBm(totalInputMw)
                        totalInputDbm + element.signalPower
                    }
                }

                is Repeater -> {
                    val maxInput = inputSignals.maxOrNull() ?: 0.0
                    maxInput + element.signalPower
                }

                is Antenna, is Load -> inputSignals.firstOrNull() ?: 0.0
            }
        }
    }
} 