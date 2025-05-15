package com.vegatel.scheme.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import scheme.composeapp.generated.resources.Res
import scheme.composeapp.generated.resources.load

@Composable
fun LoadView(
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    var elementColor by remember { mutableStateOf(Color.Black) }
    var isRed by remember { mutableStateOf(false) }

    LaunchedEffect(isRed) {
        if (isRed) {
            elementColor = Color.Red
            delay(1000)
            elementColor = Color.Black
            isRed = false
        }
    }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                    isRed = true
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(Res.drawable.load),
            contentDescription = "Нагрузка",
            tint = elementColor
        )

        Text("$signalPower", style = MaterialTheme.typography.caption)
    }
}

@Composable
@Preview
private fun preview() {
    LoadView(
        signalPower = 35.0,
        onClick = {}
    )
}