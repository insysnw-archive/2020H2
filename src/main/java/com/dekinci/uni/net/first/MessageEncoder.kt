package com.dekinci.uni.net.first

import java.io.ByteArrayOutputStream

object MessageEncoder {
    const val chunkSize = 400

    const val ackType = 1
    const val messageType = 2
    const val pingType = 3

    fun encode(map: Map<String, String>): List<ByteArray> {
        val list = ArrayList<ByteArray>()

        val outputStream = ByteArrayOutputStream()
        outputStream.write(messageType)
        outputStream.write(encodeInt(map.size))
        list.add(outputStream.toByteArray())
        var append = true
        for (entry in map) {
            val keyList = writePayload(entry.key)
            joinLists(list, keyList, append)
            append = keyList.size == 1

            val payloadList = writePayload(entry.value)
            joinLists(list, payloadList, append)
            append = payloadList.size == 1
        }

        return list
    }

    private fun joinLists(accumulator: MutableList<ByteArray>, source: List<ByteArray>, append: Boolean) {
        if (append) {
            accumulator[accumulator.lastIndex] = accumulator[accumulator.lastIndex] + source[0]
            accumulator.addAll(source.drop(1))
        } else {
            accumulator.addAll(source)
        }
    }

    private fun writePayload(s: String): List<ByteArray> {
        var outputStream = ByteArrayOutputStream()
        val list = ArrayList<ByteArray>()
        val buf = s.toByteArray(Charsets.UTF_8)
        outputStream.write(encodeInt(buf.size))
        var toWrite = buf.size

        if (toWrite < chunkSize) {
            outputStream.write(buf)
        } else {
            list.add(outputStream.toByteArray())
            list.add(byteArrayOf())
            outputStream = ByteArrayOutputStream()
            while (toWrite > 0) {
                outputStream.write(buf, buf.size - toWrite, Integer.min(chunkSize, toWrite))
                toWrite = Integer.max(toWrite - chunkSize, 0)
                list.add(outputStream.toByteArray())
                list.add(byteArrayOf())
                outputStream = ByteArrayOutputStream()
            }
        }

        if (outputStream.size() != 0)
            list.add(outputStream.toByteArray())
        return list
    }
}