package lab2.dhcp.options.ip;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractIpAddressOption;
import lab2.dhcp.util.OptionsReader;

public final class BroadcastAddressOption extends AbstractIpAddressOption implements ConfigurableOption {
    @Override
    public BroadcastAddressOptionDescription getDescription() {
        return BroadcastAddressOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setAddress(OptionsReader.readIPAddress(configNode));
    }
}
