import java.net.InetAddress;
import java.util.*;

public class Client {
    private Role role;
    private InetAddress clientAddress;
    private int clientPort;
    private Map<String, Lot> bidLots= new HashMap<>();

    public Client(String username, Role role, InetAddress clientAddress, int clientPort) {
        this.role = role;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.username = username;
    }

    public Map<String, Lot> getBidLots() {
        return bidLots;
    }

    public void setBidLots(Map<String, Lot> bidLots) {
        this.bidLots = bidLots;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(InetAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return clientPort == client.clientPort && clientAddress.equals(client.clientAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAddress, clientPort);
    }

    @Override
    public String toString() {
        return "Client{" +
                "role=" + role +
                ", clientAddress=" + clientAddress +
                ", clientPort=" + clientPort +
                ", username='" + username + '\'' +
                '}';
    }
}
