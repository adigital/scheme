package com.vegatel.scheme

import com.vegatel.scheme.model.ElementMatrix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

expect fun openElementMatrixFromDialog(elements: MutableStateFlow<ElementMatrix>)

expect fun saveElementMatrixFromDialog(elements: StateFlow<ElementMatrix>)