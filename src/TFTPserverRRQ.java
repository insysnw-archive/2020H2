import java.net.*;
import java.security.MessageDigest;
import java.io.*;
import java.util.*;


class TFTPserverRRQ extends Thread {

    protected DatagramSocket sock;
    protected InetAddress host;
    protected int port;
    protected FileInputStream source;
    protected TFTPpacket req;
    protected int timeoutLimit = 5;
    protected String fileName;

    public TFTPserverRRQ(TFTPread request) throws TftpException {
        try {
            req = request;
            sock = new DatagramSocket(); //random port
            sock.setSoTimeout(1000);
            fileName = request.fileName();
            host = request.getAddress();
            port = request.getPort();

            File srcFile = new File("../" + fileName);
            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start();
            } else
                throw new TftpException("access violation");

        } catch (Exception e) {
            TFTPerror ePak = new TFTPerror(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, sock);
            } catch (Exception f) {
            }

            System.out.println("Client start failed:  " + e.getMessage());
        }
    }

    public void run() {
        int bytesRead = TFTPpacket.maxTftpPakLen;

        if (req instanceof TFTPread) {
            try {
                for (int blkNum = 1; bytesRead == TFTPpacket.maxTftpPakLen; blkNum++) {
                    TFTPdata outPak = new TFTPdata(blkNum, source);

                    bytesRead = outPak.getLength();

                    outPak.send(host, port, sock);

                    while (timeoutLimit != 0) {
                        try {
                            TFTPpacket ack = TFTPpacket.receive(sock);
                            if (!(ack instanceof TFTPack)) {
                                throw new Exception("Client failed");
                            }
                            TFTPack a = (TFTPack) ack;

                            if (a.blockNumber() != blkNum) { //check ack
                                throw new SocketTimeoutException("last packet lost, resend packet");
                            }

                            break;
                        } catch (SocketTimeoutException t) {//resend last packet
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outPak.send(host, port, sock);
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName);
            } catch (Exception e) {
                TFTPerror ePak = new TFTPerror(1, e.getMessage());

                try {
                    ePak.send(host, port, sock);
                } catch (Exception f) {
                }

                System.out.println("Client failed:  " + e.getMessage());
            }
        }
    }
}