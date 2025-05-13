package com.vegatel.scheme.model

import androidx.compose.runtime.Composable

sealed class Element {

    abstract override fun toString(): String

    data class Antenna(
        val id: Int,
        val signalPower: Double,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Antenna(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Load(
        val id: Int,
        val signalPower: Double = 0.0,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Load(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter2(
        val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter2(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter3(
        val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val topElementId3: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter3(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, topElementId3=$topElementId3, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter4(
        val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val topElementId3: Int,
        val topElementId4: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter4(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, topElementId3=$topElementId3, topElementId4=$topElementId4, endElementId=$endElementId, cable=$cable)"
    }

    data class Repeater(
        val id: Int,
        val topElementId: Int
    ) : Element() {
        override fun toString(): String =
            "Repeater(id=$id, topElementId=$topElementId)"
    }
}

data class Cable(
    val length: Double,
    val thickness: Int, // 1, 2, 3
    val lossPerMeter: Double
)

class ElementMatrix(
    initialRows: Int,
    initialCols: Int
) {
    private val matrix: MutableList<MutableList<Element?>> =
        MutableList(initialRows) { MutableList(initialCols) { null } }

    val rowCount: Int get() = matrix.size
    val colCount: Int get() = if (matrix.isNotEmpty()) matrix[0].size else 0

    operator fun get(row: Int, col: Int): Element? = matrix[row][col]
    operator fun set(row: Int, col: Int, value: Element?) {
        matrix[row][col] = value
    }

    fun insertRow(index: Int) {
        if (matrix.isEmpty()) return
        matrix.add(index, MutableList(colCount) { null })
    }

    fun removeRow(index: Int) {
        if (matrix.isNotEmpty()) matrix.removeAt(index)
    }

    fun insertCol(index: Int) {
        for (row in matrix) {
            row.add(index, null)
        }
    }

    fun removeCol(index: Int) {
        for (row in matrix) {
            if (row.isNotEmpty()) row.removeAt(index)
        }
    }

    fun clear() {
        matrix.clear()
    }

    fun forEachElement(action: (row: Int, col: Int, element: Element?) -> Unit) {
        for (rowIndex in matrix.indices) {
            for (colIndex in matrix[rowIndex].indices) {
                action(rowIndex, colIndex, matrix[rowIndex][colIndex])
            }
        }
    }

    @Composable
    fun forEachElementComposable(action: @Composable (row: Int, col: Int, element: Element?) -> Unit) {
        for (rowIndex in matrix.indices) {
            for (colIndex in matrix[rowIndex].indices) {
                action(rowIndex, colIndex, matrix[rowIndex][colIndex])
            }
        }
    }

    fun copy(): ElementMatrix {
        val newMatrix = ElementMatrix(rowCount, colCount)
        forEachElement { row, col, element ->
            newMatrix[row, col] = element
        }

        return newMatrix
    }
}