package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.util.DHCPIllegalFormatException;
import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;
import com.github.lexcorp3439.net.lab2.dhcp.Option;

public final class DHCPMessageTypeOption implements Option {
    public enum Types {
        NoState, Discover, Offer, Request, Decline, Ack, NotAck, Release, Inform
    }

    private Types type = Types.NoState;

    public Types getType() {
        return type;
    }

    public void setType(Types value) {
        type = value;
    }

    @Override
    public DHCPMessageTypeOptionDescription getDescription() {
        return DHCPMessageTypeOptionDescription.INSTANCE;
    }

    @Override
    public void encode(Encoder encoder) {
        encoder.putByte((byte) type.ordinal());
    }

    @Override
    public void decode(Decoder decoder) {
        byte code = decoder.getByte();
        if (code < 1 || code > 8)
            throw new DHCPIllegalFormatException("illegal DHCP type code: " + code);
        type = Types.values()[code];
    }
}
