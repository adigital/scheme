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
        // Увеличиваем размер матрицы, если необходимо
        ensureSize(row + 1, col + 1)
        matrix[row][col] = value
    }

    // Обеспечивает нужный размер матрицы
    private fun ensureSize(requiredRows: Int, requiredCols: Int) {
        // Добавляем строки, если нужно
        while (rowCount < requiredRows) {
            matrix.add(MutableList(colCount) { null })
        }

        // Добавляем столбцы, если нужно
        if (requiredCols > colCount) {
            for (row in matrix) {
                while (row.size < requiredCols) {
                    row.add(null)
                }
            }
        }
    }

    fun insertRow(index: Int) {
        if (matrix.isEmpty()) {
            matrix.add(MutableList(1) { null })
            return
        }
        matrix.add(index, MutableList(colCount) { null })
    }

    fun removeRow(index: Int) {
        if (matrix.isNotEmpty()) matrix.removeAt(index)
    }

    fun insertCol(index: Int) {
        if (matrix.isEmpty()) {
            matrix.add(MutableList(1) { null })
            return
        }
        for (row in matrix) {
            row.add(index, null)
        }
    }

    fun removeCol(index: Int) {
        for (row in matrix) {
            if (row.isNotEmpty()) row.removeAt(index)
        }
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

    fun findElementById(id: Int): Pair<Int, Int>? {
        var result: Pair<Int, Int>? = null

        forEachElement { row, col, element ->
            if (element?.id == id) {
                result = row to col
            }
        }

        return result
    }

    fun isRepeaterHalfShiftRender(): Boolean {
        var result = false

        forEachElement { row, col, element ->
            if (element?.isHalfShiftRender() == true &&
                ((element is Element.Combiner2 && element.endElementId == REPEATER_ID) ||
                        (element is Element.Combiner4 && element.endElementId == REPEATER_ID) ||
                        (element is Element.Splitter2 && element.id == getNextForRepeaterElementId()) ||
                        (element is Element.Splitter4 && element.id == getNextForRepeaterElementId()))
            ) {
                result = true
                return@forEachElement
            }
        }

        return result
    }

    fun getNextForRepeaterElementId(): Int {
        var endElementId = -1

        forEachElement { row, col, element ->
            if (element is Element.Repeater) {
                endElementId = element.endElementId
                return@forEachElement
            }
        }

        return endElementId
    }

    // Проверяет, есть ли элемент в указанной позиции
    fun hasElementAt(row: Int, col: Int): Boolean {
        return if (row < 0 || row >= rowCount || col < 0 || col >= colCount) {
            false
        } else {
            matrix[row][col] != null
        }
    }

    // Сдвигает всю колонку и элементы правее неё
    fun shiftColumnAndRightElementsRight(fromCol: Int) {
        // Проверяем, нужно ли увеличить матрицу
        if (colCount == 0) {
            insertCol(0)
            return
        }

        // Добавляем столбец в конец
        for (row in matrix) {
            row.add(null)
        }

        // Сдвигаем элементы вправо во всех строках
        for (row in 0 until rowCount) {
            for (col in (colCount - 2) downTo fromCol) {
                matrix[row][col + 1] = matrix[row][col]
                matrix[row][col] = null
            }
        }
    }

    // Генерирует новый уникальный id для элемента
    fun generateNewId(): Int {
        var maxId = 0
        forEachElement { _, _, element ->
            if (element != null && element.id > maxId) {
                maxId = element.id
            }
        }
        return maxId + 1
    }
}