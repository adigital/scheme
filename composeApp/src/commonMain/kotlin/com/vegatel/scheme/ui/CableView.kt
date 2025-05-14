package com.vegatel.scheme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.vegatel.scheme.model.Cable

@Composable
fun CableView(
    start: Offset,
    end: Offset,
    isTwoCorners: Boolean = false,
    cable: Cable,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4 * cable.thickness.toFloat()

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
            color = Color.Black,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}