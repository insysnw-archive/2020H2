package lab1.b;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lab1.a.Server;
import lab1.model.Protocol;
import lab1.model.ProtocolHelper;

/**
 * @author Crunchify.com
 */

public class NIOServer {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private String hostname;
    private int port;


    private static Map<SelectionKey, String> map = new ConcurrentHashMap<>();

    public NIOServer() {
        this(3345);
    }

    public NIOServer(int port) {
        this("localhost", port);
    }

    public NIOServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(this.hostname, this.port);
        serverSocketChannel.bind(inetSocketAddress);
        serverSocketChannel.configureBlocking(false);

        int ops = serverSocketChannel.validOps();
        SelectionKey selectKy = serverSocketChannel.register(selector, ops, null);

        while (this.serverSocketChannel.isOpen()) {
            selector.select();
            Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();

            while (selectionKeyIterator.hasNext()) {
                SelectionKey myKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();

                if (myKey.isAcceptable()) {
                    this.handleAccept(myKey);
                } else if (myKey.isReadable()) {
                    this.handleRead(myKey);
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        String address = socketChannel.socket().getInetAddress().toString() + ":" + socketChannel.socket().getPort();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ + SelectionKey.OP_WRITE, address);
        log("Connection Accepted: " + socketChannel.getLocalAddress());
    }

    private void handleRead(SelectionKey currKey) {
        SocketChannel socketChannel = (SocketChannel) currKey.channel();

        List<Protocol> messages = ProtocolHelper.read(socketChannel);
        String username = messages.get(0).getName();
        if (!map.containsKey(currKey)) {
            map.put(currKey, username);
        }
        String message = ProtocolHelper.toLine(messages);
        log("[" + messages.get(0).getName() + "] " + message);

        if ("quit".equals(message)) {
            System.out.println("Client initialize connections suicide ...");
            try {
                currKey.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel && key != currKey) {
                SocketChannel socketChannel1 = (SocketChannel) key.channel();
                ProtocolHelper.write(ProtocolHelper.build(map.get(currKey), message), socketChannel1);
            }
        }

        selector.selectedKeys();
    }


    @SuppressWarnings("unused")
    public static void main(String[] args) throws IOException {
        NIOServer server = createServer(args);
        server.start();
    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static NIOServer createServer(String[] args) {
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iterator = argsList.listIterator();
        int port = 3345;
        String hostname = "localhost";

        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-p":
                    port = Integer.parseInt(iterator.next());
                    break;
                case "-h":
                    hostname = iterator.next();
                    break;
            }
        }
        return new NIOServer(hostname, port);
    }
}