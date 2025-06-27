package com.vegatel.scheme.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.CableType
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CableView(
    start: Offset,
    end: Offset,
    isTwoCorners: Boolean = false,
    isSideThenDown: Boolean = false,
    isStraightLine: Boolean = false,
    cable: Cable,
    modifier: Modifier = Modifier,
    onClick: (IntOffset) -> Unit
) {
    // Фиксированная толщина кабеля
    val strokeWidthDp = 2.dp
    val density = LocalDensity.current

    // Функция для выбора цвета по типу кабеля
    fun getCableColorByType(type: CableType): Color = when (type) {
        CableType.CF_HALF -> Color.Blue
        CableType.TEN_D_FB -> Color.Magenta
        CableType.EIGHT_D_FB -> Color.Black
        CableType.FIVE_D_FB -> Color.Red
        CableType.OPTICAL -> Color(0xFF99FF99)
    }

    // Состояние для цвета линии
    var cableColor by remember { mutableStateOf(getCableColorByType(cable.type)) }
    var isRed by remember { mutableStateOf(false) }

    // Вычисляем сегменты линии и центральные точки
    val segments = if (isStraightLine) {
        // Для прямой линии - один сегмент от start до end
        listOf(Pair(start, end))
    } else {
        // Для изогнутой линии - как раньше
        when {
            isTwoCorners -> {
                val midY = (start.y + end.y) / 2
                listOf(
                    Pair(start, Offset(start.x, midY)),
                    Pair(Offset(start.x, midY), Offset(end.x, midY)),
                    Pair(Offset(end.x, midY), end)
                )
            }

            isSideThenDown -> {
                listOf(
                    Pair(start, Offset(end.x, start.y)),
                    Pair(Offset(end.x, start.y), end)
                )
            }

            else -> {
                listOf(
                    Pair(start, Offset(start.x, end.y)),
                    Pair(Offset(start.x, end.y), end)
                )
            }
        }
    }

    // Находим центральную точку для текста
    val centerPoint =
        Offset((start.x + end.x) / 2, (start.y + end.y) / 2)

    // Слой с Box для каждого сегмента
    segments.forEach { (segStart, segEnd) ->
        val centerX = (segStart.x + segEnd.x) / 2
        val centerY = (segStart.y + segEnd.y) / 2
        val isHorizontal = segStart.y == segEnd.y
        val isVertical = segStart.x == segEnd.x

        with(density) {
            val boxWidthPx = abs(segEnd.x - segStart.x).takeIf { it > 0f } ?: strokeWidthDp.toPx()
            val boxHeightPx = abs(segEnd.y - segStart.y).takeIf { it > 0f } ?: strokeWidthDp.toPx()

            val boxWidthDp = if (isHorizontal) boxWidthPx.toDp() else strokeWidthDp
            val boxHeightDp = if (isVertical) boxHeightPx.toDp() else strokeWidthDp

            val minSize = 32.dp // Ширина зоны клика
            val finalWidth = max(boxWidthDp.value, minSize.value).dp
            val finalHeight = max(boxHeightDp.value, minSize.value).dp

            val offsetX = (centerX - finalWidth.toPx() / 2).roundToInt()
            val offsetY = (centerY - finalHeight.toPx() / 2).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(finalWidth, finalHeight)
//                    .background(Color(0x88FF00FF)) // Фон кликабельной зоны
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                            isRed = true
                        }
                    },
            )
        }
    }

    // Эффект для смены цвета на 1 секунду
    LaunchedEffect(isRed, cable.type) {
        if (isRed) {
            cableColor = Color.Red
            delay(1000)
            cableColor = getCableColorByType(cable.type)
            isRed = false
        } else {
            cableColor = getCableColorByType(cable.type)
        }
    }

    // Рисуем сам кабель
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(start.x, start.y)
            if (isStraightLine) {
                // Для прямой линии - просто линия от start до end
                lineTo(end.x, end.y)
            } else {
                // Для изогнутой линии - как раньше
                when {
                    isTwoCorners -> {
                        val midY = (start.y + end.y) / 2
                        lineTo(start.x, midY)
                        lineTo(end.x, midY)
                        lineTo(end.x, end.y)
                    }

                    isSideThenDown -> {
                        lineTo(end.x, start.y)
                        lineTo(end.x, end.y)
                    }

                    else -> {
                        lineTo(start.x, end.y)
                        lineTo(end.x, end.y)
                    }
                }
            }
        }

        drawPath(
            path = path,
            color = cableColor,
            style = Stroke(width = strokeWidthDp.toPx())
        )
    }

    // Отображаем длину кабеля
    Box(
        modifier = Modifier.absoluteOffset {
            IntOffset(
                centerPoint.x.toInt() + -8.dp.toPx().toInt(),

                if (isStraightLine) {
                    centerPoint.y.toInt()
                } else {
                    when {
                        !isTwoCorners && !isSideThenDown -> +end.y.toInt()
                        !isTwoCorners && isSideThenDown -> +start.y.toInt()
                        else -> centerPoint.y.toInt()
                    }
                } - 20.dp.toPx().toInt(),
            )
        }
    ) {
        Text(
            text = "${cable.length.toString().replace(Regex("\\.?0*$"), "")}м",
            color = Color.Red,
            style = TextStyle(fontSize = 12.sp)
        )
    }
}