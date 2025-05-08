package com.vegatel.scheme

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun log(tag: String, message: String)