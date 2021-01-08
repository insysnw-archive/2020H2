package com.github.lexcorp3439.net.lab2.dhcp.options;

import com.github.lexcorp3439.net.lab2.dhcp.Option;
import com.github.lexcorp3439.net.lab2.dhcp.util.Decoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.Encoder;
import com.github.lexcorp3439.net.lab2.dhcp.util.IPAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractIpAddressListOption implements Option {
    private final List<Integer> addresses = new ArrayList<>();

    public final List<Integer> getAddresses() {
        return addresses;
    }

    @Override
    public final void encode(Encoder encoder) {
        for (int address : addresses) {
            encoder.putIPAddress(address);
        }
    }

    @Override
    public final void decode(Decoder decoder) {
        while (decoder.remaining() != 0) {
            addresses.add(decoder.getIPAddress());
        }
    }

    @Override
    public String toString() {
        return addresses.stream()
                .map(IPAddress::ipAddressToString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
