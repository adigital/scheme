package com.vegatel.scheme.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vegatel.scheme.extensions.displayFileName
import com.vegatel.scheme.getPlatform
import com.vegatel.scheme.ui.components.BaseStationSignalDialog
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun MainMenu(
    fileName: String?,
    isDirty: Boolean,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    baseStationSignal: Double = 30.0,
    frequency: Int = 800,
    onBaseStationSignalChange: (Double) -> Unit = {},
    onFrequencyChange: (Int) -> Unit = {},
    onNew: () -> Unit = {},
    onOpen: () -> Unit = {},
    onSave: () -> Unit = {},
    onSaveAs: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onCalc1: () -> Unit = {},
    onCalc2: () -> Unit = {},
    onCalc3: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var openSubmenu1Expanded by remember { mutableStateOf(false) }
    var openSubmenu2Expanded by remember { mutableStateOf(false) }
    var showBaseStationSignalDialog by remember { mutableStateOf(false) }
    var baseStationSignalInput by remember { mutableStateOf(TextFieldValue(baseStationSignal.toString())) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            )
            .zIndex(1f)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = null)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = buildString {
                    append(displayFileName(fileName))
                    if (isDirty) append(" *")
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.clickable {
                        showFrequencyDropdown = true
                    },
                    text = "$frequency МГц"
                )

                DropdownMenu(
                    expanded = showFrequencyDropdown,
                    onDismissRequest = { showFrequencyDropdown = false }
                ) {
                    listOf(800, 900, 1800, 2100, 2600).forEach { freq ->
                        DropdownMenuItem(onClick = {
                            onFrequencyChange(freq)
                            showFrequencyDropdown = false
                        }) {
                            Text("$freq МГц")
                        }
                    }

                }
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    modifier = Modifier
                        .clickable {
                            showBaseStationSignalDialog = true
                            val text = baseStationSignal.toString()
                            baseStationSignalInput = TextFieldValue(
                                text = text,
                                selection = TextRange(0, text.length)
                            )
                        },
                    text = String.format("%.1f дБм", baseStationSignal)
                )
            }
        }

        IconButton(
            onClick = onUndo,
            enabled = canUndo
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Отменить",
            )
        }

        IconButton(
            onClick = onRedo,
            enabled = canRedo
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Повторить",
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                openSubmenu1Expanded = false
            }
        ) {
            DropdownMenuItem(
                onClick = { openSubmenu1Expanded = true }
            ) {
                Text("Схема")

                DropdownMenu(
                    expanded = openSubmenu1Expanded,
                    onDismissRequest = {
                        expanded = false
                        openSubmenu1Expanded = false
                    }
                ) {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu1Expanded = false
                        onNew()
                    }) {
                        Text("Новая")
                    }

                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu1Expanded = false
                        onOpen()
                    }) {
                        Text("Открыть")
                    }

                    if (getPlatform() == "Desktop") {
                        DropdownMenuItem(onClick = {
                            expanded = false
                            openSubmenu1Expanded = false
                            onSave()
                        }) {
                            Text("Сохранить")
                        }
                    }

                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu1Expanded = false
                        onSaveAs()
                    }) {
                        Text("Сохранить как...")
                    }
                }
            }

            DropdownMenuItem(
                onClick = { openSubmenu2Expanded = true }
            ) {
                Text("Расчёт")

                DropdownMenu(
                    expanded = openSubmenu2Expanded,
                    onDismissRequest = {
                        expanded = false
                        openSubmenu2Expanded = false
                    }
                ) {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu2Expanded = false
                        onCalc1()
                    }) {
                        Text("Подобрать кабель и пассивные устройства (DC, SW)")
                    }

                    Divider()

                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu2Expanded = false
                        onCalc2()
                    }) {
                        Text("Подобрать пассивные устройства (DC, SW)")
                    }

                    Divider()

                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu2Expanded = false
                        onCalc3()
                    }) {
                        Text("Подобрать кабель")
                    }
                }
            }
        }
    }

    if (showBaseStationSignalDialog) {
        BaseStationSignalDialog(
            baseStationSignalInput = baseStationSignalInput,
            onBaseStationSignalInputChange = { baseStationSignalInput = it },
            onBaseStationSignalChange = { value ->
                onBaseStationSignalChange(value)
                showBaseStationSignalDialog = false
            },
            onDismiss = { showBaseStationSignalDialog = false },
            focusRequester = focusRequester
        )
    }
}