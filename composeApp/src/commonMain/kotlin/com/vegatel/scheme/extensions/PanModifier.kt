package com.vegatel.scheme.extensions

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Добавляет к [Modifier] поддержку перетаскивания всей схемы.
 *
 * Desktop: перетаскивание осуществляется правой кнопкой мыши (Secondary button).
 * Android: перетаскивание осуществляется жестом двумя пальцами.
 */
expect fun Modifier.schemePanModifier(offsetState: MutableState<Offset>): Modifier 