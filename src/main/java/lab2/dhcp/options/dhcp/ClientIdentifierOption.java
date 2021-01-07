package lab2.dhcp.options.dhcp;

import lab2.dhcp.ClientIdentifier;
import lab2.dhcp.Option;
import lab2.dhcp.util.Decoder;
import lab2.dhcp.util.Encoder;

public final class ClientIdentifierOption implements Option {
    private ClientIdentifier identifier;

    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(ClientIdentifier value) {
        identifier = value;
    }

    @Override
    public ClientIdentifierOptionDescription getDescription() {
        return ClientIdentifierOptionDescription.INSTANCE;
    }

    @Override
    public void encode(Encoder encoder) {
        encoder.putByte(identifier.getType());
        identifier.put(encoder);
    }

    @Override
    public void decode(Decoder decoder) {
        byte type = decoder.getByte();
        byte[] bytes = decoder.getBytes();
        identifier = ClientIdentifier.get(type, bytes);
    }
}
