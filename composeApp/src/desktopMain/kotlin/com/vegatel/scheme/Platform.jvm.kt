package com.vegatel.scheme

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun log(tag: String, message: String) {
    println("[$tag] $message")
}