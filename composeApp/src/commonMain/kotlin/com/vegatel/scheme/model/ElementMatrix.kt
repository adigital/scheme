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

    // Сдвигает элементы в указанной строке вправо
    fun shiftRowElementsRight(row: Int, fromCol: Int) {
        // Проверяем, нужно ли увеличить матрицу
        if (colCount == 0) {
            insertCol(0)
            return
        }

        // Добавляем столбец в конец
        for (r in matrix) {
            r.add(null)
        }

        // Сдвигаем элементы вправо только в указанной строке
        for (col in (colCount - 2) downTo fromCol) {
            matrix[row][col + 1] = matrix[row][col]
            matrix[row][col] = null
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

    // Проверяет, находится ли элемент ниже репитера
    fun isElementBelowRepeater(elementId: Int): Boolean {
        // Сначала найдем репитер в матрице
        var repeaterRow = -1
        var elementRow = -1

        forEachElement { row, col, element ->
            if (element?.id == elementId) {
                elementRow = row
            }
            if (element is Element.Repeater) {
                repeaterRow = row
            }
        }

        // Если нашли и элемент, и репитер, сравниваем их позиции
        if (repeaterRow != -1 && elementRow != -1) {
            return elementRow > repeaterRow
        }

        return false
    }

    // Удаляет все элементы, подключенные к указанному элементу сверху
    fun removeConnectedElementsAbove(elementId: Int) {
        // Находим все элементы, которые подключены к данному элементу (endElementId == elementId)
        forEachElement { row, col, element ->
            if (element?.fetchEndElementId() == elementId) {
                // Если найденный элемент - сумматор или сплиттер, рекурсивно удаляем его подключения
                if (element is Element.Combiner2 || element is Element.Combiner3 || element is Element.Combiner4 ||
                    element is Element.Splitter2 || element is Element.Splitter3 || element is Element.Splitter4
                ) {
                    removeConnectedElementsAbove(element.id)
                }
                // Не удаляем репитер!
                if (element !is Element.Repeater) {
                    this[row, col] = null
                }
            }
        }
    }

    // Оптимизирует пустое пространство в матрице
    fun optimizeSpace() {
        // Находим пустые столбцы
        val emptyColumns = mutableListOf<Int>()
        for (col in 0 until colCount) {
            var isEmpty = true
            for (row in 0 until rowCount) {
                if (this[row, col] != null) {
                    isEmpty = false
                    break
                }
            }
            if (isEmpty) {
                emptyColumns.add(col)
            }
        }

        // Удаляем пустые столбцы справа налево
        emptyColumns.sortedDescending().forEach { col ->
            // Проверяем, не является ли этот столбец единственным
            if (colCount > 1) {
                removeCol(col)
            }
        }
    }
}