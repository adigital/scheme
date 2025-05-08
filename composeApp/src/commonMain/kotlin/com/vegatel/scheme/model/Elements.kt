package com.vegatel.scheme.model

sealed class TopElement {
    data class Antenna(val signalPower: Double) : TopElement()
    data class Load(val resistance: Double) : TopElement()
}

data class Cable(
    val length: Double,
    val thickness: Int, // 1, 2, 3
    val lossPerMeter: Double
)

data class Repeater(val name: String = "Репитер")

fun calculateSignalAtRepeater(
    topElement: TopElement,
    cable: Cable
): Double {
    val inputSignal = when (topElement) {
        is TopElement.Antenna -> topElement.signalPower
        is TopElement.Load -> 0.0 // Нагрузка не излучает сигнал
    }
    val cableLoss = cable.length * cable.lossPerMeter
    return inputSignal - cableLoss
}