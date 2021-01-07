package lab2.dhcp.options;

import lab2.dhcp.Option;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;
import lab2.dhcp.util.IPAddress;

public abstract class AbstractIpAddressOption implements Option {
    private int address;

    public final int getAddress() {
        return address;
    }

    public final void setAddress(int value) {
        address = value;
    }

    @Override
    public final void encode(Encoder encoder) {
        encoder.putIPAddress(address);
    }

    @Override
    public final void decode(Decoder decoder) {
        address = decoder.getIPAddress();
    }

    @Override
    public String toString() {
        return IPAddress.ipAddressToString(address);
    }
}
