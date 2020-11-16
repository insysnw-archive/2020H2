import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Scanner;

public class GroupChatClient {
    private  Selector selector;
    private  SocketChannel socketChannel;

    public GroupChatClient(String ip, int port) throws Exception{
        //turn on
        selector = Selector.open();
        //connection
        socketChannel = SocketChannel.open(new InetSocketAddress(ip,port));
        //Set non-blocking
        socketChannel.configureBlocking(false);

        //Register, monitor the read method
        socketChannel.register(selector, SelectionKey.OP_READ);
    }
    //Receive information
    public  void Receive() throws IOException {

        int sel=selector.select();
        //Received event
        if (sel > 0){
            //The SelectionKey collection obtained by iteration
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()){
                //Get SelectionKey
                SelectionKey key = iterator.next();
                if (key.isReadable()){
                    //Get the channel
                    SocketChannel channel = (SocketChannel)key.channel();
                    //buffer get information
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    //Read from the channel
                    channel.read(buffer);

                    System.out.println((getMessageDescription()+new String(buffer.array())).trim());
                }
                iterator.remove();
            }
        }
        else
            System.out.println("No channel available");

    }

    public  void Send(String str) throws IOException {

//        str=clientname+" : "+str;
        ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
        //Write
        socketChannel.write(buffer);

    }

    public static void main(String[] args) throws Exception {

        if (args.length<2) return;
        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        Console console = System.console();
        String userName = console.readLine("\nEnter your name: ");

        GroupChatClient chatClient=new GroupChatClient(ip, port);

        new Thread(() -> {
            while (true){
                try {
                    chatClient.Receive();
                    Thread.currentThread().sleep(3000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //send data
        Scanner scanner=new Scanner(System.in);
        //Input line by line
        while (scanner.hasNext()){
            String msg=scanner.nextLine();
            chatClient.Send("["+userName+"]: "+msg);
        }

    }

    private String getMessageDescription() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return "<"+dateTimeFormatter.format(now)+">";
    }

}

