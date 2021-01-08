package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractIpAddressOption;

public final class ServerIdentifierOption extends AbstractIpAddressOption {
    @Override
    public ServerIdentifierOptionDescription getDescription() {
        return ServerIdentifierOptionDescription.INSTANCE;
    }
}
