package lab2.dns;

public class Main {
    public static void main(String[] args) {
        try {
            DNSClient client = new DNSClient(args);
            client.makeRequest();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
