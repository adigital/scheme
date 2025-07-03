package com.vegatel.scheme.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun RepeaterView(
    signalPower: Double,
    isOverloaded: Boolean,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    var flash by remember { mutableStateOf(false) }

    LaunchedEffect(flash) {
        if (flash) {
            delay(1000)
            flash = false
        }
    }

    val elementColor = when {
        isOverloaded -> Color.Red
        flash -> Color.Red
        else -> Color.Black
    }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!isOverloaded) flash = true
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .border(4.dp, elementColor, RoundedCornerShape(4.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("REP", color = elementColor)
        }

        Text(
            String.format("%.1f", signalPower),
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
@Preview
private fun preview() {
    RepeaterView(
        signalPower = 30.0,
        isOverloaded = false,
        onClick = {})
}