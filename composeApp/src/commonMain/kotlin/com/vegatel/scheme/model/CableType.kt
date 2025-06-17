package com.vegatel.scheme.model

/**
 * Типы кабеля с отображаемым именем и картой затуханий (дБ/м) в зависимости от частоты (МГц). Значения затухания отрицательны для уменьшения мощности сигнала.
 */
enum class CableType(val displayName: String, val attenuationMap: Map<Int, Double>) {
    CF_HALF(
        "CF ½", mapOf(
            800 to -6.5,
            900 to -7.0,
            1800 to -10.0,
            2100 to -11.5,
            2600 to -12.2
        )
    ),
    TEN_D_FB(
        "10D-FB", mapOf(
            800 to -10.0,
            900 to -11.0,
            1800 to -15.6,
            2100 to -17.3,
            2600 to -19.7
        )
    ),
    EIGHT_D_FB(
        "8D-FB", mapOf(
            800 to -13.5,
            900 to -14.5,
            1800 to -21.8,
            2100 to -24.0,
            2600 to -27.0
        )
    ),
    FIVE_D_FB(
        "5D-FB", mapOf(
            800 to -19.0,
            900 to -21.0,
            1800 to -30.0,
            2100 to -32.5,
            2600 to -36.5
        )
    ),
    OPTICAL(
        "Оптический кабель", mapOf(
            800 to 0.0,
            900 to 0.0,
            1800 to 0.0,
            2100 to 0.0,
            2600 to 0.0
        )
    );
} 