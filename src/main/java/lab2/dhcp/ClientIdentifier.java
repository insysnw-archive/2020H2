package lab2.dhcp;

import lab2.dhcp.util.Encoder;

public abstract class ClientIdentifier {
    ClientIdentifier() {

    }

    public abstract byte getType();
    public abstract int getSize();

    public abstract void put(byte[] destination, int start);
    public abstract void put(Encoder encoder);

    public static ClientIdentifier get(final byte type, final byte[] data, final int start, final int size) {
        if (type == 1) {
            if (size != 6) {
                throw new IllegalArgumentException("size parameter != 6");
            }
            return new MACAddressClientIdentifier(data, start);
        } else {
            return new OtherClientIdentifier(type, data, start, size);
        }
    }

    public static ClientIdentifier get(final byte type, final byte[] data) {
        return get(type, data, 0, data.length);
    }
}
