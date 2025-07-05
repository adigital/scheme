package com.vegatel.scheme

import com.russhwolf.settings.Settings

/**
 * Глобальные настройки приложения, хранящиеся локально на устройстве.
 */
object AppSettings {
    private const val KEY_CONSIDER_GAIN = "consider_antenna_gain"

    // Settings() доступен благодаря зависимости multiplatform-settings-no-arg.
    private val settings by lazy { Settings() }

    /**
     * Учитывать ли усиление антенн при расчёте уровня сигнала.
     * true  – усиление учитывается (текущее поведение);
     * false – усиление игнорируется.
     */
    var considerAntennaGain: Boolean
        get() = settings.getBoolean(KEY_CONSIDER_GAIN, true)
        set(value) {
            settings.putBoolean(KEY_CONSIDER_GAIN, value)
        }
} 