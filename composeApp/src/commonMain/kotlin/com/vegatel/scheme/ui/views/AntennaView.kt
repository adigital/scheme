package com.vegatel.scheme.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vegatel.scheme.model.Element.Antenna
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import scheme.composeapp.generated.resources.Res
import scheme.composeapp.generated.resources.antenna
import scheme.composeapp.generated.resources.antenna_11y
import scheme.composeapp.generated.resources.antenna_fi
import scheme.composeapp.generated.resources.antenna_wi

@Composable
fun AntennaView(
    antenna: Antenna,
    signalPower: Double,
    onClick: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    var elementColor by remember { mutableStateOf(Color.Green) }
    var isRed by remember { mutableStateOf(false) }

    LaunchedEffect(isRed) {
        if (isRed) {
            elementColor = Color.Red
            delay(1000)
            elementColor = Color.Green
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
        val iconRes = when (antenna.signalPower) {
            6.0 -> Res.drawable.antenna_fi
            9.0 -> Res.drawable.antenna
            15.0 -> Res.drawable.antenna
            16.0 -> Res.drawable.antenna
            11.0 -> Res.drawable.antenna_11y
            3.0 -> Res.drawable.antenna_wi
            else -> Res.drawable.antenna
        }

        val name = when (antenna.signalPower) {
            6.0 -> "FI"
            9.0 -> "PI"
            15.0 -> "15\nPO"
            16.0 -> "16S"
            11.0 -> "11Y"
            3.0 -> "Wi"
            else -> ""
        }

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = if (antenna.signalPower == 11.0 || antenna.signalPower == 3.0) Alignment.BottomCenter else Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(iconRes),
                contentDescription = "Антенна",
                tint = elementColor
            )

            Text(
                text = name,
                modifier = if (antenna.signalPower == 3.0) Modifier.background(Color(0xAAFFFFFF)) else Modifier,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                color = Color.Red
            )
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
    // Для превью передаем заглушечную антенну FI
    AntennaView(
        antenna = Antenna(id = 0, signalPower = 3.0),
        signalPower = 15.0,
        onClick = {}
    )
}