package com.dekinci.uni.net.first

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DecodeEncodeUtilsKtTest {

    @Test
    fun decodeEncodeInt() {
        val a = 123412214
        val encoded = encodeInt(a)
        assertEquals(a, decodeInt(encoded[0].toIntBit(), encoded[1].toIntBit(), encoded[2].toIntBit(), encoded[3].toIntBit()))
    }

    @Test
    fun decodeEncodeZeroInt() {
        val a = 0
        val encoded = encodeInt(a)
        assertEquals(a, decodeInt(encoded[0].toIntBit(), encoded[1].toIntBit(), encoded[2].toIntBit(), encoded[3].toIntBit()))
    }

    @Test
    fun decodeEncodeSmallInt() {
        val a = 12
        val encoded = encodeInt(a)
        assertEquals(a, decodeInt(encoded[0].toIntBit(), encoded[1].toIntBit(), encoded[2].toIntBit(), encoded[3].toIntBit()))
    }
}