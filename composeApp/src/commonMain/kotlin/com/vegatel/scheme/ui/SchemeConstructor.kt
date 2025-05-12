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
import com.vegatel.scheme.model.EndElement.Repeater
import com.vegatel.scheme.model.TopElement
import com.vegatel.scheme.model.calculateSignalAtRepeater
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SchemeConstructor() {
    // Пример: два верхних элемента
    var topElements by remember {
        mutableStateOf(
            listOf<TopElement>(
                TopElement.Antenna(signalPower = 35.0, endElement = Repeater()),
                TopElement.Load(endElement = Repeater())
            )
        )
    }
    var cable by remember {
        mutableStateOf(
            Cable(
                length = 10.0,
                thickness = 2,
                lossPerMeter = 0.5
            )
        )
    }

    // Индекс элемента, для которого открыто меню, или null если ни для кого
    var topMenuOpenedForIndex by remember { mutableStateOf<Int?>(null) }

    var showCableMenu by remember { mutableStateOf(false) }
    var cableMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    val cableMenuOffsetDp = with(density) {
        DpOffset(cableMenuOffset.x.toDp(), cableMenuOffset.y.toDp())
    }

    // Геометрия схемы
    val width = 400.dp
    val height = 400.dp
    val topY = 60f
    val elementSpacing = 250
    val centerX = 150

    val signalAtRepeater = calculateSignalAtRepeater(topElements.first(), cable)

    Box(
        Modifier
            .size(width, height)
            .background(Color.White)
    ) {
        // Рисуем все верхние элементы
        topElements.forEachIndexed { index, el ->
            val elementOffset = IntOffset(centerX + index * elementSpacing - 30, topY.toInt())

            Box(
                modifier = Modifier.offset { elementOffset }
            ) {
                when (el) {
                    is TopElement.Antenna -> {
                        AntennaView(
                            signalPower = el.signalPower,
                            onClick = { topMenuOpenedForIndex = index }
                        )
                    }

                    is TopElement.Load -> {
                        LoadView(
                            onClick = { topMenuOpenedForIndex = index }
                        )
                    }
                }

                // Меню только для выбранного элемента
                DropdownMenu(
                    expanded = topMenuOpenedForIndex == index,
                    onDismissRequest = { topMenuOpenedForIndex = null }
                ) {
                    DropdownMenuItem(onClick = {
                        topElements = topElements.toMutableList().also {
                            it[index] =
                                TopElement.Antenna(signalPower = 35.0, endElement = Repeater())
                        }
                        topMenuOpenedForIndex = null
                    }) { Text("Антенна (35 дБм)") }
                    DropdownMenuItem(onClick = {
                        topElements = topElements.toMutableList().also {
                            it[index] = TopElement.Load(endElement = Repeater())
                        }
                        topMenuOpenedForIndex = null
                    }) { Text("Нагрузка") }
                }
            }
        }

        // Репитер всегда внизу по центру
        RepeaterView(
            signalPower = signalAtRepeater,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Меню для кабеля (оставляем как было)
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
