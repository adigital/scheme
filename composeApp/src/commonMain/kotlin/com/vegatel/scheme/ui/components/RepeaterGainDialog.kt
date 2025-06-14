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
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.ElementMatrix

@Composable
fun RepeaterGainDialog(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit,
    repeaterGainDialogState: Pair<Int, Int>?,
    onRepeaterGainDialogStateChange: (Pair<Int, Int>?) -> Unit,
    repeaterGainInput: TextFieldValue,
    onRepeaterGainInputChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester
) {
    if (repeaterGainDialogState != null) {
        AlertDialog(
            onDismissRequest = { onRepeaterGainDialogStateChange(null) },
            title = { Text("Укажите усиление репитера") },
            text = {
                TextField(
                    value = repeaterGainInput,
                    onValueChange = { input ->
                        // Заменяем запятую на точку и удаляем пробелы
                        val withDot = input.text.replace(",", ".").replace(" ", "")

                        // Разрешаем ввод только цифр и одной точки
                        if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                            when {
                                // Пустая строка или одна точка - разрешаем
                                withDot.isEmpty() || withDot == "." -> {
                                    onRepeaterGainInputChange(
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
                                            onRepeaterGainInputChange(
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
                            val newElements = elements.copy()
                            val (row, col) = repeaterGainDialogState
                            val element = newElements[row, col]
                            if (element is Repeater) {
                                newElements[row, col] = element.copy(
                                    signalPower = repeaterGainInput.text.toDoubleOrNull() ?: 50.0
                                )
                                onElementsChange(newElements)
                            }
                            onRepeaterGainDialogStateChange(null)
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
                    val newElements = elements.copy()
                    val (row, col) = repeaterGainDialogState
                    val element = newElements[row, col]
                    if (element is Repeater) {
                        newElements[row, col] = element.copy(
                            signalPower = repeaterGainInput.text.toDoubleOrNull() ?: 50.0
                        )
                        onElementsChange(newElements)
                    }
                    onRepeaterGainDialogStateChange(null)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { onRepeaterGainDialogStateChange(null) }) {
                    Text("Отмена")
                }
            }
        )
    }
} 