package com.vegatel.scheme.extensions

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.schemePanModifier(offsetState: MutableState<Offset>): Modifier =
    this.pointerInput(offsetState) {
        detectTransformGestures { _, pan, _, _ ->
            offsetState.value = offsetState.value + pan
        }
    } 