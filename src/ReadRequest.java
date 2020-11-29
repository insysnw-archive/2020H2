import java.net.*;
import java.io.*;


class ReadRequest extends Thread {

    protected DatagramSocket socket;
    protected InetAddress host;
    protected int port;
    protected FileInputStream source;
    protected TftpPacket req;
    protected int timeoutLimit = 5;
    protected String fileName;

    public ReadRequest(ReadPacket request) {
        try {
            req = request;
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            fileName = request.fileName();

            host = request.getAddress();
            port = request.getPort();

            File srcFile = new File("../storage/" + fileName);
            System.out.println("procce checking");
            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start();
            } else
                throw new TftpException("access violation");

        } catch (Exception e) {
            ErrorPacket errorPacket = new ErrorPacket(1, e.getMessage());
            try {
                errorPacket.send(host, port, socket);
            } catch (Exception e2) {
                System.out.println(e2.toString());
            }

            System.out.println("Client start failed:  " + e.getMessage());
        }
    }

    public void run() {
        int bytesRead = TftpPacket.maxTftpPakLen;
        if (req instanceof ReadPacket) {
            try {
                for (int blkNum = 1; bytesRead == TftpPacket.maxTftpPakLen; blkNum++) {
                    DataPacket outPak = new DataPacket(blkNum, source);
                    System.out.println("send block no. " + outPak.blockNumber());
                    bytesRead = outPak.getLength();
                    System.out.println("bytes sent:  " + bytesRead);
                    outPak.send(host, port, socket);
                    System.out.println("current op code  " + outPak.get(0));

                    while (timeoutLimit != 0) {
                        try {
                            TftpPacket ack = TftpPacket.receive(socket);
                            if (!(ack instanceof AckPacket)) {
                                throw new Exception("Client failed");
                            }
                            AckPacket a = (AckPacket) ack;

                            if (a.blockNumber() != blkNum) { //check ack
                                throw new SocketTimeoutException("last packet lost, resend packet");
                            }
                            System.out.println("confirm blk num " + a.blockNumber() + " from " + a.getPort());
                            break;
                        } catch (SocketTimeoutException t) {//resend last packet
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outPak.send(host, port, socket);
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName + "\nSHA1 checksum: " + SumHelper.getChecksum("../storage/" + fileName) + "\n");
            } catch (Exception e) {
                ErrorPacket ePak = new ErrorPacket(1, e.getMessage());

                try {
                    ePak.send(host, port, socket);
                } catch (Exception e2) {
                    System.out.println(e2.toString());
                }

                System.out.println("Client failed:  " + e.getMessage());
            }
        }
    }
}