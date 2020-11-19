import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TFTPServer {

    public static void main(String[] args) {
        try {
            DatagramSocket sock = new DatagramSocket(69);
            System.out.println("Server Ready. Port: " + sock.getLocalPort());

            while (true) {
                TFTPPacket in  = TFTPPacket.receive(sock);
                if (in instanceof TFTPRead) {
                    System.out.println("Download Request from  " + in.getAddress());
                    TFTPServerRRQ r = new TFTPServerRRQ((TFTPRead) in);
                } else if (in instanceof TFTPWrite) {
                    System.out.println("Upload Request from " + in.getAddress());
                    TFTPServerWRQ w = new TFTPServerWRQ((TFTPWrite) in);
                }
            }
        } catch (SocketException e) {
            System.out.println("Server terminated(SocketException) " + e.getMessage());
        } catch (TftpException e) {
            System.out.println("Server terminated(TftpException)" + e.getMessage());
        } catch (IOException e) {
            System.out.println("Server terminated(IOException)" + e.getMessage());
        }
    }
}
