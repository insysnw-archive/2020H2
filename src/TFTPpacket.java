

import java.net.*;
import java.io.*;


class TftpException extends Exception {
    public TftpException(String s) {
        super(s);
    }
}

public class TFTPpacket {

    public static int maxTftpPakLen = 516;
    public static int maxTftpData = 512;

    // Tftp opcodes
    protected static final short tftpRRQ = 1;
    protected static final short tftpWRQ = 2;
    protected static final short tftpDATA = 3;
    protected static final short tftpACK = 4;
    protected static final short tftpERROR = 5;

    protected static final int opOffset = 0;
    protected static final int fileOffset = 2;
    protected static final int blkOffset = 2;
    protected static final int dataOffset = 4;
    protected static final int numOffset = 2;
    protected static final int msgOffset = 4;

    protected byte[] message;
    protected int length;

    protected InetAddress host;
    protected int port;

    public TFTPpacket() {
        message = new byte[maxTftpPakLen];
        length = maxTftpPakLen;
    }

    public static TFTPpacket receive(DatagramSocket sock) throws IOException {
        TFTPpacket in = new TFTPpacket(), retPak = new TFTPpacket();
        DatagramPacket inPak = new DatagramPacket(in.message, in.length);
        sock.receive(inPak);

        switch (in.get(0)) {
            case tftpRRQ:
                retPak = new TFTPread();
                break;
            case tftpWRQ:
                retPak = new TFTPwrite();
                break;
            case tftpDATA:
                retPak = new TFTPdata();
                break;
            case tftpACK:
                retPak = new TFTPack();
                break;
            case tftpERROR:
                retPak = new TFTPerror();
                break;
        }
        retPak.message = in.message;
        retPak.length = inPak.getLength();
        retPak.host = inPak.getAddress();
        retPak.port = inPak.getPort();

        return retPak;
    }

    public void send(InetAddress ip, int port, DatagramSocket s) throws IOException {
        s.send(new DatagramPacket(message, length, ip, port));
    }

    public InetAddress getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLength() {
        return length;
    }

    protected void put(int at, short value) {
        message[at++] = (byte) (value >>> 8);  // first byte
        message[at] = (byte) (value % 256);    // last byte
    }


    protected void put(int at, String value, byte del) {
        value.getBytes(0, value.length(), message, at);
        message[at + value.length()] = del;
    }

    protected int get(int at) {
        return (message[at] & 0xff) << 8 | message[at + 1] & 0xff;
    }

    protected String get(int at, byte del) {
        StringBuffer result = new StringBuffer();
        while (message[at] != del) result.append((char) message[at++]);
        return result.toString();
    }
}


final class TFTPdata extends TFTPpacket {


    protected TFTPdata() {
    }

    public TFTPdata(int blockNumber, FileInputStream in) throws IOException {
        this.message = new byte[maxTftpPakLen];
        this.put(opOffset, tftpDATA);
        this.put(blkOffset, (short) blockNumber);
        length = in.read(message, dataOffset, maxTftpData) + 4;
    }

    public int blockNumber() {
        return this.get(blkOffset);
    }


    public int write(FileOutputStream out) throws IOException {
        out.write(message, dataOffset, length - 4);

        return (length - 4);
    }
}


class TFTPerror extends TFTPpacket {


    protected TFTPerror() {
    }


    public TFTPerror(int number, String message) {
        length = 4 + message.length() + 1;
        this.message = new byte[length];
        put(opOffset, tftpERROR);
        put(numOffset, (short) number);
        put(msgOffset, message, (byte) 0);
    }


    public int number() {
        return this.get(numOffset);
    }

    public String message() {
        return this.get(msgOffset, (byte) 0);
    }
}


final class TFTPack extends TFTPpacket {


    protected TFTPack() {
    }


    public TFTPack(int blockNumber) {
        length = 4;
        this.message = new byte[length];
        put(opOffset, tftpACK);
        put(blkOffset, (short) blockNumber);
    }


    public int blockNumber() {
        return this.get(blkOffset);
    }
}


final class TFTPread extends TFTPpacket {

    protected TFTPread() {
    }


    public TFTPread(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(opOffset, tftpRRQ);
        put(fileOffset, filename, (byte) 0);
        put(fileOffset + filename.length() + 1, dataMode, (byte) 0);
    }



    public String fileName() {
        return this.get(fileOffset, (byte) 0);
    }

    public String requestType() {
        String fname = fileName();
        return this.get(fileOffset + fname.length() + 1, (byte) 0);
    }
}


final class TFTPwrite extends TFTPpacket {



    protected TFTPwrite() {
    }

    public TFTPwrite(String filename, String dataMode) {
        length = 2 + filename.length() + 1 + dataMode.length() + 1;
        message = new byte[length];

        put(opOffset, tftpWRQ);
        put(fileOffset, filename, (byte) 0);
        put(fileOffset + filename.length() + 1, dataMode, (byte) 0);
    }



    public String fileName() {
        return this.get(fileOffset, (byte) 0);
    }

    public String requestType() {
        String fname = fileName();
        return this.get(fileOffset + fname.length() + 1, (byte) 0);
    }
}