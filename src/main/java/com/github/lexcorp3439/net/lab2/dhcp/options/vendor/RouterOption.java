package com.github.lexcorp3439.net.lab2.dhcp.options.vendor;

import com.github.lexcorp3439.net.lab2.dhcp.options.AbstractIpAddressListOption;
import com.github.lexcorp3439.net.lab2.dhcp.util.OptionsReader;
import com.github.lexcorp3439.net.lab2.dhcp.ConfigurableOption;

public final class RouterOption extends AbstractIpAddressListOption implements ConfigurableOption {
    @Override
    public RouterOptionDescription getDescription() {
        return RouterOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        getAddresses().addAll(OptionsReader.readIPAddressList(configNode));
    }
}
