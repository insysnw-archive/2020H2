package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractIpAddressOption;

public final class RequestedIpAddressOption extends AbstractIpAddressOption {
    @Override
    public RequestedIpAddressOptionDescription getDescription() {
        return RequestedIpAddressOptionDescription.INSTANCE;
    }
}
