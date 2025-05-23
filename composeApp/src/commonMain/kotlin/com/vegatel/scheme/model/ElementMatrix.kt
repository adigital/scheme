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
                ((element is Element.Splitter2 && element.endElementId == REPEATER_ID) ||
                        element is Element.Splitter4 && element.endElementId == REPEATER_ID)
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

    // Находит все элементы, подключенные к указанному элементу (с endElementId = elementId)
    fun findConnectedElements(elementId: Int): List<Triple<Int, Int, Element>> {
        val result = mutableListOf<Triple<Int, Int, Element>>()
        forEachElement { row, col, element ->
            if (element != null && element.fetchEndElementId() == elementId) {
                result.add(Triple(row, col, element))
            }
        }
        return result
    }

    // Сдвигает элементы вправо в указанном диапазоне строк с учетом связанных элементов
    fun shiftElementsRightInRows(fromCol: Int, startRow: Int, endRow: Int = rowCount - 1) {
        // Проверяем, нужно ли увеличить матрицу
        if (colCount == 0) {
            insertCol(0)
            return
        }

        // Добавляем столбец в конец
        for (row in matrix) {
            row.add(null)
        }

        // Собираем информацию о сплиттерах и их связанных элементах
        val splitterConnections = mutableMapOf<Int, List<Triple<Int, Int, Element>>>()

        // Находим все сплиттеры в сдвигаемой области и их связанные элементы
        forEachElement { row, col, element ->
            if (row in startRow..endRow && col >= fromCol && element is Element.Splitter2) {
                splitterConnections[element.id] = findConnectedElements(element.id)
            }
        }

        // Сначала удаляем все связанные элементы
        splitterConnections.values.flatten().forEach { (row, col, _) ->
            matrix[row][col] = null
        }

        // Сдвигаем элементы вправо
        for (row in startRow..endRow) {
            for (col in (colCount - 2) downTo fromCol) {
                matrix[row][col + 1] = matrix[row][col]
                matrix[row][col] = null
            }
        }

        // Восстанавливаем связанные элементы в новых позициях
        splitterConnections.forEach { (splitterId, connectedElements) ->
            // Находим новую позицию сплиттера
            val splitterPos = findElementById(splitterId) ?: return@forEach
            val (_, splitterCol) = splitterPos

            // Для каждого связанного элемента вычисляем новую позицию
            connectedElements.forEach { (originalRow, originalCol, element) ->
                // Определяем смещение относительно сплиттера
                val colOffset = originalCol - (splitterCol - 1)
                // Размещаем элемент в новой позиции
                matrix[originalRow][splitterCol + colOffset - 1] = element
            }
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