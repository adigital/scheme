package com.vegatel.scheme.model

// На Android сохранение реализовано через диалог (ActivityResult API) в AppAndroidExt.kt.
// Эта функция-заглушка нужна только для совместимости с expect/actual.
actual fun saveElementMatrixToFile(matrix: ElementMatrix, filename: String) {
    // No-op: direct file saving is not supported on Android in this context.
}