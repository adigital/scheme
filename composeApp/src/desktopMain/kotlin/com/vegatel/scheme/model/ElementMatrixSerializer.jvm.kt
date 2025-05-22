package com.vegatel.scheme.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun loadElementMatrixFromFile(filename: String): ElementMatrix {
    val json = File(filename).readText()
    val serializable = Json.decodeFromString<SerializableElementMatrix>(json)
    return serializable.toElementMatrix()
}

actual fun saveElementMatrixToFile(matrix: ElementMatrix, filename: String) {
    val serializable = matrix.toSerializable()
    File(filename).writeText(Json.encodeToString<SerializableElementMatrix>(serializable))
}
