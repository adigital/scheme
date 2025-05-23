package com.vegatel.scheme

import android.util.Log

actual fun getPlatform(): String = "Android"

actual fun log(tag: String, message: String) {
    Log.d(tag, message)
}