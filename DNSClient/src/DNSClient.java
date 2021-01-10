import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class DNSClient {

    String address;
    private final byte[] server = new byte[4];
    private String name;
    private int port = 53;
    private QueryType queryType = QueryType.A;


    public DNSClient(String[] args) {

        List<String> argsList = Arrays.asList(args);
        ListIterator<String> iterator = argsList.listIterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-mx":
                    queryType = QueryType.MX;
                    break;
                case "-ns":
                    queryType = QueryType.NS;
                    break;
                case "-p":
                    port = Integer.parseInt(iterator.next());
                    break;
                default:
                    if (arg.contains("@")) {
                        address = arg.substring(1);
                        String[] addressComponents = address.split("\\.");

                        for (int i = 0; i < addressComponents.length; i++) {
                            int ipValue = Integer.parseInt(addressComponents[i]);
                            if (ipValue < 0 || ipValue > 255) {
                                throw new NumberFormatException("ERROR\tIncorrect input syntax: IP Address numbers must be between 0 and 255, inclusive.");
                            }
                            server[i] = (byte) ipValue;
                        }
                        name = iterator.next();
                    }
                    break;
            }
        }

        if (name == null) {
            throw new IllegalArgumentException("ERROR\tIncorrect input syntax: Server IP and domain name must be provided.");
        } else makeRequest();
    }

    private void makeRequest() {
        System.out.println("Domain: " + name);
        System.out.println("Server: " + address);
        pollRequest();
    }

    private void pollRequest() {
        try {
            DatagramSocket client = new DatagramSocket();
            InetAddress ipAddress = InetAddress.getByAddress(server);
            DnsRequest request = new DnsRequest(name, queryType);
            byte[] sendbyte = request.getRequest();
            byte[] receivebyte = new byte[1024];
            DatagramPacket sender = new DatagramPacket(sendbyte, sendbyte.length, ipAddress, port);
            client.send(sender);
            DatagramPacket receiver = new DatagramPacket(receivebyte, receivebyte.length);
            client.receive(receiver);
            DnsResponse response = new DnsResponse(receiver.getData(), sendbyte.length, queryType);
            response.outputResponse();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        if (args.length<2) return;
        DNSClient dnsClient = new DNSClient(args);
    }
}
