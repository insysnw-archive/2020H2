import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class TftpException extends Exception {
    public TftpException() {
        super();
    }

    public TftpException(String s) {
        super(s);
    }
}

public class TFTPPacket {

    public static int maxTftpPakLen = 516;
    public static int maxTftpData = 512;

    // Tftp opcodes
    protected static final short tftpRRQ = 1;
    protected static final short tftpWRQ = 2;
    protected static final short tftpDATA = 3;
    protected static final short tftpACK = 4;
    protected static final short tftpERROR = 5;

    // Address info (required for replies)
    protected InetAddress host;
    protected int port;

    // Packet Offsets
    protected static final int opOffset = 0;

    protected static final int fileOffset = 2;

    protected static final int blkOffset = 2;
    protected static final int dataOffset = 4;

    protected static final int numOffset = 2;
    protected static final int msgOffset = 4;

    protected byte[] message;
    protected int length;

    public TFTPPacket() {
        message = new byte[maxTftpPakLen];
        length = maxTftpPakLen;
    }

    public static TFTPPacket receive(DatagramSocket sock) throws IOException {
        TFTPPacket in = new TFTPPacket(), retPak = new TFTPPacket();
        //receive data and put them into in.message
        DatagramPacket inPak = new DatagramPacket(in.message, in.length);
        sock.receive(inPak);

        //Check the opcode in message, then cast the message into the corresponding type
        switch (in.get(0)) {
            case tftpRRQ:
                retPak = new TFTPRead();
                break;
            case tftpWRQ:
                retPak = new TFTPWrite();
                break;
            case tftpDATA:
                retPak = new TFTPData();
                break;
            case tftpACK:
                retPak = new TFTPack();
                break;
            case tftpERROR:
                retPak = new TFTPError();
                break;
        }
        retPak.message = in.message;
        retPak.length = inPak.getLength();
        retPak.host = inPak.getAddress();
        retPak.port = inPak.getPort();

        return retPak;
    }

    //Method to send packet
    public void send(InetAddress ip, int port, DatagramSocket s) throws IOException {
        s.send(new DatagramPacket(message, length, ip, port));
    }

    // DatagramPacket like methods
    public InetAddress getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLength() {
        return length;
    }

    protected int get(int at) {
        return (message[at] & 0xff) << 8 | message[at + 1] & 0xff;
    }

    protected String get(int at, byte del) {
        StringBuffer result = new StringBuffer();
        while (message[at] != del) result.append((char) message[at++]);
        return result.toString();
    }

    protected void put(int at, short value) {
        message[at++] = (byte) (value >>> 8);  // first byte
        message[at] = (byte) (value % 256);    // last byte
    }

    @SuppressWarnings("deprecation")
    //Put the filename and mode into the 'message' at 'at' follow by byte "del"
    protected void put(int at, String value, byte del) {
        value.getBytes(0, value.length(), message, at);
        message[at + value.length()] = del;
    }
}

final class TFTPRead extends TFTPPacket {

    // Constructors
    protected TFTPRead() {
    }

    //specify the filename and transfer mode
    public TFTPRead(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(opOffset, tftpRRQ);
        put(fileOffset, filename, (byte) 0);
        put(fileOffset + filename.length() + 1, dataMode, (byte) 0);
    }

// Accessors

    public String fileName() {
        return this.get(fileOffset, (byte) 0);
    }

    public String requestType() {
        String fName = fileName();
        return this.get(fileOffset + fName.length() + 1, (byte) 0);
    }
}

final class TFTPWrite extends TFTPPacket {

//Constructors

    protected TFTPWrite() {
    }

    public TFTPWrite(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(opOffset, tftpWRQ);
        put(fileOffset, filename, (byte) 0);
        put(fileOffset + filename.length() + 1, dataMode, (byte) 0);
    }

//Accessors

    public String fileName() {
        return this.get(fileOffset, (byte) 0);
    }

//    public String requestType() {
//        String fName = fileName();
//        return this.get(fileOffset + fName.length() + 1, (byte) 0);
//    }
}

final class TFTPData extends TFTPPacket {

    // Constructors
    protected TFTPData() {
    }

    public TFTPData(int blockNumber, FileInputStream in) throws IOException {
        this.message = new byte[maxTftpPakLen];
        // manipulate message
        this.put(opOffset, tftpDATA);
        this.put(blkOffset, (short) blockNumber);
        // read the file into packet and calculate the entire length
        length = in.read(message, dataOffset, maxTftpData) + 4;
    }

    // Accessors

    public int blockNumber() {
        return this.get(blkOffset);
    }

    // File output
    public int write(FileOutputStream out) throws IOException {
        out.write(message, dataOffset, length - 4);

        return (length - 4);
    }
}

class TFTPError extends TFTPPacket {

    // Constructors
    protected TFTPError() {
    }

    //Generate error packet
    public TFTPError(int number, String message) {
        length = 4 + message.length() + 1;
        this.message = new byte[length];
        put(opOffset, tftpERROR);
        put(numOffset, (short) number);
        put(msgOffset, message, (byte) 0);
    }

    // Accessors
    public int number() {
        return this.get(numOffset);
    }

    public String message() {
        return this.get(msgOffset, (byte) 0);
    }
}

final class TFTPack extends TFTPPacket {

    // Constructors
    protected TFTPack() {
    }

    //Generate ack packet
    public TFTPack(int blockNumber) {
        length = 4;
        this.message = new byte[length];
        put(opOffset, tftpACK);
        put(blkOffset, (short) blockNumber);
    }

    // Accessors
    public int blockNumber() {
        return this.get(blkOffset);
    }
}
