package com.vegatel.scheme.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.extensions.displayFileName

@Composable
fun MainMenu(
    fileName: String?,
    isDirty: Boolean,
    onNew: () -> Unit = {},
    onOpen: () -> Unit = {},
    onSave: () -> Unit = {},
    onSaveAs: () -> Unit = {},

    onCalc1: () -> Unit = {},
    onCalc2: () -> Unit = {},
    onCalc3: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var openSubmenu1Expanded by remember { mutableStateOf(false) }
    var openSubmenu2Expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.padding(start = 16.dp)) {
        Button(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = null)
        }

        Text(
            text = buildString {
                append(displayFileName(fileName))
                if (isDirty) append(" *")
            },
            modifier = Modifier.padding(start = 8.dp)
        )

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

                    DropdownMenuItem(onClick = {
                        expanded = false
                        openSubmenu1Expanded = false
                        onSave()
                    }) {
                        Text("Сохранить")
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
}