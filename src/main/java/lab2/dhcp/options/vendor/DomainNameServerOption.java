package lab2.dhcp.options.vendor;

import lab2.dhcp.ConfigurableOption;
import lab2.dhcp.options.AbstractIpAddressListOption;
import lab2.dhcp.util.OptionsReader;

public final class DomainNameServerOption extends AbstractIpAddressListOption implements ConfigurableOption {
    @Override
    public DomainNameServerOptionDescription getDescription() {
        return DomainNameServerOptionDescription.INSTANCE;
    }

    @Override
    public void configure(Object configNode) {
        getAddresses().addAll(OptionsReader.readIPAddressList(configNode));
    }
}
