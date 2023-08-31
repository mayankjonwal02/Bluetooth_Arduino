package com.example.bluetooth_arduino

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform