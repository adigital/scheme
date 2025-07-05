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
import com.vegatel.scheme.model.Element.Booster
import com.vegatel.scheme.model.ElementMatrix

private const val MIN_BOOSTER_GAIN = 0.0
private const val DEFAULT_MAX_BOOSTER_GAIN = 100.0

@Composable
fun BoosterGainDialog(
    elements: ElementMatrix,
    onElementsChange: (ElementMatrix) -> Unit,
    boosterGainDialogState: Pair<Int, Int>?,
    onBoosterGainDialogStateChange: (Pair<Int, Int>?) -> Unit,
    boosterGainInput: TextFieldValue,
    onBoosterGainInputChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester
) {
    if (boosterGainDialogState != null) {
        val (dialogRow, dialogCol) = boosterGainDialogState
        val currentElement = elements[dialogRow, dialogCol] as? Booster
        val maxGain = currentElement?.maxGain ?: DEFAULT_MAX_BOOSTER_GAIN

        AlertDialog(
            onDismissRequest = { onBoosterGainDialogStateChange(null) },
            title = { Text("Параметры бустера") },
            text = {
                Column {
                    Text("Усиление, дБ (${MIN_BOOSTER_GAIN.toInt()} - ${maxGain.toInt()})")

                    TextField(
                        value = boosterGainInput,
                        onValueChange = { input ->
                            val withDot = input.text.replace(",", ".").replace(" ", "")
                            if (withDot.matches(Regex("^\\d*\\.?\\d*$"))) {
                                when {
                                    withDot.isEmpty() || withDot == "." -> {
                                        onBoosterGainInputChange(
                                            TextFieldValue(
                                                text = withDot,
                                                selection = input.selection
                                            )
                                        )
                                    }

                                    else -> {
                                        withDot.toDoubleOrNull()?.let { value ->
                                            if (value in MIN_BOOSTER_GAIN..maxGain) {
                                                onBoosterGainInputChange(
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
                        keyboardActions = KeyboardActions(onDone = {
                            val newElements = elements.copy()
                            val (row, col) = boosterGainDialogState
                            val element = newElements[row, col]
                            if (element is Booster) {
                                newElements[row, col] = element.copy(
                                    signalPower = boosterGainInput.text.toDoubleOrNull()
                                        ?: element.signalPower
                                )
                                onElementsChange(newElements)
                            }
                            onBoosterGainDialogStateChange(null)
                        }),
                        placeholder = { Text("$MIN_BOOSTER_GAIN … $maxGain") },
                        singleLine = true,
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newElements = elements.copy()
                    val (row, col) = boosterGainDialogState
                    val element = newElements[row, col]
                    if (element is Booster) {
                        newElements[row, col] = element.copy(
                            signalPower = boosterGainInput.text.toDoubleOrNull()
                                ?: element.signalPower
                        )
                        onElementsChange(newElements)
                    }
                    onBoosterGainDialogStateChange(null)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { onBoosterGainDialogStateChange(null) }) {
                    Text("Отмена")
                }
            }
        )
    }
} 