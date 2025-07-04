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
import com.vegatel.scheme.model.Element
import com.vegatel.scheme.model.ElementMatrix

@Composable
fun CableLengthDialog(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit,
    cableLengthDialogState: Pair<Int, Int>?,
    onCableLengthDialogStateChange: (Pair<Int, Int>?) -> Unit,
    cableLengthInput: TextFieldValue,
    onCableLengthInputChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester
) {
    if (cableLengthDialogState != null) {
        AlertDialog(
            onDismissRequest = { onCableLengthDialogStateChange(null) },
            title = { Text("Укажите длину кабеля") },
            text = {
                TextField(
                    value = cableLengthInput,
                    onValueChange = { input ->
                        // Заменяем запятую на точку и удаляем пробелы
                        val withDot = input.text.replace(",", ".").replace(" ", "")

                        // Разрешаем ввод только цифр и одной точки
                        if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                            when {
                                // Пустая строка или одна точка - разрешаем
                                withDot.isEmpty() || withDot == "." -> {
                                    onCableLengthInputChange(
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
                                            onCableLengthInputChange(
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
                            val (row, col) = cableLengthDialogState
                            val element = newElements[row, col]
                            if (element != null) {
                                val newCable = element.fetchCable().copy(
                                    length = cableLengthInput.text.toDoubleOrNull() ?: 0.0
                                )
                                newElements[row, col] = when (element) {
                                    is Element.Antenna -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Load -> element.copy(cable = newCable)
                                    is Element.Combiner2 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Combiner3 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Combiner4 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Repeater -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Splitter2 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Splitter3 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Splitter4 -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Coupler -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Booster -> element.copy(
                                        cable = newCable
                                    )

                                    is Element.Attenuator -> element.copy(
                                        cable = newCable
                                    )
                                }
                                onElementsChange(newElements)
                            }
                            onCableLengthDialogStateChange(null)
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
                    val (row, col) = cableLengthDialogState
                    val element = newElements[row, col]
                    if (element != null) {
                        val newCable = element.fetchCable().copy(
                            length = cableLengthInput.text.toDoubleOrNull() ?: 0.0
                        )
                        newElements[row, col] = when (element) {
                            is Element.Antenna -> element.copy(cable = newCable)
                            is Element.Load -> element.copy(cable = newCable)
                            is Element.Combiner2 -> element.copy(cable = newCable)
                            is Element.Combiner3 -> element.copy(cable = newCable)
                            is Element.Combiner4 -> element.copy(cable = newCable)
                            is Element.Repeater -> element.copy(cable = newCable)
                            is Element.Splitter2 -> element.copy(cable = newCable)
                            is Element.Splitter3 -> element.copy(cable = newCable)
                            is Element.Splitter4 -> element.copy(cable = newCable)
                            is Element.Coupler -> element.copy(cable = newCable)
                            is Element.Booster -> element.copy(cable = newCable)
                            is Element.Attenuator -> element.copy(cable = newCable)
                        }
                        onElementsChange(newElements)
                    }
                    onCableLengthDialogStateChange(null)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { onCableLengthDialogStateChange(null) }) {
                    Text("Отмена")
                }
            }
        )
    }
} 