package com.github.lexcorp3439.net.lab2.dhcp.options.dhcp;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractTimeOption;
import com.github.lexcorp3439.net.lab2.dhcp.OptionDescription;

public final class IpAddressLeaseTimeOption extends AbstractTimeOption {
    @Override
    public OptionDescription getDescription() {
        return IpAddressLeaseTimeOptionDescription.INSTANCE;
    }
}
