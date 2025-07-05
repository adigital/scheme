package com.vegatel.scheme.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import scheme.composeapp.generated.resources.Res
import scheme.composeapp.generated.resources.coupler

@Composable
fun CouplerView(
    attenuations: List<Double>,
    signalPowers: List<Double>,
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
                painter = painterResource(Res.drawable.coupler),
                contentDescription = "Ответвитель",
                tint = elementColor
            )

            Row(modifier = Modifier.width(48.dp)) {
                attenuations.forEachIndexed { index, attenuation ->
                    Text(
                        if (attenuation % 1 == 0.0) attenuation.toInt()
                            .toString() else "%.1f".format(attenuation),
                        modifier = Modifier.background(Color.White.copy(alpha = 0.7f)),
                        style = MaterialTheme.typography.caption,
                        fontSize = 9.sp
                    )
                    if (index != attenuations.lastIndex) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(modifier = Modifier.width(48.dp)) {
            signalPowers.forEachIndexed { index, power ->
                Text(
                    String.format("%.1f", power),
                    modifier = Modifier.background(Color.White.copy(alpha = 0.7f)),
                    style = MaterialTheme.typography.caption,
                    fontSize = 9.sp
                )
                if (index != signalPowers.lastIndex) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
@Preview
private fun preview() {
    CouplerView(
        attenuations = listOf(0.3, 25.0),
        signalPowers = listOf(-35.0, -45.0),
        onClick = {}
    )
}