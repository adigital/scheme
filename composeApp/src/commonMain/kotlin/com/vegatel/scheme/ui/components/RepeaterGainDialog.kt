package com.vegatel.scheme.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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
        val (dialogRow, dialogCol) = repeaterGainDialogState
        val currentElement = elements[dialogRow, dialogCol] as? Repeater
        var maxOutputInput by remember {
            mutableStateOf(TextFieldValue((currentElement?.maxOutputPower ?: 33.0).toString()))
        }

        AlertDialog(
            onDismissRequest = { onRepeaterGainDialogStateChange(null) },
            title = { Text("Параметры репитера") },
            text = {
                Column {
                    Text("Усиление, дБ")

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
                                        signalPower = repeaterGainInput.text.toDoubleOrNull()
                                            ?: element.signalPower,
                                        maxOutputPower = maxOutputInput.text.toDoubleOrNull()
                                            ?: element.maxOutputPower
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Максимальная выходная мощность, дБм")

                    TextField(
                        value = maxOutputInput,
                        onValueChange = { input ->
                            val withDot = input.text.replace(",", ".").replace(" ", "")
                            if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                                when {
                                    withDot.isEmpty() || withDot == "." -> {
                                        maxOutputInput = TextFieldValue(
                                            text = withDot,
                                            selection = input.selection
                                        )
                                    }

                                    else -> {
                                        withDot.toDoubleOrNull()?.let { value ->
                                            if (value in 0.0..50.0) {
                                                maxOutputInput = TextFieldValue(
                                                    text = withDot,
                                                    selection = input.selection
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
                                val (r, c) = repeaterGainDialogState
                                val element = newElements[r, c]
                                if (element is Repeater) {
                                    newElements[r, c] = element.copy(
                                        signalPower = repeaterGainInput.text.toDoubleOrNull()
                                            ?: element.signalPower,
                                        maxOutputPower = maxOutputInput.text.toDoubleOrNull()
                                            ?: element.maxOutputPower
                                    )
                                    onElementsChange(newElements)
                                }
                                onRepeaterGainDialogStateChange(null)
                            }
                        ),
                        placeholder = { Text("0.0 - 50.0 дБм") },
                        singleLine = true
                    )
                }

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
                            signalPower = repeaterGainInput.text.toDoubleOrNull()
                                ?: element.signalPower,
                            maxOutputPower = maxOutputInput.text.toDoubleOrNull()
                                ?: element.maxOutputPower
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