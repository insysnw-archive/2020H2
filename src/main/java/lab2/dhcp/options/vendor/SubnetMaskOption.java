package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractIpAddressOption;
import lab2.dhcp.util.OptionsReader;

public final class SubnetMaskOption extends AbstractIpAddressOption implements ConfigurableOption {
    @Override
    public SubnetMaskOptionDescription getDescription() {
        return SubnetMaskOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        setAddress(OptionsReader.readIPAddress(configNode));
    }
}
