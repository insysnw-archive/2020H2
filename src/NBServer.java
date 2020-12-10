import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
public class NBServer {

    public static void main(String[] args)
            throws Exception {
        B b = new B();
        InetAddress host = InetAddress.getByName("localhost");
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel =
                ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(host, 8000));
        serverSocketChannel.register(selector, SelectionKey.
                OP_ACCEPT);
        SelectionKey key = null;
        ArrayList<SocketChannel> arr = new ArrayList<>();
        while (true) {
            if (selector.select() <= 0)
                continue;
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.
                            OP_READ);
                    System.out.println("Connection Accepted: "
                            + sc.getLocalAddress() + "\n");
                    arr.add(sc);
                }
                if (key.isReadable()) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer bb = ByteBuffer.allocate(1024);
                    sc.read(bb);
                    ArrayList<byte[]> parsedValues = inputParser(bb.array());
                    String result = new String(bb.array()).trim();
                    System.out.println("Server: <" + b.bToS(parsedValues.get(1)) + "> [" + b.bToS(parsedValues.get(3)) + "] " + b.bToS(parsedValues.get(5)) + "\n");
                    byte[] glue = outputGlue(parsedValues);
                    ByteBuffer b1 = ByteBuffer.wrap(glue);
                    //////////////////////////////////////////////////////////
                    for(SocketChannel c : arr){
                        c.configureBlocking(false);
                        c.write(b1);
                        b1.flip();
                    }
                    //////////////////////////////////////////////////////////
                    if (result.length() <= 0) {
                        sc.close();
                        System.out.println("Connection closed...");
                        System.out.println(
                                "Server will keep running. " +
                                        "Try running another client to " +
                                        "re-establish connection");
                    }
                }


            }
        }
    }

    private static ArrayList<byte[]> inputParser(byte[] input) {
        ArrayList<byte[]> result = new ArrayList<>();
        int prePoint = 0;
        int postPoint = 4;
        B b = new B();
        String time = getTime();
        byte[] bTime = b.sToB(time);
        int timeLength = bTime.length;
        byte[] bTimeLength = b.iToB(timeLength);
        result.add(bTimeLength);
        result.add(bTime);

        byte[] bNameLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bNameLength);
        int bNL = bNameLength.length;
        int nameLength = b.bToI(bNameLength);
        prePoint = bNL;
        postPoint += nameLength;
        byte[] bName = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bName);

        prePoint = postPoint;
        postPoint += 4;
        byte[] bMessLength = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMessLength);
        int messLength = b.bToI(bMessLength);
        prePoint = postPoint;
        postPoint += messLength;
        byte[] bMess = Arrays.copyOfRange(input, prePoint, postPoint);
        result.add(bMess);

        return result;
    }

    private static String getTime() {
        Date time = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(time);
    }

    private static byte[] outputGlue(ArrayList<byte[]> output) {
        B b = new B();
        byte[] temp1 = b.sum(output.get(0), output.get(1));
        byte[] temp2 = b.sum(temp1, output.get(2));
        byte[] temp3 = b.sum(temp2, output.get(3));
        byte[] temp4 = b.sum(temp3, output.get(4));
        return b.sum(temp4, output.get(5));
    }
}

class B {

    public byte[] sToB(String str) {
        return str.getBytes(StandardCharsets.UTF_16);
    }

    public String bToS(byte[] b) {
        return new String(b, StandardCharsets.UTF_16);
    }

    public byte[] sum(byte[] a, byte[] b) {
        byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public int bToI(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public byte[] iToB(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public String arrToStr(byte[] b) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            res.append(b[i]);
            if (i != b.length - 1) {
                res.append(", ");
            }
        }
        return res.toString();
    }

}