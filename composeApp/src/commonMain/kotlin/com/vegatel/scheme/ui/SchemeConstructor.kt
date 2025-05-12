package com.vegatel.scheme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.TopElement
import com.vegatel.scheme.model.calculateSignalAtRepeater
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SchemeConstructor() {
    // Состояния схемы
    var topElement by remember { mutableStateOf<TopElement>(TopElement.Antenna(signalPower = 35.0)) }
    var cable by remember {
        mutableStateOf(
            Cable(
                length = 10.0,
                thickness = 2,
                lossPerMeter = 0.5
            )
        )
    }

    // Состояния меню и их позиции
    val density = LocalDensity.current

    var showTopMenu by remember { mutableStateOf(false) }
    var topMenuOffsetPx by remember { mutableStateOf(IntOffset.Zero) }
    val topMenuOffsetDp = with(density) {
        DpOffset(topMenuOffsetPx.x.toDp(), topMenuOffsetPx.y.toDp())
    }

    var showCableMenu by remember { mutableStateOf(false) }
    var cableMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val cableMenuOffsetDp = with(density) {
        DpOffset(cableMenuOffset.x.toDp(), cableMenuOffset.y.toDp())
    }

    // Геометрия схемы
    val width = 300.dp
    val height = 400.dp
    val topY = 60f
    val bottomY = 340f
    val centerX = 150f


    val signalAtRepeater = calculateSignalAtRepeater(topElement, cable)

    Box(
        Modifier
            .size(width, height)
            .background(Color.White)
    ) {
        // Верхний элемент (Антенна или Нагрузка)
        val elementOffset = IntOffset(centerX.toInt() - 30, topY.toInt())
        when (val el = topElement) {
            is TopElement.Antenna -> AntennaView(
                signalPower = el.signalPower,
                onClick = { offset ->
                    showTopMenu = true
                    topMenuOffsetPx = IntOffset(offset.x, offset.y)
                },
                modifier = Modifier
                    .offset { elementOffset }
            )

            is TopElement.Load -> LoadView(
                resistance = el.resistance,
                onClick = { offset ->
                    showTopMenu = true
                    topMenuOffsetPx = IntOffset(offset.x, offset.y)
                },
                modifier = Modifier
                    .offset { elementOffset }
            )
        }

        // Репитер всегда внизу по центру
        RepeaterView(
            signalPower = signalAtRepeater,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Меню для верхнего элемента
        DropdownMenu(
            expanded = showTopMenu,
            onDismissRequest = { showTopMenu = false },
            offset = topMenuOffsetDp
        ) {
            DropdownMenuItem(onClick = {
                topElement = TopElement.Antenna(signalPower = 35.0)
                showTopMenu = false
            }) { Text("Антенна (35 дБм)") }
            DropdownMenuItem(onClick = {
                topElement = TopElement.Load(resistance = 50.0)
                showTopMenu = false
            }) { Text("Нагрузка (50 Ом)") }
        }

        // Меню для кабеля
        DropdownMenu(
            expanded = showCableMenu,
            onDismissRequest = { showCableMenu = false },
            offset = cableMenuOffsetDp
        ) {
            listOf(1, 2, 3).forEach { thickness ->
                DropdownMenuItem(onClick = {
                    cable = cable.copy(thickness = thickness)
                    showCableMenu = false
                }) { Text("Толщина $thickness") }
            }
            DropdownMenuItem(onClick = {
                cable = cable.copy(length = cable.length + 5)
                showCableMenu = false
            }) { Text("Увеличить длину на 5м") }
            DropdownMenuItem(onClick = {
                cable = cable.copy(length = (cable.length - 5).coerceAtLeast(1.0))
                showCableMenu = false
            }) { Text("Уменьшить длину на 5м") }
        }
    }
}

@Composable
@Preview
private fun preview() {
    SchemeConstructor()
}