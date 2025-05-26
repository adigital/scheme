package com.vegatel.scheme.ui

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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CableView(
    start: Offset,
    end: Offset,
    isTwoCorners: Boolean = false,
    cable: Cable,
    modifier: Modifier = Modifier,
    onClick: (IntOffset) -> Unit
) {
    val strokeWidthDp = (2 * cable.thickness).dp
    val density = LocalDensity.current

    // Функция для выбора цвета по толщине
    fun getCableColorByThickness(thickness: Int): Color = when (thickness) {
        1 -> Color.Black
        2 -> Color.Blue
        3 -> Color.Green
        else -> Color.Black
    }

    // Состояние для цвета линии
    var cableColor by remember { mutableStateOf(getCableColorByThickness(cable.thickness)) }
    var isRed by remember { mutableStateOf(false) }

    // Вычисляем сегменты линии и центральные точки
    val segments = if (isTwoCorners) {
        val midY = (start.y + end.y) / 2
        listOf(
            Pair(start, Offset(start.x, midY)),
            Pair(Offset(start.x, midY), Offset(end.x, midY)),
            Pair(Offset(end.x, midY), end)
        )
    } else {
        listOf(
            Pair(start, Offset(start.x, end.y)),
            Pair(Offset(start.x, end.y), end)
        )
    }

    // Находим центральную точку для текста
    val midX = start.x
    val midY = (start.y + end.y) / 2

    val centerPoint = Offset(midX, midY)

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
    LaunchedEffect(isRed, cable.thickness) {
        if (isRed) {
            cableColor = Color.Red
            delay(1000)
            cableColor = getCableColorByThickness(cable.thickness)
            isRed = false
        } else {
            cableColor = getCableColorByThickness(cable.thickness)
        }
    }

    // Рисуем сам кабель
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(start.x, start.y)
            if (isTwoCorners) {
                val midY = (start.y + end.y) / 2
                lineTo(start.x, midY)
                lineTo(end.x, midY)
                lineTo(end.x, end.y)
            } else {
                lineTo(start.x, end.y)
                lineTo(end.x, end.y)
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
                centerPoint.x.toInt() + 4.dp.toPx().toInt(),
                centerPoint.y.toInt() - 20.dp.toPx().toInt()
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
