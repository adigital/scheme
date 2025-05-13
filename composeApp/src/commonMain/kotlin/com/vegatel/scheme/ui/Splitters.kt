package com.vegatel.scheme.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import scheme.composeapp.generated.resources.Res
import scheme.composeapp.generated.resources.load

@Composable
fun Splitter2(
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(Res.drawable.load),
            contentDescription = "Сплитер 2",
            tint = Color.Black
        )

        Text("$signalPower", style = MaterialTheme.typography.caption)
    }
}

@Composable
fun Splitter3(
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(Res.drawable.load),
            contentDescription = "Сплитер 2",
            tint = Color.Black
        )

        Text("$signalPower", style = MaterialTheme.typography.caption)
    }
}

@Composable
fun Splitter4(
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(Res.drawable.load),
            contentDescription = "Сплитер 2",
            tint = Color.Black
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