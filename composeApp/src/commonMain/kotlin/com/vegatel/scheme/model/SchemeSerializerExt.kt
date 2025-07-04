package com.vegatel.scheme.model

import com.vegatel.scheme.SchemeState

/**
 * Преобразует текущее состояние схемы (элементы и смещения) в сериализуемую схему.
 */
fun SchemeState.toSerializableScheme(): SerializableScheme = SerializableScheme(
    matrix = elements.toSerializable(),
    schemeOffset = SerializableOffset(schemeOffset.x, schemeOffset.y),
    elementOffsets = elementOffsets.map { (id, offset) ->
        SerializableElementOffset(id, SerializableOffset(offset.x, offset.y))
    },
    schemeScale = schemeScale,
    backgroundFileName = backgroundFileName,
    backgroundScale = backgroundScale,
    baseStationSignal = baseStationSignal
) 