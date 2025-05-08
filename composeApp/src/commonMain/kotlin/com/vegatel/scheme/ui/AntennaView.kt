package com.vegatel.scheme.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun AntennaView(
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .size(60.dp, 40.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // offset - координаты внутри элемента
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Email, contentDescription = "Антенна", tint = Color.Blue)
        Text("Антенна", color = Color.Blue)
        Text("Мощн: $signalPower", style = MaterialTheme.typography.caption)
    }
}
