import java.net.*;
import java.io.*;


class WriteRequest extends Thread {

    protected DatagramSocket sock;
    protected InetAddress host;
    protected int port;
    protected FileOutputStream outFile;
    protected TftpPacket req;
    protected int timeoutLimit = 5;
    protected File saveFile;
    protected String fileName;

    // Initialize read request
    public WriteRequest(WritePacket request) {
        try {
            req = request;
            sock = new DatagramSocket();
            sock.setSoTimeout(1000);

            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();
            //create file object in parent folder
            saveFile = new File("../storage/" + fileName);

            if (!saveFile.exists()) {
                outFile = new FileOutputStream(saveFile);
                AckPacket ackPacket = new AckPacket(0);
                ackPacket.send(host, port, sock);
                this.start();
            } else
                throw new TftpException("access violation, file exists");

        } catch (Exception e) {
            ErrorPacket errorPacket = new ErrorPacket(1, e.getMessage()); // error code 1
            try {
                errorPacket.send(host, port, sock);
            } catch (Exception e2) {
                System.out.println(e2.toString());
            }

            System.out.println("Client start failed:" + e.getMessage());
        }
    }

    public void run() {
        if (req instanceof WritePacket) {
            try {
                for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
                    while (timeoutLimit != 0) {
                        try {
                            TftpPacket inPacket = TftpPacket.receive(sock);
                            //check packet type
                            if (inPacket instanceof ErrorPacket) {
                                ErrorPacket errorPacket = (ErrorPacket) inPacket;
                                throw new TftpException(errorPacket.message());
                            } else if (inPacket instanceof DataPacket) {
                                DataPacket p = (DataPacket) inPacket;
                                if (p.blockNumber() != blkNum) {
                                    throw new SocketTimeoutException();
                                }
                                //write to the file and send ack
                                bytesOut = p.write(outFile);
                                AckPacket ackPacket = new AckPacket(blkNum);
                                ackPacket.send(host, port, sock);
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            System.out.println("Time out, resend ack");
                            AckPacket ackPacket = new AckPacket(blkNum - 1);
                            ackPacket.send(host, port, sock);
                            timeoutLimit--;
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("Connection failed");
                    }
                }
                System.out.println("Transfer completed.(Client " + host + ")");
                System.out.println("Filename: " + fileName + "\nSHA1 checksum: " + SumHelper.getChecksum("../storage/" + fileName) + "\n");
            } catch (Exception e) {
                ErrorPacket errorPacket = new ErrorPacket(1, e.getMessage());
                try {
                    errorPacket.send(host, port, sock);
                } catch (Exception e2) {
                    System.out.println(e2.toString());
                }

                System.out.println("Client failed:  " + e.getMessage());
                saveFile.delete();
            }
        }
    }
}