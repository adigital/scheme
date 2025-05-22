package com.vegatel.scheme

import kotlinx.coroutines.flow.MutableStateFlow

expect fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>)
expect fun saveElementMatrixFromDialog(state: MutableStateFlow<SchemeState>)