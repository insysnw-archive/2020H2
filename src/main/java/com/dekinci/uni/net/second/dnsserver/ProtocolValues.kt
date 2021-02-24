package com.dekinci.uni.net.second.dnsserver

@ExperimentalUnsignedTypes
enum class MessageType(val v: UByte) {
    QUERY(0u),
    RESPONSE(1u);

    companion object {
        private val map = values().associateBy { it.v }
        fun find(type: UByte) = map[type]!!
    }
}

@ExperimentalUnsignedTypes
enum class Opcode(val v: UByte) {
    QUERY(0u),
    IQUERY(1u),
    STATUS(2u);

    companion object {
        private val map = values().associateBy { it.v }
        fun find(type: UByte) = map[type]!!
    }
}

@ExperimentalUnsignedTypes
enum class Rcode(val v: UByte) {
    NO_ERROR(0u),
    FORMAT_ERROR(1u),
    SERVER_FAILURE(2u),
    NAME_ERROR(3u),
    NOT_IMPLEMENTED(4u),
    REFUSED(5u);

    companion object {
        private val map = values().associateBy { it.v }
        fun find(type: UByte) = map[type]!!
    }
}

@ExperimentalUnsignedTypes
enum class QType(val v: UShort) {
    NONE(0u),
    A(1u),
    MX(15u),
    TXT(16u),
    AAAA(28u);

    companion object {
        private val map = values().drop(1).associateBy { it.v }
        fun find(type: UShort) = map[type]!!
    }
}

@ExperimentalUnsignedTypes
enum class QClass(val v: UShort) {
    NONE(0u),
    IN(1u),
    CS(2u),
    CH(3u),
    HS(4u);

    companion object {
        private val map = values().drop(1).associateBy { it.v }
        fun find(type: UShort) = map[type]!!
    }
}