package com.dekinci.uni.net.second.dnsserver

import kotlin.experimental.and

@ExperimentalUnsignedTypes
infix fun UShort.shr(shift: Int): UShort = (this / (1 shl shift).toUShort()).toUShort()

@ExperimentalUnsignedTypes
infix fun UShort.shl(shift: Int): UShort = (this * (1 shl shift).toUShort()).toUShort()

infix fun Short.shr(shift: Int): Short = (this / (1 shl shift)).toShort()

fun Short.toByteArray() = byteArrayOf((this shr 8).toByte(), (this and 0xFF).toByte())

fun ByteArray.toShort() = (this[0].toInt() and 0xFF shl 8 or (this[1].toInt() and 0xFF)).toShort()
