package com.dekinci.uni.net.second.dnsserver

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "0.0.0.0:5553" else args[0]
    val adds = serverString.split(":")

    val datagramSocket = DatagramSocket(InetSocketAddress(InetAddress.getByName(adds[0]), adds[1].toInt()))

    println("Server started on $serverString")

    val receivePacket = DatagramPacket(ByteArray(1024), 1024)
    while (true) {
        datagramSocket.receive(receivePacket)
        println("Packet received")
        val buf = ByteBuffer.wrap(receivePacket.data).order(ByteOrder.BIG_ENDIAN)

        val data = try {
            val queryHeader = parseHeader(buf)
            val query = parseQuery(buf)
            println("Request \n$queryHeader\n$query")

            if (query.name != "example.com.")
                throw IllegalStateException("unsupported name")
            val resourceRecord = handleQuery(query)

            val responceHeader = DnsHeader.success()
            responceHeader.recursionDesired = queryHeader.recursionDesired

            println("Response \n$responceHeader\n$resourceRecord")
            encode(responceHeader, resourceRecord)
        } catch (e: Exception) {
            e.printStackTrace()
            encode(DnsHeader.error(), ResourceRecord())
        }

        datagramSocket.send(DatagramPacket(data, data.size, receivePacket.address, receivePacket.port))
    }
}

@ExperimentalUnsignedTypes
fun handleQuery(query: Query): ResourceRecord {
    return when (query.type) {
        QType.A -> ResourceRecord.aReply(query, "192.168.1.4")
        QType.AAAA -> ResourceRecord.aaaaReply(query, "::192.168.1.4")
        QType.MX -> ResourceRecord.mxReply(query, "mail.example.com")
        QType.TXT -> ResourceRecord.txtReply(query, "Hello DNS!")
        QType.NONE -> throw IllegalStateException("Zero qtype")
    }
}

@ExperimentalUnsignedTypes
fun parseHeader(buffer: ByteBuffer): DnsHeader {
    val header = DnsHeader()
    header.id = buffer.short.toUShort()
    header.flags = buffer.short.toUShort();
    header.questionEntriesCount = buffer.short.toUShort();
    header.resourceRecordsCount = buffer.short.toUShort();
    header.nameServerRRCount = buffer.short.toUShort();
    header.additionalRRCount = buffer.short.toUShort();
    return header
}

@ExperimentalUnsignedTypes
fun parseQuery(buffer: ByteBuffer): Query {
    val query = Query();

    var length: Byte
    do {
        length = buffer.get()

        for (i in 0 until length) {
            val c = buffer.get().toChar()
            query.name += c
        }
        if (length != 0.toByte()) {
            query.name += '.'
        }
    } while (length != 0.toByte());

    query.type = QType.find(buffer.getShort().toUShort())
    query.qClass = QClass.find(buffer.getShort().toUShort());
    return query;
}

@ExperimentalUnsignedTypes
fun encode(header: DnsHeader, record: ResourceRecord): ByteArray {
    val buffer = ByteBuffer.wrap(ByteArray(1000)).order(ByteOrder.BIG_ENDIAN)

    buffer.putShort(header.id.toShort())
    buffer.putShort(header.flags.toShort())
    buffer.putShort(header.questionEntriesCount.toShort())
    buffer.putShort(header.resourceRecordsCount.toShort())
    buffer.putShort(header.nameServerRRCount.toShort())
    buffer.putShort(header.additionalRRCount.toShort())
    if (header.resourceRecordsCount != 0u.toUShort()) {
        encodeResourceRecord(record, buffer)
    }

    buffer.flip()
    val data = ByteArray(buffer.remaining())
    buffer[data]

    return data
}

@ExperimentalUnsignedTypes
fun encodeResourceRecord(record: ResourceRecord, buffer: ByteBuffer) {
    var start = 0
    var end = record.name.indexOf('.', start)
    while (end != -1) {
        buffer.put((end - start).toUByte().toByte())
        for (i in start until end) {
            buffer.put(record.name[i].toByte())
        }
        start = end + 1
        end = record.name.indexOf('.', start)
    }
    buffer.put((record.name.length - start).toByte())
    for (i in start until record.name.length) {
        buffer.put(record.name[i].toByte())
    }

    buffer.putShort(record.type.v.toShort())
    buffer.putShort(record.qClass.v.toShort())
    buffer.putInt(record.ttl)

    val dataLength = record.recordData.size;
    buffer.putShort(dataLength.toUShort().toShort())
    for (data in record.recordData) {
        buffer.put(data.toByte())
    }
}
