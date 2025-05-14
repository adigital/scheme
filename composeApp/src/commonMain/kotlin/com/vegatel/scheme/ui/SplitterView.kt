package com.vegatel.scheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import scheme.composeapp.generated.resources.splitter

@Composable
fun SplitterView(
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
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(19.dp)
                    .height(48.dp)
                    .background(Color.White)
                    .align(Alignment.Center)
            )

            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(Res.drawable.splitter),
                contentDescription = "Сплитер",
                tint = Color.Black
            )
        }

        Text("$signalPower", style = MaterialTheme.typography.caption)
    }
}

@Composable
@Preview
private fun preview() {
    SplitterView(
        signalPower = 35.0,
        onClick = {}
    )
}