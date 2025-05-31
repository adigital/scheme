package com.vegatel.scheme.model

import kotlinx.serialization.Serializable

@Serializable
data class SerializableElementMatrix(
    val rows: Int,
    val cols: Int,
    val elements: List<SerializableElement?>
)

@Serializable
data class SerializableElement(
    val type: String,
    val id: Int,
    val signalPower: Double? = null,
    val endElementId: Int? = null,
    val cable: SerializableCable? = null
)

@Serializable
data class SerializableCable(
    val length: Double,
    val thickness: Int,
    val lossPerMeter: Double
)

fun ElementMatrix.toSerializable(): SerializableElementMatrix {
    val elements = mutableListOf<SerializableElement?>()
    for (row in 0 until rowCount) {
        for (col in 0 until colCount) {
            val e = this[row, col]
            elements.add(e?.let { element ->
                when (element) {
                    is Element.Antenna -> SerializableElement(
                        "Antenna", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Load -> SerializableElement(
                        "Load", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Combiner2 -> SerializableElement(
                        "Combiner2", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Combiner3 -> SerializableElement(
                        "Combiner3", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Combiner4 -> SerializableElement(
                        "Combiner4", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Repeater -> SerializableElement(
                        "Repeater", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Splitter2 -> SerializableElement(
                        "Splitter2", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Splitter3 -> SerializableElement(
                        "Splitter3", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )

                    is Element.Splitter4 -> SerializableElement(
                        "Splitter4", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            element.cable.length,
                            element.cable.thickness,
                            element.cable.lossPerMeter
                        )
                    )
                }
            })
        }
    }
    return SerializableElementMatrix(rowCount, colCount, elements)
}

fun SerializableElementMatrix.toElementMatrix(): ElementMatrix {
    val matrix = ElementMatrix(rows, cols)
    elements.forEachIndexed { index, e ->
        val row = index / cols
        val col = index % cols
        if (e != null) {
            val cable = e.cable?.let {
                Cable(
                    it.length,
                    it.thickness,
                    it.lossPerMeter
                )
            }
                ?: Cable()
            val element = when (e.type) {
                "Antenna" -> Element.Antenna(
                    e.id,
                    e.signalPower ?: 35.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Load" -> Element.Load(
                    e.id,
                    e.signalPower ?: 0.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Combiner2" -> Element.Combiner2(
                    e.id,
                    e.signalPower ?: -3.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Combiner3" -> Element.Combiner3(
                    e.id,
                    e.signalPower ?: -4.8,
                    e.endElementId ?: -1,
                    cable
                )

                "Combiner4" -> Element.Combiner4(
                    e.id,
                    e.signalPower ?: -6.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Repeater" -> Element.Repeater(
                    e.id,
                    e.signalPower ?: 50.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Splitter2" -> Element.Splitter2(
                    e.id,
                    e.signalPower ?: -3.0,
                    e.endElementId ?: -1,
                    cable
                )

                "Splitter3" -> Element.Splitter3(
                    e.id,
                    e.signalPower ?: -4.8,
                    e.endElementId ?: -1,
                    cable
                )

                "Splitter4" -> Element.Splitter4(
                    e.id,
                    e.signalPower ?: -6.0,
                    e.endElementId ?: -1,
                    cable
                )

                else -> null
            }
            matrix[row, col] = element
        }
    }
    return matrix
}

expect fun saveElementMatrixToFile(matrix: ElementMatrix, filename: String)