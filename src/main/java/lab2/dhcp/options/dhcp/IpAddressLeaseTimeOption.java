package lab2.dhcp.options.dhcp;

import lab2.dhcp.OptionDescription;
import lab2.dhcp.options.AbstractTimeOption;

public final class IpAddressLeaseTimeOption extends AbstractTimeOption {
    @Override
    public OptionDescription getDescription() {
        return IpAddressLeaseTimeOptionDescription.INSTANCE;
    }
}
