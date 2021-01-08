package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractIpAddressOption;

public final class RequestedIpAddressOption extends AbstractIpAddressOption {
    @Override
    public RequestedIpAddressOptionDescription getDescription() {
        return RequestedIpAddressOptionDescription.INSTANCE;
    }
}
