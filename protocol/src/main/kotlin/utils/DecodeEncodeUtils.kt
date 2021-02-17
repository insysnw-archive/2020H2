package utils

fun Byte.toIntBit() = toInt() and 0xFF

fun decodeInt(i1: Int, i2: Int, i3: Int, i4: Int): Int = (i1 shl 24) + (i2 shl 16) + (i3 shl 8) + (i4 shl 0)

fun encodeInt(i: Int) = byteArrayOf(
        (i ushr 24 and 0xFF).toByte(),
        (i ushr 16 and 0xFF).toByte(),
        (i ushr 8 and 0xFF).toByte(),
        (i ushr 0 and 0xFF).toByte()
)
