package com.vegatel.scheme.model

import com.vegatel.scheme.AppConfig
import kotlinx.serialization.Serializable

@Serializable
data class SerializableOffset(val x: Float, val y: Float)

@Serializable
data class SerializableElementOffset(val id: Int, val offset: SerializableOffset)

@Serializable
data class SerializableScheme(
    val matrix: SerializableElementMatrix,
    val schemeOffset: SerializableOffset = SerializableOffset(0f, 0f),
    val elementOffsets: List<SerializableElementOffset> = emptyList(),
    val schemeScale: Float = 1f,
    val backgroundFileName: String? = null,
    val backgroundScale: Float = 1f,
    val baseStationSignal: Double = AppConfig.DEFAULT_BASE_STATION_SIGNAL_DBM
) 