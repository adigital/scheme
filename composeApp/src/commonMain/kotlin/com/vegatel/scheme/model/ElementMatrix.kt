package com.vegatel.scheme.model

import androidx.compose.runtime.Composable

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