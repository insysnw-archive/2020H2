import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TFTPServerRRQ extends Thread {

    protected DatagramSocket sock;
    protected InetAddress host;
    protected int port;
    protected FileInputStream source;
    protected TFTPPacket req;
    protected int timeoutLimit=5;
    protected String fileName;

    // initialize read request
    public TFTPServerRRQ(TFTPRead request) throws TftpException {
        try {
            req = request;
            sock = new DatagramSocket();
            sock.setSoTimeout(1000);
            fileName = request.fileName();

            host = request.getAddress();
            port = request.getPort();

            //create file object in parent folder
            File srcFile = new File("../"+fileName);
            //check file
            if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
                source = new FileInputStream(srcFile);
                this.start(); //open new thread for transfer
            } else
                throw new TftpException("access violation");

        } catch (Exception e) {
            TFTPError ePak = new TFTPError(1, e.getMessage()); // error code 1
            try {
                ePak.send(host, port, sock);
            } catch (Exception f) {
                f.printStackTrace();
            }

            System.out.println("Client start failed:  " + e.getMessage());
        }
    }
    //everything is fine, open new thread to transfer file
    public void run() {
        int bytesRead = TFTPPacket.maxTftpPakLen;
        // handle read request
        if (req instanceof TFTPRead) {
            try {
                for (int blkNum = 1; bytesRead == TFTPPacket.maxTftpPakLen; blkNum++) {
                    TFTPData outPak = new TFTPData(blkNum, source);
                    bytesRead = outPak.getLength();
                    outPak.send(host, port, sock);
                    //wait for the correct ack. if incorrect, retry up to 5 times
                    while (timeoutLimit!=0) {
                        try {
                            TFTPPacket ack = TFTPPacket.receive(sock);
                            if (!(ack instanceof TFTPack)){throw new Exception("Client failed");}
                            TFTPack a = (TFTPack) ack;

                            if(a.blockNumber()!=blkNum){ //check ack
                                throw new SocketTimeoutException("last packet lost, resend packet");}
                            break;
                        }
                        catch (SocketTimeoutException t) {//resend last packet
                            System.out.println("Resent blk " + blkNum);
                            timeoutLimit--;
                            outPak.send(host, port, sock);
                        }
                    } // end of while
                    if(timeoutLimit==0){throw new Exception("connection failed");}
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
            }
        }
    }
}
