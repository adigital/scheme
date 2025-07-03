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
    val cable: SerializableCable? = null,
    val attenuation1: Double? = null,
    val attenuation2: Double? = null
)

@Serializable
data class SerializableCable(
    val length: Double,
    val type: String,
    val isTwoCorners: Boolean = false,
    val isSideThenDown: Boolean = false,
    val isStraightLine: Boolean = false
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
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Load -> SerializableElement(
                        "Load", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Combiner2 -> SerializableElement(
                        "Combiner2", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Combiner3 -> SerializableElement(
                        "Combiner3", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Combiner4 -> SerializableElement(
                        "Combiner4", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Repeater -> SerializableElement(
                        "Repeater", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        ),
                        attenuation1 = element.maxOutputPower
                    )

                    is Element.Splitter2 -> SerializableElement(
                        "Splitter2", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Splitter3 -> SerializableElement(
                        "Splitter3", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Splitter4 -> SerializableElement(
                        "Splitter4", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        )
                    )

                    is Element.Coupler -> SerializableElement(
                        "Coupler", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        ),
                        attenuation1 = element.attenuation1,
                        attenuation2 = element.attenuation2
                    )

                    is Element.Booster -> SerializableElement(
                        "Booster", element.id, element.signalPower, element.endElementId,
                        SerializableCable(
                            length = element.cable.length,
                            type = element.cable.type.name,
                            isTwoCorners = element.cable.isTwoCorners,
                            isSideThenDown = element.cable.isSideThenDown,
                            isStraightLine = element.cable.isStraightLine
                        ),
                        attenuation1 = element.maxOutputPower
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
                val cableType = CableType.valueOf(it.type)
                Cable(
                    length = it.length,
                    type = cableType,
                    isTwoCorners = it.isTwoCorners,
                    isSideThenDown = it.isSideThenDown,
                    isStraightLine = it.isStraightLine
                )
            } ?: Cable()
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
                    id = e.id,
                    signalPower = e.signalPower ?: 50.0,
                    maxOutputPower = e.attenuation1 ?: 33.0,
                    endElementId = e.endElementId ?: -1,
                    cable = cable
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

                "Coupler" -> Element.Coupler(
                    id = e.id,
                    attenuation1 = e.attenuation1 ?: 0.0,
                    attenuation2 = e.attenuation2 ?: 0.0,
                    signalPower = e.signalPower ?: 0.0,
                    endElementId = e.endElementId ?: -1,
                    cable = cable
                )

                "Booster" -> Element.Booster(
                    id = e.id,
                    maxOutputPower = e.attenuation1 ?: 0.0,
                    signalPower = e.signalPower ?: 0.0,
                    endElementId = e.endElementId ?: -1,
                    cable = cable
                )

                else -> null
            }
            matrix[row, col] = element
        }
    }
    return matrix
}

expect fun saveElementMatrixToFile(matrix: ElementMatrix, filename: String)