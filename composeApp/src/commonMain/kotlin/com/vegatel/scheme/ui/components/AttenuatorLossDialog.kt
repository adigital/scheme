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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vegatel.scheme.model.Element.Attenuator
import com.vegatel.scheme.model.ElementMatrix

private const val MIN_ATTENUATOR_LOSS = -20.0
private const val MAX_ATTENUATOR_LOSS = 0.0

@Composable
fun AttenuatorLossDialog(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit,
    attenuatorDialogState: Pair<Int, Int>?,
    onDialogStateChange: (Pair<Int, Int>?) -> Unit,
    lossInput: TextFieldValue,
    onLossInputChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester
) {
    if (attenuatorDialogState != null) {
        AlertDialog(
            onDismissRequest = { onDialogStateChange(null) },
            title = { Text("Параметры аттенюатора") },
            text = {
                Column {
                    Text("Ослабление, дБ (${MIN_ATTENUATOR_LOSS.toInt()} … ${MAX_ATTENUATOR_LOSS.toInt()})")

                    TextField(
                        value = lossInput,
                        onValueChange = { input ->
                            val sanitized = input.text.replace(",", ".").replace(" ", "")
                            if (sanitized.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                when {
                                    sanitized.isEmpty() || sanitized == "-" || sanitized == "." || sanitized == "-." -> {
                                        onLossInputChange(
                                            TextFieldValue(
                                                text = sanitized,
                                                selection = input.selection
                                            )
                                        )
                                    }

                                    else -> {
                                        sanitized.toDoubleOrNull()?.let { value ->
                                            if (value in MIN_ATTENUATOR_LOSS..MAX_ATTENUATOR_LOSS) {
                                                onLossInputChange(
                                                    TextFieldValue(
                                                        text = sanitized,
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
                        keyboardActions = KeyboardActions(onDone = {
                            val newElements = elements.copy()
                            val (row, col) = attenuatorDialogState
                            val element = newElements[row, col]
                            if (element is Attenuator) {
                                newElements[row, col] = element.copy(
                                    signalPower = lossInput.text.toDoubleOrNull()
                                        ?: element.signalPower
                                )
                                onElementsChange(newElements)
                            }
                            onDialogStateChange(null)
                        }),
                        placeholder = { Text("$MIN_ATTENUATOR_LOSS … $MAX_ATTENUATOR_LOSS") },
                        singleLine = true,
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            },
            confirmButton = {
                Button(onClick = {
                    val newElements = elements.copy()
                    val (row, col) = attenuatorDialogState
                    val element = newElements[row, col]
                    if (element is Attenuator) {
                        newElements[row, col] = element.copy(
                            signalPower = lossInput.text.toDoubleOrNull() ?: element.signalPower
                        )
                        onElementsChange(newElements)
                    }
                    onDialogStateChange(null)
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { onDialogStateChange(null) }) { Text("Отмена") }
            }
        )
    }
} 