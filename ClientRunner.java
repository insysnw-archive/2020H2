import java.io.IOException;
import java.net.*;
import java.util.Random;


public class ClientRunner implements Runnable {
    DatagramSocket socket;
    DatagramPacket responsePacket;
    DatagramPacket discoverPacket;
    byte[] buf;

    private final byte[] broadcast = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private final byte[] broadcast2 = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] mac = new byte[6];
    private final byte[] secs = new byte[2];
    private final int serverPort = 67;
    private final int clientPort = 68;
    private final int lifeTime = 3000;
    public long time;


    public ClientRunner() {
        new Random().nextBytes(mac);
        buf = new byte[548];
    }

    @Override
    public synchronized void run() {
        try {
            buf = new DHCPMessage().createDiscover(mac);
            time = System.currentTimeMillis();
            socket = new DatagramSocket( serverPort,Inet4Address.getByAddress(broadcast2));
            discoverPacket = new DatagramPacket(buf, buf.length, Inet4Address.getByAddress(broadcast), clientPort);
            socket.send(discoverPacket);
            System.out.println("Send Discover Packet");
            socket.close();
            boolean isPackReceived = false;
            while (!isPackReceived) {
                socket = new DatagramSocket(serverPort);
                socket.setSoTimeout(lifeTime);
                buf = new byte[250];
                responsePacket = new DatagramPacket(buf, buf.length);
                socket.receive(responsePacket);
                System.out.println("Received a response");
                switch (DhcpMessageHelper.interpret(buf)) {
                    case 2:
                        if (isOffer(buf)) {
                            printResponseInfo();
                            sendRequest(socket);
                        }
                        break;
                    case 5:
                        if (isPack(buf)) {
                            printResponseInfo();
                            isPackReceived = true;
                        }
                        break;
                    case 0:
                        System.out.println("Incorrect Response");
                        break;
                }
                socket.close();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Server is not responding");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendRequest(DatagramSocket socket) throws IOException {
        secs[0] = (byte) (((System.currentTimeMillis() - time) / 1000) / 256);
        secs[1] = (byte) (((System.currentTimeMillis() - time) / 1000) % 256);

        buf = new DHCPMessage().createRequest(this.buf, secs);

        discoverPacket = new DatagramPacket(buf, buf.length, Inet4Address.getByAddress(broadcast), clientPort);
        socket.send(discoverPacket);
        System.out.println("Send Request");
    }

    private synchronized boolean isOffer(byte[] buf) {
        for (int i = 0; i < 6; i++) {
            if (buf[i + 28] != mac[i]) return false;
        }
        System.out.println("Response is Offer");
        return true;
    }

    private synchronized boolean isPack(byte[] buf) {
        for (int i = 0; i < 6; i++) {
            if (buf[i + 28] != mac[i]) return false;
        }
        System.out.println("Response is Pack");
        return true;
    }

    private synchronized void printResponseInfo() {
        char[] ipAddress = new char[4];
        for (int i = 0; i < 4; i++) {
            ipAddress[i] = (char) buf[i + 16];
        }

        StringBuilder resStr = new StringBuilder();
        resStr.append("Ip: ");
        for (int i = 0; i < 4; i++) {
            resStr.append(ipAddress[i] & 0xFF);
            if (i < 3) {
                resStr.append(".");
            }
        }
        System.out.println(resStr);
    }
}
