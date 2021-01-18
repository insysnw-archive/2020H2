package com.dekinci.uni.net.first

fun definedHugeData(): Pair<ByteArray, Map<String, String>> {
    val pair1 = "key1" to "value1".repeat(MessageEncoder.chunkSize / 4)
    val pair2 = "key2" to "value2".repeat(MessageEncoder.chunkSize / 4)

    val key1 = pair1.first.toByteArray(Charsets.UTF_8)
    val value1 = pair1.second.toByteArray(Charsets.UTF_8)
    val key2 = pair2.first.toByteArray(Charsets.UTF_8)
    val value2 = pair2.second.toByteArray(Charsets.UTF_8)
    val data = byteArrayOf(MessageEncoder.messageType.toByte()) +
            encodeInt(2) +
            encodeInt(key1.size) + key1 +
            encodeInt(value1.size) + value1 +
            encodeInt(key2.size) + key2 +
            encodeInt(value2.size) + value2

    return data to mapOf(pair1, pair2)
}

fun definedSmallData(): Pair<ByteArray, Map<String, String>> {
    val pair1 = "key1" to "value1"
    val pair2 = "key2" to "value2"

    val key1 = pair1.first.toByteArray(Charsets.UTF_8)
    val value1 = pair1.second.toByteArray(Charsets.UTF_8)
    val key2 = pair2.first.toByteArray(Charsets.UTF_8)
    val value2 = pair2.second.toByteArray(Charsets.UTF_8)
    val data = byteArrayOf(MessageEncoder.messageType.toByte()) +
            encodeInt(2) +
            encodeInt(key1.size) + key1 +
            encodeInt(value1.size) + value1 +
            encodeInt(key2.size) + key2 +
            encodeInt(value2.size) + value2

    return data to mapOf(pair1, pair2)
}

fun encodedStream(map: Map<String, String>) = MessageEncoder.encode(map).flatMap { it.asList() }.toByteArray()
