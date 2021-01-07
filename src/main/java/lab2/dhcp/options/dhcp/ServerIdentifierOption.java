package lab2.dhcp.options.dhcp;

import lab2.dhcp.options.AbstractIpAddressOption;

public final class ServerIdentifierOption extends AbstractIpAddressOption {
    @Override
    public ServerIdentifierOptionDescription getDescription() {
        return ServerIdentifierOptionDescription.INSTANCE;
    }
}
