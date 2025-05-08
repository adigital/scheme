package com.vegatel.scheme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun CableView(
    start: Offset,
    end: Offset,
    thickness: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val clickableZone = with(density) { 24.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(thickness) {
                detectTapGestures { offset ->
                    val distance = distanceToSegment(offset, start, end)
                    if (distance <= clickableZone) {
                        onClick()
                    }
                }
            }
    ) {
        drawLine(
            color = Color.Black,
            start = start,
            end = end,
            strokeWidth = thickness.toFloat() * 1.5f,
            cap = StrokeCap.Round
        )
    }
}

// Вспомогательная функция для вычисления расстояния от точки до отрезка
fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val ap = p - a
    val abLenSq = ab.getDistanceSquared()
    val t = if (abLenSq == 0f) 0f else (ap dot ab) / abLenSq
    val tClamped = t.coerceIn(0f, 1f)
    val closest = a + ab * tClamped
    return (p - closest).getDistance()
}

private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
private operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
private operator fun Offset.times(scalar: Float) = Offset(x * scalar, y * scalar)
private infix fun Offset.dot(other: Offset) = x * other.x + y * other.y
private fun Offset.getDistance() = kotlin.math.sqrt(x * x + y * y)
private fun Offset.getDistanceSquared() = x * x + y * y