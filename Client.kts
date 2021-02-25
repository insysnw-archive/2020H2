import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

    val diff = synchronize("pool.ntp.org")
    println(Date(System.currentTimeMillis() + diff))

    @Throws(IOException::class)
    fun synchronize(server: String?): Long {
        val socket = DatagramSocket()
        socket.soTimeout = 2000

        val ntpPacket = Packet()
        val t0 = System.currentTimeMillis()
        ntpPacket.transmitTimestamp = Packet.Timestamp(t0)

        val ntpRawPacket: ByteArray = ntpPacket.asByteArray()
        val packet = DatagramPacket(
            ntpRawPacket,
            ntpRawPacket.size, InetAddress.getByName(server), 123
        )

        socket.send(packet)
        socket.receive(packet)

        val t3 = System.currentTimeMillis()
        val recvPacket = Packet(ByteBuffer.wrap(packet.data))
        val t1: Long = recvPacket.receiveTimestamp.timeMillis
        val t2: Long = recvPacket.transmitTimestamp.timeMillis
        socket.close()
        return (t1 + t2 - t0 - t3 + 1) / 2
    }

    class Packet {
        class Timestamp {
            val seconds: Long
            val fraction: Long

            constructor(time: Long) {
                seconds = time / 1000 + 2208988800L
                val milli = time % 1000
                fraction = (milli * 0x100000000L + 500) / 1000
            }

            constructor(seconds: Long, fraction: Long) {
                require(Integer.toUnsignedLong(seconds.toInt()) == seconds) { "Seconds outside valid range" }
                require(Integer.toUnsignedLong(fraction.toInt()) == fraction) { "Fraction outside valid range" }
                this.seconds = seconds
                this.fraction = fraction
            }

            val timeMillis: Long
                get() {
                    val time = (seconds - 2208988800L) * 1000
                    val milli = (fraction * 1000 + 0x80000000L) / 0x100000000L
                    return time + milli
                }
        }

        private var buffer: ByteBuffer

        constructor() {
            buffer = ByteBuffer.allocate(48)
            setLeapIndicator(3)
            setVersion(4)
            setMode(3)
        }

        constructor(buffer: ByteBuffer) {
            require(buffer.limit() >= 48) { "Buffer size is less than 48" }
            this.buffer = buffer
        }

        fun setLeapIndicator(leapIndicator: Int) {
            require(!(leapIndicator < 0 || leapIndicator >= 4)) { "Leap Indicator outside valid range" }
            var b = buffer[0]
            b = b and 0b11000000.toByte().inv()
            b = b or ((leapIndicator shl 6).toByte())
            buffer.put(0, b)
        }

        fun setVersion(version: Int) {
            require(!(version < 0 || version >= 8)) { "Version outside valid range" }
            var b = buffer[0]
            b = b and 56.inv()
            b = b or ((version shl 3).toByte())
            buffer.put(0, b)
        }

        fun setMode(mode: Int) {
            require(!(mode < 0 || mode >= 8)) { "Mode outside valid range" }
            var b = buffer[0]
            b = b and 7.inv()
            b = b or mode.toByte()
            buffer.put(0, b)
        }

        val receiveTimestamp: Timestamp
            get() {
                val seconds = Integer.toUnsignedLong(buffer.getInt(32))
                val fraction = Integer.toUnsignedLong(buffer.getInt(36))
                return Timestamp(seconds, fraction)
            }
        var transmitTimestamp: Timestamp
            get() {
                val seconds = Integer.toUnsignedLong(buffer.getInt(40))
                val fraction = Integer.toUnsignedLong(buffer.getInt(44))
                return Timestamp(seconds, fraction)
            }
            set(t) {
                buffer.putInt(40, t.seconds.toInt())
                buffer.putInt(44, t.fraction.toInt())
            }

        fun asByteArray(): ByteArray {
            return buffer.array()
        }
    }