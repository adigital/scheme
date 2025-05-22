package com.vegatel.scheme

import com.vegatel.scheme.model.ElementMatrix
import kotlinx.coroutines.flow.MutableStateFlow

expect fun openElementMatrixFromDialog(elements: MutableStateFlow<ElementMatrix>)