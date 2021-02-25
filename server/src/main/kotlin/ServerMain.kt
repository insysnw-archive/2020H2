import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val serverString = if (args.isEmpty()) "localhost:8888" else args[0]
    val adds = serverString.split(":")
    val server = Server(InetSocketAddress(InetAddress.getByName(adds[0]), adds[1].toInt()))
    while (true) {
        server.handleConnection()
    }
}
