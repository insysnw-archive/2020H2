package com.dekinci.uni.net.second.dnsserver

import java.net.InetAddress

@ExperimentalUnsignedTypes
data class DnsHeader(
        var id: UShort = 0u,
        var flags: UShort = 0u,
        var questionEntriesCount: UShort = 0u,
        var resourceRecordsCount: UShort = 0u,
        var nameServerRRCount: UShort = 0u,
        var additionalRRCount: UShort = 0u
) {

    var messageType: MessageType
        set(value) {
            flags = if (value == MessageType.RESPONSE)
                flags or messageTypeMask
            else
                flags and messageTypeMask.inv()
        }
        get() = MessageType.find(((flags and messageTypeMask) shr 15).toUByte())

    var opcode: Opcode
        set(value) {
            flags = flags and opcodeMask.inv();
            flags = flags or (value.v.toUShort() shl 11)
        }
        get() = Opcode.find(((flags and opcodeMask) shr 11).toUByte())

    var responseCode: Rcode
        set(value) {
            flags = flags and responseCodeMask.inv();
            flags = flags or value.v.toUShort();
        }
        get() = Rcode.find((flags and responseCodeMask).toUByte())

    var authoritativeAnswer: Boolean
        set(value) {
            flags = (if (value)
                flags or authoritativeMask
            else
                flags and authoritativeMask.inv())
        }
        get() = flags and authoritativeMask != zero2

    var truncated: Boolean
        set(value) {
            flags = (if (value)
                flags or truncatedMask
            else
                flags and truncatedMask.inv())
        }
        get() = flags and truncatedMask != zero2

    var recursionDesired: Boolean
        set(value) {
            flags = (if (value)
                flags or recursionDesiredMask
            else
                flags and recursionDesiredMask.inv())
        }
        get() = flags and recursionDesiredMask != zero2

    var recursionAvailable: Boolean
        set(value) {
            flags = if (value)
                flags or recursionAvailableMask
            else
                flags and recursionAvailableMask.inv()
        }
        get() = flags and recursionAvailableMask != zero2

    override fun toString(): String {
        return "DnsHeader{ id: ${id.toString(16)}, " +
                "flags: [ $messageType, $responseCode, $opcode, " +
                "recAvailable: $recursionAvailable, recDesired: $recursionDesired, truncated: $truncated, auth: $authoritativeAnswer ], " +
                "questions: $questionEntriesCount, records: $resourceRecordsCount, nameServers: $nameServerRRCount, additional: $additionalRRCount }"
    }

    companion object {
        private const val zero2: UShort = 0u

        private const val messageTypeMask: UShort = 0x8000u
        private const val opcodeMask: UShort = 0x7800u
        private const val authoritativeMask: UShort = 0x0400u
        private const val truncatedMask: UShort = 0x0200u
        private const val recursionDesiredMask: UShort = 0x0100u
        private const val recursionAvailableMask: UShort = 0x0080u
        private const val responseCodeMask: UShort = 0x000Fu

        const val size = 12

        fun success(): DnsHeader {
            val header = DnsHeader()
            header.messageType = MessageType.RESPONSE
            header.opcode = Opcode.QUERY
            header.authoritativeAnswer = false
            header.truncated = false
            header.recursionDesired = false
            header.recursionAvailable = false
            header.questionEntriesCount = 0u;
            header.resourceRecordsCount = 1u;
            header.nameServerRRCount = 0u;
            header.additionalRRCount = 0u;
            header.responseCode = Rcode.NO_ERROR
            return header
        }

        fun error(): DnsHeader {
            val header = DnsHeader()
            header.messageType = MessageType.RESPONSE
            header.opcode = Opcode.QUERY
            header.authoritativeAnswer = false
            header.truncated = false
            header.recursionDesired = false
            header.recursionAvailable = false
            header.responseCode = Rcode.NOT_IMPLEMENTED
            header.questionEntriesCount = 0u
            header.resourceRecordsCount = 0u
            header.nameServerRRCount = 0u
            header.additionalRRCount = 0u
            return header
        }
    }
}

@ExperimentalUnsignedTypes
data class Query(var name: String = "", var type: QType = QType.NONE, var qClass: QClass = QClass.NONE) {
    override fun toString(): String {
        return "Query {name: $name, type: $type, qClass: $qClass}"
    }
}

@ExperimentalUnsignedTypes
data class ResourceRecord(
        val name: String = "",
        val type: QType = QType.NONE,
        val qClass: QClass = QClass.NONE,
        val ttl: Int = 0,
        val recordData: List<UByte> = listOf()
) {
    companion object {
        fun aReply(query: Query, ip: String): ResourceRecord {
            return ResourceRecord(query.name, query.type, query.qClass, 0, InetAddress.getByName(ip).address.toUByteArray().toList())
        }

        fun aaaaReply(query: Query, ip: String): ResourceRecord {
            return ResourceRecord(query.name, query.type, query.qClass, 0, InetAddress.getByName(ip).address.toUByteArray().toList())
        }

        fun mxReply(query: Query, domain: String): ResourceRecord {
            val split = domain.split(".")
            val data = listOf<UByte>(0u, 1u) + split.flatMap { encodeString(it) } + listOf(0u)
            return ResourceRecord(query.name, query.type, query.qClass, 0, data)
        }

        fun txtReply(query: Query, text: String): ResourceRecord {
            return ResourceRecord(query.name, query.type, query.qClass, 0, encodeString(text))
        }

        private fun encodeString(str: String): List<UByte> {
            if (str.length > 255)
                throw IllegalArgumentException("Str is too long")
            return listOf(str.length.toUByte()) + str.toByteArray().toUByteArray().toList()
        }
    }
}


