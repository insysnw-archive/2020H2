import java.net.*;
import java.io.*;
import java.util.*;

public class TFTPServer {

	public static void main(String[] argv) {
		try {
			DatagramSocket sock;
			if (argv.length==2) {
				byte[] server = new byte[4];
				String[] addressComponents = argv[0].split("\\.");

				for (int i = 0; i < addressComponents.length; i++) {
					int ipValue = Integer.parseInt(addressComponents[i]);
					if (ipValue < 0 || ipValue > 255) {
						throw new NumberFormatException("ERROR\tIncorrect input syntax: IP Address numbers must be between 0 and 255, inclusive.");
					}
					server[i] = (byte) ipValue;
				}
				sock = new DatagramSocket(Integer.parseInt(argv[1]), InetAddress.getByAddress(server));
			} else if (argv.length == 1) {
				sock = new DatagramSocket(Integer.parseInt(argv[0]));
			} else
			sock = new DatagramSocket(6973);
			System.out.println("Server Ready.  Port:  " + sock.getLocalPort());

			// Listen for requests
			while (true) {
				TFTPpacket in = TFTPpacket.receive(sock);
				// receive read request
				if (in instanceof TFTPread) {
					System.out.println("Read Request from " + in.getAddress());
					TFTPserverRRQ r = new TFTPserverRRQ((TFTPread) in);
				}
				// receive write request
				else if (in instanceof TFTPwrite) {
					System.out.println("Write Request from " + in.getAddress());
					TFTPserverWRQ w = new TFTPserverWRQ((TFTPwrite) in);
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