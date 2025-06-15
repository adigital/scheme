package com.vegatel.scheme.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun BaseStationSignalDialog(
    baseStationSignalInput: TextFieldValue,
    onBaseStationSignalInputChange: (TextFieldValue) -> Unit,
    onBaseStationSignalChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Укажите уровень сигнала базовой станции") },
        text = {
            TextField(
                value = baseStationSignalInput,
                onValueChange = { input ->
                    // Заменяем запятую на точку и удаляем пробелы
                    val withDot = input.text.replace(",", ".").replace(" ", "")

                    // Разрешаем ввод только цифр и одной точки
                    if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                        when {
                            // Пустая строка или одна точка - разрешаем
                            withDot.isEmpty() || withDot == "." -> {
                                onBaseStationSignalInputChange(
                                    TextFieldValue(
                                        text = withDot,
                                        selection = input.selection
                                    )
                                )
                            }
                            // Если есть число после точки или целое число
                            else -> {
                                withDot.toDoubleOrNull()?.let { value ->
                                    if (value in 0.0..100.0) {
                                        onBaseStationSignalInputChange(
                                            TextFieldValue(
                                                text = withDot,
                                                selection = input.selection
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        baseStationSignalInput.text.toDoubleOrNull()?.let { value ->
                            if (value in 0.0..100.0) {
                                onBaseStationSignalChange(value)
                            }
                        }
                        onDismiss()
                    }
                ),
                placeholder = { Text("0.0 - 100.0") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Button(onClick = {
                baseStationSignalInput.text.toDoubleOrNull()?.let { value ->
                    if (value in 0.0..100.0) {
                        onBaseStationSignalChange(value)
                    }
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
} 