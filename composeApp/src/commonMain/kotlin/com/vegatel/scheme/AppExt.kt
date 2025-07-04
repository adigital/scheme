package com.vegatel.scheme

import kotlinx.coroutines.flow.MutableStateFlow

expect fun openElementMatrixFromDialog(state: MutableStateFlow<SchemeState>)
expect fun saveElementMatrixFromDialog(state: MutableStateFlow<SchemeState>)
expect fun saveSchemeToFile(state: MutableStateFlow<SchemeState>)
expect fun openBackgroundFromDialog(state: MutableStateFlow<SchemeState>)
expect fun exportSchemeToPdfFromDialog(state: MutableStateFlow<SchemeState>)