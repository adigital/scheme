// D:/Work/Vegatel/Scheme/composeApp/src/commonMain/kotlin/com/vegatel/scheme/ui/SchemeConstructor.kt
package com.vegatel.scheme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.Repeater
import com.vegatel.scheme.model.TopElement
import com.vegatel.scheme.model.calculateSignalAtRepeater
import kotlin.math.roundToInt

@Composable
fun SchemeConstructor() {
    // Состояния схемы
    var topElement by remember { mutableStateOf<TopElement>(TopElement.Antenna(signalPower = 35.0)) }
    var cable by remember {
        mutableStateOf(
            Cable(
                length = 10.0,
                thickness = 2,
                lossPerMeter = 0.5
            )
        )
    }
    val repeater = remember { Repeater() }

    // Состояния меню и их позиции
    var showTopMenu by remember { mutableStateOf(false) }
    var topMenuOffsetPx by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    val topMenuOffsetDp = with(density) {
        DpOffset(topMenuOffsetPx.x.toDp(), topMenuOffsetPx.y.toDp())
    }
    var showCableMenu by remember { mutableStateOf(false) }
    var cableMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val cableMenuOffsetDp = with(density) {
        DpOffset(cableMenuOffset.x.toDp(), cableMenuOffset.y.toDp())
    }

    // Геометрия схемы
    val width = 300.dp
    val height = 400.dp
    val topY = 60f
    val bottomY = 340f
    val centerX = 150f

    val signalAtRepeater = calculateSignalAtRepeater(topElement, cable)

    Box(
        Modifier
            .size(width, height)
            .background(Color.White)
    ) {
        // Кабель (линия) с расширенной зоной клика
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Проверяем, попал ли клик в зону кабеля (расширяем до 24dp)
                        val cableStart = Offset(centerX, topY + 40)
                        val cableEnd = Offset(centerX, bottomY - 40)
                        val clickZonePx = with(density) { 24.dp.toPx() }
                        val inCableZone = isPointNearLine(
                            offset,
                            cableStart,
                            cableEnd,
                            clickZonePx
                        )
                        if (inCableZone) {
                            showCableMenu = true
                            cableMenuOffset =
                                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                        }
                    }
                }
        ) {
            drawLine(
                color = Color.Black,
                start = Offset(centerX, topY + 40),
                end = Offset(centerX, bottomY - 40),
                strokeWidth = cable.thickness.toFloat() * 2,
                cap = StrokeCap.Round
            )
        }

        // Верхний элемент (Антенна или Нагрузка)
        val elementOffset = IntOffset(centerX.toInt() - 30, topY.toInt())
        when (val el = topElement) {
            is TopElement.Antenna -> AntennaView(
                signalPower = el.signalPower,
                onClick = { offset ->
                    showTopMenu = true
                    topMenuOffsetPx = IntOffset(offset.x, offset.y)
                },
                modifier = Modifier
                    .offset { elementOffset }
            )

            is TopElement.Load -> LoadView(
                resistance = el.resistance,
                onClick = { offset ->
                    showTopMenu = true
                    topMenuOffsetPx = IntOffset(offset.x, offset.y)
                },
                modifier = Modifier
                    .offset { elementOffset }
            )
        }

        // Репитер всегда внизу по центру
        RepeaterView(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Отображение потерь сигнала
        Text(
            text = "Сигнал на репитере: ${"%.2f".format(signalAtRepeater)}",
            color = Color.DarkGray,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )

        // Меню для верхнего элемента
        DropdownMenu(
            expanded = showTopMenu,
            onDismissRequest = { showTopMenu = false },
            offset = topMenuOffsetDp
        ) {
            DropdownMenuItem(onClick = {
                topElement = TopElement.Antenna(signalPower = 35.0)
                showTopMenu = false
            }) { Text("Антенна (35 дБм)") }
            DropdownMenuItem(onClick = {
                topElement = TopElement.Load(resistance = 50.0)
                showTopMenu = false
            }) { Text("Нагрузка (50 Ом)") }
        }

        // Меню для кабеля
        DropdownMenu(
            expanded = showCableMenu,
            onDismissRequest = { showCableMenu = false },
            offset = cableMenuOffsetDp
        ) {
            listOf(1, 2, 3).forEach { thickness ->
                DropdownMenuItem(onClick = {
                    cable = cable.copy(thickness = thickness)
                    showCableMenu = false
                }) { Text("Толщина $thickness") }
            }
            DropdownMenuItem(onClick = {
                cable = cable.copy(length = cable.length + 5)
                showCableMenu = false
            }) { Text("Увеличить длину на 5м") }
            DropdownMenuItem(onClick = {
                cable = cable.copy(length = (cable.length - 5).coerceAtLeast(1.0))
                showCableMenu = false
            }) { Text("Уменьшить длину на 5м") }
        }
    }
}

// Проверка попадания точки в зону вокруг линии
fun isPointNearLine(
    point: Offset,
    lineStart: Offset,
    lineEnd: Offset,
    tolerance: Float
): Boolean {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared == 0f) return false
    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared
    val nearest = when {
        t < 0f -> lineStart
        t > 1f -> lineEnd
        else -> Offset(lineStart.x + t * dx, lineStart.y + t * dy)
    }
    val distance = (point - nearest).getDistance()
    return distance <= tolerance
}