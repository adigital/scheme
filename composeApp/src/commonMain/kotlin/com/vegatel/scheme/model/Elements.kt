package com.vegatel.scheme.model

sealed class TopElement {
    data class Antenna(
        val signalPower: Double,
        val endElement: EndElement
    ) : TopElement()

    data class Load(
        val resistance: Double = 0.0,
        val endElement: EndElement
    ) : TopElement()
}

sealed class EndElement {
    data class Splitter2(
        val topElement1: TopElement,
        val topElement2: TopElement,
        val endElement: EndElement
    ) : EndElement()

    data class Splitter3(
        val topElement1: TopElement,
        val topElement2: TopElement,
        val topElement3: TopElement,
        val endElement: EndElement
    ) : EndElement()

    data class Splitter4(
        val topElement1: TopElement,
        val topElement2: TopElement,
        val topElement3: TopElement,
        val topElement4: TopElement,
        val endElement: EndElement
    ) : EndElement()

    class Repeater() : EndElement()
}

data class Cable(
    val length: Double,
    val thickness: Int, // 1, 2, 3
    val lossPerMeter: Double
)

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