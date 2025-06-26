package com.vegatel.scheme.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.schemePanModifier(offsetState: MutableState<Offset>): Modifier =
    this.onDrag(
        matcher = PointerMatcher.mouse(PointerButton.Primary)
    ) { delta ->
        offsetState.value = offsetState.value + delta
    } 