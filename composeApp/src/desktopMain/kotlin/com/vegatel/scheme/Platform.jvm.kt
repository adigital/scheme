package com.vegatel.scheme

actual fun getPlatform(): String = "Desktop"

actual fun log(tag: String, message: String) {
    println("[$tag] $message")
}