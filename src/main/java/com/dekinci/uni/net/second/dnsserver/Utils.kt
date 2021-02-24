package com.dekinci.uni.net.second.dnsserver

@ExperimentalUnsignedTypes
public infix fun UShort.shr(shift: Int): UShort = (this / (1 shl shift).toUShort()).toUShort()

@ExperimentalUnsignedTypes
public infix fun UShort.shl(shift: Int): UShort = (this * (1 shl shift).toUShort()).toUShort()
