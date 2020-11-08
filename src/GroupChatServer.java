import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

//Group chat server
public class GroupChatServer {
//    private final int Port = 9999;
    //Properties used
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel socketChannel;

    //The constructor completes the server connection operation
    public GroupChatServer(int port) throws Exception {
        //Open selector and serverSocketChannel
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        //Set non-blocking
        serverSocketChannel.configureBlocking(false);
        //Listening port
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        //registered
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


    }


    //Method of reading message

    public static void main(String[] args) throws Exception {
        if (args.length<1) {
            System.out.println("Syntax: java ChatServer port");
            return;
        }
        int port = Integer.parseInt(args[0]);
        GroupChatServer server = new GroupChatServer(port);
        server.Listen();
    }

    //Set up a monitoring method to monitor events
    public void Listen() throws Exception {
        while (true) {
            int cout=selector.select();
            //selector monitoring
            if (cout > 0) {
                //Get the collection iterator
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    //Get the SelectionKey
                    SelectionKey key = keyIterator.next();
                    //Judgment event
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        //Set non-blocking
                        socketChannel.configureBlocking(false);
                        //Register and listen to read events
                        socketChannel.register(selector, SelectionKey.OP_READ);
//                        System.out.println("Client:" + socketChannel.getRemoteAddress() + "Online");
                        System.out.println("New user connected");
                    }

                    if (key.isReadable()) {
                        send(key);
                    }
                    //Remove the processed SelectionKey
                    keyIterator.remove();
                }

            }
            //The connection is abnormal, the client should be offline
        }

    }

    //Send a message
    public void send(SelectionKey key) {
        //Get Channel
        SocketChannel channel = null;
        //Read
        try {
            channel = (SocketChannel) key.channel();
            //Create buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);
            //Read the information
            if (count > 0) {
                String msg = new String(buffer.array());
                System.out.println(getMessageDescription() + msg);
//                System.out.println("Message sent by the client:" + msg);
                // forward the message
                Forward(msg, channel);
            }
        } catch (IOException e) {
            try {
                System.out.println(channel.getRemoteAddress() + "Offline...");
                //Cancel registration
                key.cancel();
                //Close the channel
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    //Method of forwarding message
    //Need to know the message and its own channel (forwarding channels other than itself)
    public void Forward(String msg, SocketChannel self) throws IOException {
//        System.out.println("Message forwarding...");
        //Get all channels registered on the selector
        for (SelectionKey key : selector.keys()) {
            //Get the channel
            Channel channel = key.channel();
            //Exclude yourself and non-SocketChannel
            if (channel instanceof SocketChannel && channel != self) {
                SocketChannel sc = (SocketChannel) channel;
                //Create buffer
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                //Write
                sc.write(buffer);

            }
        }

    }

    private String getMessageDescription() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<"+dateTimeFormatter.format(now)+">";
    }
}

