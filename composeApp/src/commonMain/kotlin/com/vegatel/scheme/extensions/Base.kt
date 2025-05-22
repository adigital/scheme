package com.vegatel.scheme.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.vegatel.scheme.log

@Composable
fun Dp.toPx(): Float {
    val density = LocalDensity.current
    return with(density) { this@toPx.toPx() }
}

@Composable
expect fun displayFileName(fileName: String?): String