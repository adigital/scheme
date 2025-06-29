package com.vegatel.scheme.model

import kotlinx.serialization.Serializable

@Serializable
data class SerializableOffset(val x: Float, val y: Float)

@Serializable
data class SerializableElementOffset(val id: Int, val offset: SerializableOffset)

@Serializable
data class SerializableScheme(
    val matrix: SerializableElementMatrix,
    val schemeOffset: SerializableOffset = SerializableOffset(0f, 0f),
    val elementOffsets: List<SerializableElementOffset> = emptyList()
) 