package com.vegatel.scheme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.vegatel.scheme.model.Cable

@Composable
fun CableView(
    start: Offset,
    end: Offset,
    cable: Cable,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawLine(
            color = Color.Black,
            start = start,
            end = end,
            strokeWidth = cable.thickness.toFloat(),
            cap = StrokeCap.Round
        )
    }
}