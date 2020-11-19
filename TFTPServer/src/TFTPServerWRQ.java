import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TFTPServerWRQ extends Thread {
    protected DatagramSocket sock;
    protected InetAddress host;
    protected int port;
    protected FileOutputStream outFile;
    protected TFTPPacket req;
    protected int timeoutLimit = 5;
    protected File saveFile;
    protected String fileName;

    // Initialize read request
    public TFTPServerWRQ(TFTPWrite request) throws TftpException {
        try {
            req = request;
            sock = new DatagramSocket(); // new port for transfer
            sock.setSoTimeout(1000);

            host = request.getAddress();
            port = request.getPort();
            fileName = request.fileName();
            //create file object in parent folder
            saveFile = new File("../"+fileName);

            if (!saveFile.exists()) {
                outFile = new FileOutputStream(saveFile);
                TFTPack a = new TFTPack(0);
                a.send(host, port, sock); // send ack 0 at first, ready to
                // receive
                this.start();
            } else
                throw new TftpException("access violation, file exists");

        } catch (Exception e) {
            TFTPError ePak = new TFTPError(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, sock);
            } catch (Exception f) {
                f.printStackTrace();
            }

            System.out.println("Client start failed:" + e.getMessage());
        }
    }

    public void run() {
        // handle write request
        if (req instanceof TFTPWrite) {
            try {
                for (int blkNum = 1, bytesOut = 512; bytesOut == 512; blkNum++) {
                    while (timeoutLimit != 0) {
                        try {
                            TFTPPacket inPak = TFTPPacket.receive(sock);
                            //check packet type
                            if (inPak instanceof TFTPError) {
                                TFTPError p = (TFTPError) inPak;
                                throw new TftpException(p.message());
                            } else if (inPak instanceof TFTPData) {
                                TFTPData p = (TFTPData) inPak;
                                /*System.out.println("incoming data " + p.blockNumber());*/
                                // check blk num
                                if (p.blockNumber() != blkNum) {
                                    throw new SocketTimeoutException();
                                }
                                //write to the file and send ack
                                bytesOut = p.write(outFile);
                                TFTPack a = new TFTPack(blkNum);
                                a.send(host, port, sock);
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            System.out.println("Time out, resend ack");
                            TFTPack a = new TFTPack(blkNum - 1);
                            a.send(host, port, sock);
                            timeoutLimit--;
                        }
                    }
                    if(timeoutLimit==0){throw new Exception("Connection failed");}
                }
                System.out.println("Transfer completed.(Client " +host +")" );

            } catch (Exception e) {
                TFTPError ePak = new TFTPError(1, e.getMessage());
                try {
                    ePak.send(host, port, sock);
                } catch (Exception f) {
                    f.printStackTrace();
                }

                System.out.println("Client failed:  " + e.getMessage());
                saveFile.delete();
            }
        }
    }
}
